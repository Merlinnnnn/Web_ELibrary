package com.spkt.libraSys.service.document.viewer;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentService;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import com.spkt.libraSys.service.document.upload.UploadRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentViewerServiceImpl implements DocumentViewerService {
    private final UploadRepository uploadRepository;
    private final DigitalDocumentService digitalDocumentService;

    @Override
    public byte[] getDocumentPageContent(Long uploadId, int pageNumber) {
        UploadEntity upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND, "Không tìm thấy tệp đã chọn"));

        String filePath = upload.getFilePath();

        if (!Files.exists(Paths.get(filePath))) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND, "Tệp không tồn tại trên hệ thống");
        }

        try {
            if (filePath.endsWith(".pdf")) {
                return extractPdfPage(filePath, pageNumber);
            } else if (filePath.endsWith(".docx")) {
                List<String> pages = extractWordPages(filePath);
                if (pageNumber < 1 || pageNumber > pages.size()) {
                    throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND, "Trang không hợp lệ");
                }
                return pages.get(pageNumber - 1).getBytes();  // Trả về nội dung của trang yêu cầu
            } else {
                throw new AppException(ErrorCode.UNSUPPORTED_FILE, "Định dạng tệp không được hỗ trợ");
            }
        } catch (IOException e) {
            throw new AppException(ErrorCode.FILE_PROCESSING_ERROR, "Lỗi khi đọc tài liệu");
        }
    }
    private List<String> extractWordPages(String filePath) throws IOException {
        List<String> pages = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder currentPage = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (paragraph.getText().trim().isEmpty()) continue;  // Bỏ qua đoạn trống

                currentPage.append(paragraph.getText()).append("\n");

                // Kiểm tra nếu đoạn văn chứa Page Break
                if (paragraph.getCTP().toString().contains("<w:br w:type=\"page\"/>")) {
                    pages.add(currentPage.toString().trim()); // Lưu trang hiện tại
                    currentPage = new StringBuilder();  // Chuyển sang trang mới
                }
            }

            // Thêm trang cuối cùng nếu có nội dung
            if (!currentPage.isEmpty()) {
                pages.add(currentPage.toString().trim());
            }
        }
        return pages;
    }


    private byte[] extractPdfPage(String filePath, int pageNumber) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, 150);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }
    }

    private byte[] extractWordContent(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            for (var paragraph : document.getParagraphs()) {
                baos.write(paragraph.getText().getBytes());
                baos.write("\n".getBytes());
            }
            return baos.toByteArray();
        }
    }

    @Override
    public int getDocumentPageCount(Long uploadId) {
        UploadEntity upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND, "Không tìm thấy tệp đã chọn"));

        String filePath = upload.getFilePath();
        if (!Files.exists(Paths.get(filePath))) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND, "Tệp không tồn tại trên hệ thống");
        }

        try {
            if (filePath.endsWith(".pdf")) {
                return getPdfPageCount(filePath);
            } else if (filePath.endsWith(".docx")) {
                return getWordPageCount(filePath);
            } else {
                throw new AppException(ErrorCode.UNSUPPORTED_FILE, "Định dạng tệp không được hỗ trợ");
            }
        } catch (IOException e) {
            throw new AppException(ErrorCode.FILE_PROCESSING_ERROR, "Lỗi khi đọc tài liệu");
        }
    }

    private int getPdfPageCount(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            return document.getNumberOfPages();
        }
    }

    private int getWordPageCount(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {
            return document.getProperties().getExtendedProperties().getPages();
        }
    }
    @Override
    public ResponseEntity<Resource> streamVideo(Long uploadId, String rangeHeader) throws IOException {
        UploadEntity upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND, "Không tìm thấy tệp đã chọn"));

        String filePath = upload.getFilePath();
        Path videoPath = Paths.get(filePath);
        if (!Files.exists(videoPath)) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND, "Tệp không tồn tại trên hệ thống");
        }

        Resource videoResource = new UrlResource(videoPath.toUri());
        long fileSize = Files.size(videoPath);

        if (rangeHeader == null) {
            return ResponseEntity.ok()
                    .contentType(MediaTypeFactory.getMediaType(videoResource).orElse(MediaType.APPLICATION_OCTET_STREAM))
                    .body(videoResource);
        }

        String[] ranges = rangeHeader.replace("bytes=", "").split("-");
        long rangeStart = Long.parseLong(ranges[0]);
        long rangeEnd = (ranges.length > 1 && !ranges[1].isEmpty()) ? Long.parseLong(ranges[1]) : fileSize - 1;
        long contentLength = rangeEnd - rangeStart + 1;

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + rangeStart + "-" + rangeEnd + "/" + fileSize);
        headers.setContentLength(contentLength);
        headers.setContentType(MediaTypeFactory.getMediaType(videoResource).orElse(MediaType.APPLICATION_OCTET_STREAM));

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .body(videoResource);
    }

    @Override
    public Resource getFullDocumentContent(Long uploadId) throws IOException {
        UploadEntity upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND, "Không tìm thấy tệp đã chọn"));

        Path filePath = Paths.get(upload.getFilePath());
        if (!Files.exists(filePath)) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND, "Tệp không tồn tại trên hệ thống");
        }

        return new UrlResource(filePath.toUri());
    }

    @Override
    public String getFileName(Long uploadId) {
        UploadEntity upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND, "Không tìm thấy tệp đã chọn"));
        return upload.getFileName();
    }

    @Override
    public String getFileContentType(Long uploadId) {


        UploadEntity upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND, "Không tìm thấy tệp đã chọn"));

        String filePath = upload.getFilePath();
        try {
            return Files.probeContentType(Paths.get(filePath)); // Xác định loại MIME chính xác
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE; // Nếu lỗi, mặc định là dữ liệu nhị phân
        }
    }



}
