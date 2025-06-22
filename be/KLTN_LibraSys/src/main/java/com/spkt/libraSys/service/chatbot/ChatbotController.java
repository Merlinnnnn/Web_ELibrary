package com.spkt.libraSys.service.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * Controller for handling chatbot webhook requests and responses.
 * Processes incoming webhook requests and returns appropriate responses.
 */
@RestController
@RequestMapping("/webhook")
@Slf4j
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody WebhookRequest request) {
        try {
            // Process request and get response
            WebhookResponse response = chatbotService.handleIntent(request);

            // Convert WebhookResponse to Map
            Map<String, Object> responseMap = objectMapper.convertValue(response, Map.class);

            return ResponseEntity.ok(responseMap);
        } catch (Exception e) {
            // Handle error and return default response
            WebhookResponse errorResponse = new WebhookResponse(
                "Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.",
                null,
                null,
                null,
                Arrays.asList("Thử lại", "Liên hệ hỗ trợ", "Quay lại trang chủ")
            );

            Map<String, Object> errorMap = objectMapper.convertValue(errorResponse, Map.class);
            return ResponseEntity.ok(errorMap);
        }
    }
}
