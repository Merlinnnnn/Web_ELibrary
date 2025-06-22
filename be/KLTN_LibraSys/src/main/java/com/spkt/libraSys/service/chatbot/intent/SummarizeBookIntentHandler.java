package com.spkt.libraSys.service.chatbot.intent;

import com.spkt.libraSys.service.chatbot.IntentHandler;
import com.spkt.libraSys.service.chatbot.WebhookRequest;
import com.spkt.libraSys.service.chatbot.WebhookResponse;
import com.spkt.libraSys.service.document.*;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentEntity;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentRepository;
import com.spkt.libraSys.service.document.briefDocs.DocumentTextExtractionService;
import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SummarizeBookIntentHandler implements IntentHandler {
    private final DocumentRepository documentRepository;
    private final DocumentTextExtractionService documentTextExtractionService;

    @Override
    public String getIntentName() {
        return "SummarizeBook";
    }

    @Override
    public String getToDesc() {
        return "tóm tắt sách";
    }

    @Override
    public WebhookResponse handle(WebhookRequest request) {
        try {
            String bookId = getStringParameter(request, "bookId");
            if (bookId == null || bookId.trim().isEmpty()) {
                return createEmptyIdResponse();
            }

            // Tìm sách theo ID
            Optional<DocumentEntity> bookOptional = documentRepository.findById(Long.parseLong(bookId));
            if (bookOptional.isEmpty()) {
                return createNoBookFoundResponse(bookId);
            }

            DocumentEntity book = bookOptional.get();
            String summary = getBookSummary(book);

            // Quick replies cho các hành động phổ biến
            List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
                new WebhookResponse.QuickReply("Tìm sách khác", "{\"eventName\": \"search_book\", \"parameters\": {}}"),
                new WebhookResponse.QuickReply("Xem chi tiết sách", "{\"eventName\": \"book_details\", \"parameters\": {\"bookId\": \"" + book.getDocumentId() + "\"}}"),
                new WebhookResponse.QuickReply("Liên hệ thủ thư", "{\"eventName\": \"contact_librarian\", \"parameters\": {\"bookId\": \"" + book.getDocumentId() + "\"}}")
            );

            return createSuccessResponse(
                summary,
                quickReplies,
                null,
                null,
                null
            );

        } catch (Exception e) {
            return createErrorResponse("Đã có lỗi xảy ra khi xử lý yêu cầu tóm tắt sách. Vui lòng thử lại sau.");
        }
    }

    private String getBookSummary(DocumentEntity book) {
        // Nếu đã có summary thì trả về
        if (book.getSummary() != null && !book.getSummary().trim().isEmpty()) {
            return book.getSummary();
        }

        // Nếu là tài liệu vật lý và có mô tả
        if (book.getDocumentCategory() == DocumentCategory.PHYSICAL && 
            book.getDescription() != null && !book.getDescription().trim().isEmpty()) {
            return book.getDescription();
        }

        // Nếu không có summary và description, tạo mới bằng FileSummarizerService
        try {
            documentTextExtractionService.summarizeDoc(book.getDocumentId(),true);
            // Đợi một chút để đảm bảo quá trình tóm tắt hoàn thành
            Thread.sleep(2000);
            // Lấy lại document từ database để có summary mới
            book = documentRepository.findById(book.getDocumentId()).orElse(book);
            return book.getSummary() != null ? book.getSummary() : "Đang tạo tóm tắt, vui lòng thử lại sau.";
        } catch (Exception e) {
            return "Xin lỗi, không thể tạo tóm tắt cho sách này. Vui lòng liên hệ thủ thư để được hỗ trợ.";
        }
    }

    private WebhookResponse createEmptyIdResponse() {
        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("Tìm sách", "{\"eventName\": \"search_book\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("Danh sách sách mới", "{\"eventName\": \"new_books\", \"parameters\": {}}")
        );

        return createSuccessResponse(
            "Vui lòng cung cấp ID sách bạn muốn tóm tắt.",
            quickReplies,
            null,
            null,
            null
        );
    }

    private WebhookResponse createNoBookFoundResponse(String bookId) {
        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("Tìm sách khác", "{\"eventName\": \"search_book\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("Liên hệ thủ thư", "{\"eventName\": \"contact_librarian\", \"parameters\": {}}")
        );

        return createSuccessResponse(
            "Xin lỗi, chúng tôi không tìm thấy sách với ID \"" + bookId + "\" trong thư viện. " +
            "Vui lòng kiểm tra lại ID sách hoặc liên hệ với nhân viên thư viện để được hỗ trợ.",
            quickReplies,
            null,
            null,
            null
        );
    }
} 