package com.spkt.libraSys.service.notification;

import java.util.Map;

public enum NotificationType {
    LOAN_CREATED(
            "Yêu cầu mượn sách được tạo",
            "Yêu cầu mượn sách '{bookName}' của bạn đã được tạo và đang chờ phê duyệt."
    ),
    LOAN_APPROVED(
            "Yêu cầu mượn sách được duyệt",
            "Yêu cầu mượn sách '{bookName}' của bạn đã được duyệt."
    ),
    BOOK_DUE_SOON(
            "Sắp đến hạn trả sách",
            "Sách '{bookName}' sẽ đến hạn trả trong {daysLeft} ngày."
    ),
    LOAN_REJECTED(
            "Yêu cầu mượn sách bị từ chối",
            "Yêu cầu mượn sách '{bookName}' đã bị từ chối. Lý do: {reason}"
    ),
    LOAN_RETURNED(
            "Sách đã được trả",
            "Sách '{bookName}' đã được trả thành công vào lúc {returnTime}."
    ),
    LOAN_AUTO_CANCAL(
            "Hủy mượn sách tự động",
            "Yêu cầu mượn sách cho tài liệu '{bookName}' đã bị hủy tự động do không xác nhận đúng hạn."
    ),
    LOAN_NEAR_DUE(
            "Sắp đến hạn trả sách",
            "Sách '{bookName}' sẽ đến hạn trả vào ngày {dueDate}."
    ),
    LOAN_FINE(
            "Khoản phạt sách",
            "Bạn có một khoản phạt sách là {fineAmount} VNĐ cho giao dịch #{loanId}."
    ),
    LOAN_FINE_PAID(
            "Khoản phạt sách đã thanh toán",
            "Bạn đã thanh toán khoản phạt sách là {fineAmount} VNĐ cho giao dịch #{loanId}."
    );

    private final String title;
    private final String contentTemplate;

    NotificationType(String title, String contentTemplate) {
        this.title = title;
        this.contentTemplate = contentTemplate;
    }

    public String formatContent(Map<String, Object> parameters) {
        String content = contentTemplate;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            content = content.replace("{" + entry.getKey() + "}",
                    String.valueOf(entry.getValue()));
        }
        return content;
    }

    public String getTitle() {
        return title;
    }
}