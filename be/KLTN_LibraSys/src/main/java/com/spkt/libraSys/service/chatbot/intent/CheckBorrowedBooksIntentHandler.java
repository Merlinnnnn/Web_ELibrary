package com.spkt.libraSys.service.chatbot.intent;

import com.spkt.libraSys.service.chatbot.IntentHandler;
import com.spkt.libraSys.service.chatbot.WebhookRequest;
import com.spkt.libraSys.service.chatbot.WebhookResponse;
import com.spkt.libraSys.service.chatbot.annotation.RequiresAuth;
import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.loan.LoanEntity;
import com.spkt.libraSys.service.loan.LoanRepository;
import com.spkt.libraSys.service.loan.LoanStatus;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@RequiresAuth(message = "Vui lòng đăng nhập để xem sách đang mượn")
public class CheckBorrowedBooksIntentHandler implements IntentHandler {
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;

    @Override
    public String getIntentName() {
        return "CheckBorrowedBooks";
    }

    @Override
    public String getToDesc() {
        return "Kiểm tra sách đang mượn";
    }

    @Override
    public WebhookResponse handle(WebhookRequest request) {
        Map<String, Object> payload = request.getOriginalDetectIntentRequest().getPayload();
        String username = payload.get("username").toString();

        Optional<UserEntity> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            return createUserNotFoundResponse(username);
        }
        System.out.println("checkBorrowBook:userOptional: " + userOptional.get());
        UserEntity user = userOptional.get();
        Pageable pageable = PageRequest.of(0, 10);
        Page<LoanEntity> borrowedBooks = loanRepository.findByUserEntity(user, pageable);

        if (borrowedBooks.isEmpty()) {
            return createNoBorrowedBooksResponse();
        }

        return createBorrowedBooksResponse(user, borrowedBooks);
    }

    private WebhookResponse createUserNotFoundResponse(String username) {
        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("Đăng ký", "register"),
            new WebhookResponse.QuickReply("Liên hệ hỗ trợ", "contact_support")
        );

        List<String> suggestions = Arrays.asList(
            "Kiểm tra lại tài khoản",
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

    private WebhookResponse createNoBorrowedBooksResponse() {
        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("Tìm sách", "search_book"),
            new WebhookResponse.QuickReply("Sách mới", "new_books")
        );

        List<String> suggestions = Arrays.asList(
            "Xem sách phổ biến",
            "Tìm sách theo thể loại",
            "Đặt sách trước"
        );

        return createSuccessResponse(
            "Bạn hiện không mượn sách nào từ thư viện.",
            quickReplies,
            null,
            null,
            suggestions
        );
    }

    private WebhookResponse createBorrowedBooksResponse(UserEntity user, Page<LoanEntity> borrowedBooks) {
        StringBuilder response = new StringBuilder();
        response.append("Sách bạn đang mượn:\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<WebhookResponse.Card> cards = new ArrayList<>();
        
        for (LoanEntity loan : borrowedBooks.getContent()) {
            if (loan.getStatus() == LoanStatus.BORROWED) {
                DocumentEntity document = loan.getPhysicalDoc().getDocument();
                String dueDate = loan.getDueDate().format(formatter);
                
                response.append("- ")
                       .append(document.getDocumentName())
                       .append(" (Hạn trả: ")
                       .append(dueDate)
                       .append(")")
                       .append(loan.isOverdue() ? " - QUÁ HẠN" : "")
                       .append("\n");

                // Create card for each book
                List<WebhookResponse.Button> buttons = Arrays.asList(
                    new WebhookResponse.Button(
                        "Chi tiết",
                        "{\"documentId\": \"" + document.getDocumentId() + "\"}",
                        "GET",
                        "/api/v1/documents/" + document.getDocumentId()
                    )
                );

                WebhookResponse.Card card = new WebhookResponse.Card(
                    document.getDocumentName(),
                    "Hạn trả: " + dueDate + (loan.isOverdue() ? " - QUÁ HẠN" : ""),
                    document.getCoverImage(),
                    buttons
                );
                cards.add(card);
            }
        }
        
        response.append("\nTổng số sách đang mượn: ").append(user.getCurrentBorrowedCount());
        response.append("\nGiới hạn mượn: ").append(user.getMaxBorrowLimit());

        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("Tìm sách mới", "{\"eventName\": \"search_book\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("Sách phổ biến", "{\"eventName\": \"popular_books\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("Trả sách", "{\"eventName\": \"return_book\", \"parameters\": {}}")
        );

        List<String> suggestions = Arrays.asList(
            "Quy định mượn sách",
            "Hướng dẫn trả sách",
            "Liên hệ thủ thư"
        );

        Map<String, Object> customData = new HashMap<>();
        customData.put("totalBorrowed", user.getCurrentBorrowedCount());
        customData.put("maxLimit", user.getMaxBorrowLimit());
        customData.put("hasOverdue", borrowedBooks.getContent().stream().anyMatch(LoanEntity::isOverdue));

        return createSuccessResponse(
            response.toString(),
            quickReplies,
            cards,
            customData,
            suggestions
        );
    }
}