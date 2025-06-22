package com.spkt.libraSys.service.dialogflow;

import com.spkt.libraSys.exception.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final DialogflowService dialogflowService;

    @Autowired
    public ChatController(DialogflowService dialogflowService) {
        this.dialogflowService = dialogflowService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String ,Object>>> chat(@RequestBody ChatRequest request) {
        ChatResponse response = dialogflowService.detectIntent(request.getMessage(), request.getSessionId(),request.getEventName(),request.getParameters());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reply", response.getReply());
        data.put("quickReplies", response.getQuickReplies());
        data.put("cards", response.getCards());
        data.put("suggestions", response.getSuggestions());
        data.put("customData", response.getCustomData());
        data.put("error", response.getError());

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .message("Đã xử lý tin nhắn thành công")
                .data(data)
                .build());
    }
} 