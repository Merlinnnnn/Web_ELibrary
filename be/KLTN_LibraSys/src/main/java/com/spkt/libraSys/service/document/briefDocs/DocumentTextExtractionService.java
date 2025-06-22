package com.spkt.libraSys.service.document.briefDocs;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentEntity;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentRepository;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentRepository;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import com.spkt.libraSys.service.drm.DrmEncryptionUtil;
import com.spkt.libraSys.service.drm.DrmKeyEntity;
import com.spkt.libraSys.service.drm.DrmKeyRepository;
import com.spkt.libraSys.service.openAI.OpenAIService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTextExtractionService {

    private final TextProcessingService textProcessingService;
    private final OpenAIService openAIService;
    private final DigitalDocumentRepository digitalDocumentRepository;
    private final DocumentRepository documentRepository;
    private final DrmEncryptionUtil drmEncryptionUtil;
    private final DrmKeyRepository drmKeyEntityRepository;

//    @Value("${OpenAI.apiKey}")
//    protected String apiKey;


    @Async
    @Transactional
    public void summarizeDoc(Long documentId,boolean isSummarizeDoc) {
        try {
            Thread.sleep(1000);
            DocumentEntity document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu với ID: " + documentId));

            if (isSummarizeDoc &&document.getSummary() != null && !document.getSummary().isBlank()) {
                log.info("Tài liệu ID {} đã có bản tóm tắt. Bỏ qua.", documentId);
                return;
            }


            DigitalDocumentEntity digitalDocument = document.getDigitalDocument();
            log.info("digitalDocument: {} ", digitalDocument.getDigitalDocumentId());
            if (digitalDocument == null) {
                throw new RuntimeException("Tài liệu số ID " + documentId + " chưa được liên kết với DigitalDocumentEntity.");
            }

            if (digitalDocument.getUploads() == null || digitalDocument.getUploads().isEmpty()) {
                log.warn("Tài liệu số ID {} không có file đính kèm để tóm tắt", documentId);
                return;
            }

            StringBuilder combinedSummary = new StringBuilder();

            for (UploadEntity upload : digitalDocument.getUploads()) {
                String filePath = upload.getFilePath();
                try {
                    File encryptedFile = Paths.get(upload.getFilePath()).toFile();

                    if (!encryptedFile.exists()) {
                        log.warn("File không tồn tại tại đường dẫn: {}", upload.getFilePath());
                        continue;
                    }
                    // 1. Get decryption key from DB
                    DrmKeyEntity drmKeyEntity = drmKeyEntityRepository.findByUploadIdAndActive(upload.getUploadId(), true)
                            .orElseThrow(() -> new AppException(ErrorCode.INVALID_CONTENT_KEY, "Key không tồn tại"));

                    // 2. Read encrypted file data
                    byte[] encryptedData = Files.readAllBytes(encryptedFile.toPath());

                    // 3. Decrypt content
                    String contentKey = drmEncryptionUtil.decryptKey(drmKeyEntity.getContentKey());
                    byte[] decryptedData = drmEncryptionUtil.decryptContent(encryptedData, contentKey);

                    // 4. Write temporary file for summarization
                    File decryptedFile = File.createTempFile("decrypted_", ".tmp");
                    decryptedFile.deleteOnExit();  // Ensure temporary file is deleted when program ends

                    try (FileOutputStream fos = new FileOutputStream(decryptedFile)) {
                        fos.write(decryptedData);
                    }

                    // 5. Change file extension if needed
                    File convertedFile = convertFileExtension(decryptedFile, filePath);

                    // 6. Summarize file content
                    String summary = this.summarizeFile(convertedFile,isSummarizeDoc,documentId);
                    if(summary!=null &&  summary.equals("REJECTED_BY_AI")) return;
                    combinedSummary.append(summary).append("\n---\n");
                } catch (Exception ex) {
                    log.error("Lỗi khi tóm tắt file: {} - {}", upload.getFilePath(), ex.getMessage());
                }
            }

            // Update summary content in DocumentEntity
            if(isSummarizeDoc){
                document.setSummary(combinedSummary.toString());
                documentRepository.save(document);
                log.info(combinedSummary.toString());
                log.info("✅ Tóm tắt tài liệu số ID {} thành công", documentId);
            }else{
                document.setApprovalStatus(ApprovalStatus.APPROVED_BY_AI);
                documentRepository.save(document);
                log.info("APPROVED BY  AI:");
            }

        } catch (Exception e) {
            log.error("❌ Lỗi khi tóm tắt tài liệu số ID {}: {}", documentId, e.getMessage(), e);
        }
    }

    private File convertFileExtension(File decryptedFile, String originalFilePath) throws IOException {
        String newFilePath = originalFilePath.replaceFirst("\\.drm$", "");  // Change file extension
        File convertedFile = new File(newFilePath);

        // Rename decrypted file to original filename
        Files.move(decryptedFile.toPath(), convertedFile.toPath());
        return convertedFile;
    }


    // Method to process text summarization from file
    public String summarizeFile(File file,boolean isSummarizeDoc,Long documentId) throws IOException {
        String text;

        // Extract text from file
        if (file.getName().endsWith(".pdf")) {
            text = textProcessingService.cleanText(textProcessingService.extractTextFromPDF(file));
        } else if (file.getName().endsWith(".docx")) {
            text = textProcessingService.cleanText(textProcessingService.extractTextFromDocx(file));
        } else {
            throw new IllegalArgumentException("Unsupported file type!");
        }

        // 2. If only moderation => Call moderation once for entire content
        if (!isSummarizeDoc) {
            DocumentEntity documentEntity = documentRepository.findById(documentId)
                    .orElseThrow(()->new AppException(ErrorCode.DOCUMENT_NOT_FOUND,"Tai lieu khong ton tai"));

            OpenAIService.ModerationResult result = openAIService.moderateDocumentContent(text);
            if (result.isRejected()) {
                log.info( "🚫 Nội dung bị từ chối bởi AI: " + result.getReason());
                documentEntity.setApprovalStatus(ApprovalStatus.REJECTED_BY_AI);
                documentEntity.setReason_approval(result.reason);
                documentRepository.save(documentEntity);
                return "REJECTED_BY_AI";
            }
            return result.reason;
        }

        // Split text into smaller sections
        String[] sections = textProcessingService.splitTextBySections(text);

        StringBuilder fullSummary = new StringBuilder();
        String previousContext = "";
        int maxTokenCount = 1000; // Token limit for previousContext

        // Summarize each text section
        for (String section : sections) {
            if (textProcessingService.estimateTokenCount(section) > 4000) {
                section = section.substring(0, 4000);  // Truncate if section is too long
            }

            previousContext = trimContext(previousContext, maxTokenCount);

            String summary = openAIService.summarizeTextWithOpenAI(section, previousContext);
            fullSummary.append(summary).append("\n");

            previousContext += summary;  // Update context for next section
        }

        return fullSummary.toString();  // Return complete summary
    }

    // Trim context if it exceeds allowed token count
    private String trimContext(String previousContext, int maxTokenCount) {
        int tokenCount = textProcessingService.estimateTokenCount(previousContext);
        if (tokenCount > maxTokenCount) {
            int trimLength = previousContext.length() - (maxTokenCount * 4);
            previousContext = previousContext.substring(trimLength);
        }
        return previousContext;
    }
}
