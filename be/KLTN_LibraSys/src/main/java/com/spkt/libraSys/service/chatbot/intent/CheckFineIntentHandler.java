package com.spkt.libraSys.service.chatbot.intent;

import com.spkt.libraSys.service.chatbot.IntentHandler;
import com.spkt.libraSys.service.chatbot.WebhookRequest;
import com.spkt.libraSys.service.chatbot.WebhookResponse;
import com.spkt.libraSys.service.chatbot.annotation.RequiresAuth;
import com.spkt.libraSys.service.loan.LoanEntity;
import com.spkt.libraSys.service.loan.LoanRepository;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@RequiresAuth(message = "Vui lòng đăng nhập để kiểm tra tiền phạt")
public class CheckFineIntentHandler implements IntentHandler {
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;

    @Override
    public String getIntentName() {
        return "CheckFine";
    }

    @Override
    public WebhookResponse handle(WebhookRequest request) {
        Map<String, Object> payload = request.getOriginalDetectIntentRequest().getPayload();
        String username = payload.get("username").toString();

        Optional<UserEntity> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return createUserNotFoundResponse(username);
        }

        UserEntity user = userOptional.get();
        List<LoanEntity> unpaidLoans = loanRepository.findByUserEntityAndPaymentStatus(
            user, 
            LoanEntity.PaymentStatus.UNPAID
        );

        double totalFine = unpaidLoans.stream()
            .mapToDouble(LoanEntity::getFineAmount)
            .sum();

        if (totalFine == 0) {
            return createNoFineResponse();
        }

        return createFineResponse(user, totalFine, unpaidLoans);
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

    private WebhookResponse createNoFineResponse() {
        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("Mượn sách", "borrow_book"),
            new WebhookResponse.QuickReply("Tìm sách", "search_book")
        );

        List<String> suggestions = Arrays.asList(
            "Sách mới cập nhật",
            "Sách phổ biến",
            "Quy định mượn sách"
        );

        return createSuccessResponse(
            "Bạn hiện không có khoản phạt nào cần thanh toán.",
            quickReplies,
            null,
            null,
            suggestions
        );
    }

    private WebhookResponse createFineResponse(UserEntity user, double totalFine, List<LoanEntity> unpaidLoans) {
        StringBuilder response = new StringBuilder();
        response.append("Tài khoản ").append(user.getUsername()).append(" đang có ").append(totalFine).append("đ tiền phạt.\n\n");
        response.append("Chi tiết các khoản phạt:\n");
        
        List<WebhookResponse.Card> cards = new ArrayList<>();
        for (LoanEntity loan : unpaidLoans) {
            String status = "";
            switch (loan.getReturnCondition()) {
                case DAMAGED:
                    status = "Sách bị hư hỏng";
                    break;
                case OVERDUE:
                    status = "Quá hạn trả";
                    break;
                default:
                    status = "Chưa thanh toán";
            }

            response.append("- ").append(loan.getPhysicalDoc().getDocument().getDocumentName())
                   .append(": ").append(loan.getFineAmount()).append("đ")
                   .append(" (").append(status).append(")\n");

            // Tạo card cho từng khoản vay
            List<WebhookResponse.Button> buttons = new ArrayList<>();
            buttons.add(new WebhookResponse.Button(
                "Thanh toán qua VNPay",
                "",
                "GET",
                "/api/v1/vnpay/submitOrder/" + loan.getTransactionId()
            ));

            WebhookResponse.Card card = new WebhookResponse.Card(
                loan.getPhysicalDoc().getDocument().getDocumentName(),
                "Tiền phạt: " + loan.getFineAmount() + "đ\n" +
                "Ngày trả: " + loan.getReturnDate() + "\n" +
                "Trạng thái: " + status,
                null,
                buttons
            );
            cards.add(card);
        }

        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("Thanh toán phí", "{\"eventName\": \"pay_fine\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("Xem chi tiết", "{\"eventName\": \"view_fine_details\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("Liên hệ hỗ trợ", "{\"eventName\": \"contact_support\", \"parameters\": {}}")
        );

        List<String> suggestions = Arrays.asList(
            "Quy định phí trễ hạn",
            "Hướng dẫn thanh toán",
            "Liên hệ thủ thư"
        );

        Map<String, Object> customData = new HashMap<>();
        customData.put("totalFine", totalFine);
        customData.put("unpaidLoansCount", unpaidLoans.size());
        customData.put("hasOverdue", unpaidLoans.stream().anyMatch(loan -> loan.getReturnCondition() == LoanEntity.Condition.OVERDUE));
        customData.put("hasDamaged", unpaidLoans.stream().anyMatch(loan -> loan.getReturnCondition() == LoanEntity.Condition.DAMAGED));

        return createSuccessResponse(
            response.toString(),
            quickReplies,
            cards,
            customData,
            suggestions
        );
    }

    @Override
    public String getToDesc() {
        return "Kiểm tra tiền phạt của người dùng.";
    }
}
