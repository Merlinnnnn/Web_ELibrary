package com.spkt.libraSys.service.chatbot.intent;

import com.spkt.libraSys.service.chatbot.IntentHandler;
import com.spkt.libraSys.service.chatbot.WebhookRequest;
import com.spkt.libraSys.service.chatbot.WebhookResponse;
import com.spkt.libraSys.service.chatbot.annotation.RequiresAuth;
import com.spkt.libraSys.service.document.*;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentEntity;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentRepository;
import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import com.spkt.libraSys.service.loan.LoanEntity;
import com.spkt.libraSys.service.loan.LoanRepository;
import com.spkt.libraSys.service.loan.LoanStatus;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@RequiresAuth(message = "Vui lòng đăng nhập để mượn sách")
public class BorrowBookIntentHandler implements IntentHandler {
    private final DocumentRepository documentRepository;
    private final PhysicalDocumentRepository physicalDocumentRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final DocumentService documentService;

    @Override
    public String getIntentName() {
        return "BorrowBook";
    }

    @Override
    public String getToDesc() {
        return "mượn sách";
    }

    private List<DocumentEntity> findBooksByTitle(String title) {
        Specification<DocumentEntity> spec = (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("documentName")),
                        "%" + title.toLowerCase() + "%"
                ),
                criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("documentCategory"), DocumentCategory.PHYSICAL),
                        criteriaBuilder.equal(root.get("documentCategory"), DocumentCategory.BOTH)
                ),
                criteriaBuilder.equal(root.get("status"), DocumentStatus.ENABLED),
                criteriaBuilder.equal(root.get("approvalStatus"), ApprovalStatus.APPROVED)
        );

        return documentRepository.findAll(spec, PageRequest.of(0, 3)).getContent();
    }

    @Override
    public WebhookResponse handle(WebhookRequest request) {
        Map<String, Object> payload = request.getOriginalDetectIntentRequest().getPayload();
        String username = payload.get("username").toString();
        try {
            String bookTitle = getStringParameter(request, "bookTitle");
            if (bookTitle == null || bookTitle.trim().isEmpty()) {
                return createEmptyTitleResponse();
            }

            // Tìm user theo email
            Optional<UserEntity> userOptional = userRepository.findByUsername(username);
            if (userOptional.isEmpty()) {
                return createUserNotFoundResponse(username);
            }

            UserEntity user = userOptional.get();

            // Tìm sách theo tên
            System.out.println("bookTitle: " + bookTitle);
            List<DocumentEntity> matchingBooks = findBooksByTitle(bookTitle);
            System.out.println("bookTitle: " + matchingBooks.size());
            if (matchingBooks.isEmpty()) {
                return createNoBookFoundResponse(bookTitle);
            }

            // Tạo danh sách cards cho mỗi cuốn sách
            List<WebhookResponse.Card> cards = new ArrayList<>();
            for (DocumentEntity book : matchingBooks) {
                PhysicalDocumentEntity physicalBook = book.getPhysicalDocument();
                if (physicalBook != null) {
                    List<WebhookResponse.Button> buttons = Arrays.asList(
                        new WebhookResponse.Button(
                            "Mượn sách",
                            "{\"physicalDocId\": \"" + physicalBook.getPhysicalDocumentId() + "\"}",
                            "POST",
                            "/api/v1/loans"
                        ),
                        new WebhookResponse.Button(
                            "Chi tiết sách",
                            "{\"documentId\": \"" + book.getDocumentId() + "\"}",
                            "GET",
                            "/api/v1/documents/" + book.getDocumentId()
                        )
                    );

                    WebhookResponse.Card card = new WebhookResponse.Card(
                        book.getDocumentName(),
                        "Còn " + physicalBook.getAvailableCopies() + " bản có sẵn",
                        book.getCoverImage(),
                        buttons
                    );
                    cards.add(card);
                }
            }

            // Quick replies cho các hành động phổ biến
            List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
                new WebhookResponse.QuickReply("🔍 Tìm sách khác", "{\"eventName\": \"SearchBookByTitle\", \"parameters\": {\"bookTitle\": \"\"}}"),
                new WebhookResponse.QuickReply("📚 Xem sách mới", "{\"eventName\": \"SearchTopBook\", \"parameters\": {}}"),
                new WebhookResponse.QuickReply("⏰ Giờ mở cửa", "{\"eventName\": \"LibraryHours\", \"parameters\": {}}")
            );

            // Suggestions cho các tùy chọn khác
            List<String> suggestions = Arrays.asList(
                "Tìm sách theo tác giả",
                "Tìm sách theo thể loại",
                "Xem sách đang mượn"
            );

            return createSuccessResponse(
                "Tìm thấy " + cards.size() + " sách phù hợp với từ khóa \"" + bookTitle + "\". " +
                "Vui lòng chọn sách bạn muốn mượn:",
                quickReplies,
                cards,
                null,
                suggestions
            );

        } catch (Exception e) {
            return createErrorResponse("Đã có lỗi xảy ra khi xử lý yêu cầu mượn sách. Vui lòng thử lại sau.");
        }
    }

    private WebhookResponse createEmptyTitleResponse() {
        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("🔍 Tìm sách", "{\"eventName\": \"SearchBookByTitle\", \"parameters\": {\"bookTitle\": \"\"}}"),
            new WebhookResponse.QuickReply("📚 Danh sách sách mới", "{\"eventName\": \"SearchTopBook\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("⏰ Giờ mở cửa", "{\"eventName\": \"LibraryHours\", \"parameters\": {}}")
        );

        List<String> suggestions = Arrays.asList(
            "Tìm sách theo tác giả",
            "Tìm sách theo thể loại",
            "Xem sách đang mượn"
        );

        return createSuccessResponse(
            "Vui lòng cung cấp tên sách bạn muốn mượn.",
            quickReplies,
            null,
            null,
            suggestions
        );
    }

    private WebhookResponse createUserNotFoundResponse(String username) {
        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("📝 Đăng ký", "{\"eventName\": \"DefaultWelcomeIntent\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("📞 Liên hệ hỗ trợ", "{\"eventName\": \"DefaultFallbackIntent\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("⏰ Giờ mở cửa", "{\"eventName\": \"LibraryHours\", \"parameters\": {}}")
        );

        List<String> suggestions = Arrays.asList(
            "Kiểm tra lại email",
            "Quên mật khẩu",
            "Hướng dẫn đăng ký"
        );

        return createSuccessResponse(
            "Không tìm thấy tài khoản với username " + username + ". Vui lòng kiểm tra lại hoặc đăng ký tài khoản mới.",
            quickReplies,
            null,
            null,
            suggestions
        );
    }

    private WebhookResponse createNoBookFoundResponse(String bookTitle) {
        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("🔍 Tìm sách khác", "{\"eventName\": \"SearchBookByTitle\", \"parameters\": {\"bookTitle\": \"\"}}"),
            new WebhookResponse.QuickReply("📞 Liên hệ thủ thư", "{\"eventName\": \"DefaultFallbackIntent\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("⏰ Giờ mở cửa", "{\"eventName\": \"LibraryHours\", \"parameters\": {}}")
        );

        List<String> suggestions = Arrays.asList(
            "Kiểm tra lại tên sách",
            "Tìm sách tương tự",
            "Đặt trước sách"
        );

        return createSuccessResponse(
            "Xin lỗi, chúng tôi không tìm thấy sách \"" + bookTitle + "\" trong thư viện. " +
            "Vui lòng kiểm tra lại tên sách hoặc liên hệ với nhân viên thư viện để được hỗ trợ.",
            quickReplies,
            null,
            null,
            suggestions
        );
    }
}
