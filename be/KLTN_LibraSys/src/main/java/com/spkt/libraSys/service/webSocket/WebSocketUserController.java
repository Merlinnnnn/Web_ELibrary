package com.spkt.libraSys.service.webSocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/users")
public class WebSocketUserController {

    private final WebSocketUserTracker webSocketUserTracker;
    private final SimpMessagingTemplate messagingTemplate;


    public WebSocketUserController(WebSocketUserTracker webSocketUserTracker, SimpMessagingTemplate messagingTemplate) {
        this.webSocketUserTracker = webSocketUserTracker;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/online")
    public Set<String> getOnlineUsers() {
        return webSocketUserTracker.getConnectedUsers();
    }
    @PostMapping("/send")
    public String sendToUser(@RequestParam String userId, @RequestParam String message) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", message);
        System.out.println("✅ Đã gửi thông báo đến người dùng: " + userId + " | Nội dung: " + message);
        return "Đã gửi tin nhắn đến người dùng " + userId;
    }
}
