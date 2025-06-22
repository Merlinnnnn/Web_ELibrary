package com.spkt.libraSys.service.chatbot.intent;

import com.spkt.libraSys.service.chatbot.IntentHandler;
import com.spkt.libraSys.service.chatbot.WebhookRequest;
import com.spkt.libraSys.service.chatbot.WebhookResponse;
import com.spkt.libraSys.service.document.*;
import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SummarizeBookByTitleIntentHandler implements IntentHandler {
    private final DocumentRepository documentRepository;

    @Override
    public String getIntentName() {
        return "SummarizeBookByTitle";
    }

    @Override
    public String getToDesc() {
        return "tìm và tóm tắt sách theo tên";
    }

    private List<DocumentEntity> findBooksByTitle(String title) {
        Specification<DocumentEntity> spec = (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("documentName")),
                        "%" + title.toLowerCase() + "%"
                ),
                criteriaBuilder.equal(root.get("status"), DocumentStatus.ENABLED),
                criteriaBuilder.equal(root.get("approvalStatus"), ApprovalStatus.APPROVED)
        );

        return documentRepository.findAll(spec, PageRequest.of(0, 5)).getContent();
    }

    @Override
    public WebhookResponse handle(WebhookRequest request) {
        try {
            String bookTitle = getStringParameter(request, "bookTitle");
            if (bookTitle == null || bookTitle.trim().isEmpty()) {
                return createEmptyTitleResponse();
            }

            // Tìm sách theo tên
            List<DocumentEntity> matchingBooks = findBooksByTitle(bookTitle);
            if (matchingBooks.isEmpty()) {
                return createNoBookFoundResponse(bookTitle);
            }

            // Tạo danh sách cards cho mỗi cuốn sách
            List<WebhookResponse.Card> cards = new ArrayList<>();
            for (DocumentEntity book : matchingBooks) {
                List<WebhookResponse.Button> buttons = Arrays.asList(
                    new WebhookResponse.Button(
                        "Tóm tắt sách",
                        "{\"eventName\": \"summarize\", \"parameters\": {\"bookId\": \"" + book.getDocumentId() + "\"}}",
                        "POST",
                        "/api/chat"
                    ),
                    new WebhookResponse.Button(
                        "Xem chi tiết",
                        "",
                        "GET",
                        "/api/v1/documents/" + book.getDocumentId()
                    )
                );

                WebhookResponse.Card card = new WebhookResponse.Card(
                    book.getDocumentName(),
                    book.getDescription() != null ? book.getDescription() : "Không có mô tả",
                    book.getCoverImage(),
                    buttons
                );
                cards.add(card);
            }

            // Quick replies cho các hành động phổ biến
            List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
                new WebhookResponse.QuickReply("Tìm sách khác", "{\"eventName\": \"search_book\", \"parameters\": {}}"),
                new WebhookResponse.QuickReply("Danh sách sách mới", "{\"eventName\": \"new_books\", \"parameters\": {}}"),
                new WebhookResponse.QuickReply("⏰ Giờ mở cửa", "{\"eventName\": \"LibraryHours\", \"parameters\": {}}")
            );

            return createSuccessResponse(
                "Tìm thấy " + cards.size() + " sách phù hợp với từ khóa \"" + bookTitle + "\". " +
                "Vui lòng chọn sách bạn muốn tóm tắt:",
                quickReplies,
                cards,
                null,
                null
            );

        } catch (Exception e) {
            return createErrorResponse("Đã có lỗi xảy ra khi tìm kiếm sách. Vui lòng thử lại sau.");
        }
    }

    private WebhookResponse createEmptyTitleResponse() {
        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("Tìm sách", "{\"eventName\": \"search_book\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("Danh sách sách mới", "{\"eventName\": \"new_books\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("⏰ Giờ mở cửa", "{\"eventName\": \"LibraryHours\", \"parameters\": {}}")
        );

        return createSuccessResponse(
            "Vui lòng cung cấp tên sách bạn muốn tìm kiếm và tóm tắt.",
            quickReplies,
            null,
            null,
            null
        );
    }

    private WebhookResponse createNoBookFoundResponse(String bookTitle) {
        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("Tìm sách khác", "{\"eventName\": \"search_book\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("Liên hệ thủ thư", "{\"eventName\": \"contact_librarian\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("⏰ Giờ mở cửa", "{\"eventName\": \"LibraryHours\", \"parameters\": {}}")
        );

        return createSuccessResponse(
            "Xin lỗi, chúng tôi không tìm thấy sách nào phù hợp với từ khóa \"" + bookTitle + "\". " +
            "Vui lòng thử tìm kiếm với từ khóa khác hoặc liên hệ với nhân viên thư viện để được hỗ trợ.",
            quickReplies,
            null,
            null,
            null
        );
    }
} 