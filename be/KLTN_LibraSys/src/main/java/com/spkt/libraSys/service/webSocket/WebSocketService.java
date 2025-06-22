package com.spkt.libraSys.service.webSocket;

import com.spkt.libraSys.service.loan.LoanResponse;
import com.spkt.libraSys.service.notification.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send notification to specific user via WebSocket
     *
     * @param userId      ID of the user to receive notification
     * @param notification Notification content
     */
    public void sendNotificationToUser(String userId , NotificationResponse notification) {
        messagingTemplate.convertAndSendToUser(userId , "/queue/notifications", notification);
    }

    /**
     * Send loan status update to user
     *
     * @param userId      ID of the user
     * @param loanResponse Loan status update information
     */
    public void sendUpdateStatusLoan(String userId , LoanResponse loanResponse){
        messagingTemplate.convertAndSendToUser(userId , "/queue/loans", loanResponse);
    }
}