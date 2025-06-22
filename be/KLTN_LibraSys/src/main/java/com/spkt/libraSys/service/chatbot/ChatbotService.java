package com.spkt.libraSys.service.chatbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spkt.libraSys.service.access.AuthRequest;
import com.spkt.libraSys.service.access.AuthResponse;
import com.spkt.libraSys.service.access.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service class for handling chatbot interactions and intent processing.
 * Manages different intent handlers and provides appropriate responses for user queries.
 */
@Slf4j
@Service
public class ChatbotService {

    private final List<IntentHandler> handlers;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public ChatbotService(List<IntentHandler> handlers, AuthService authService) {
        this.handlers = handlers;
        this.authService = authService;
        this.objectMapper = new ObjectMapper();
    }

    public WebhookResponse handleIntent(WebhookRequest request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonRequest = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            log.info("Incoming WebhookRequest:\n{}", jsonRequest);

            Map<String, Object> payload = request.getOriginalDetectIntentRequest().getPayload();
            String username = payload != null && payload.get("username") != null
                    ? payload.get("username").toString()
                    : "unknown";

            log.info("Username : {}", username);

            String intent = request.getQueryResult().getIntent().getDisplayName();
            log.info("Handling intent: {}", intent);

            // Find appropriate handler for the intent
            Optional<IntentHandler> handler = handlers.stream()
                    .filter(h -> h.getIntentName().equals(intent))
                    .findFirst();

            if (handler.isPresent()) {
                // Process intent using the corresponding handler
                return handler.get().handle(request);
            } else {
                // No suitable handler found
                return createUnknownIntentResponse(intent);
            }
        } catch (Exception e) {
            log.error("Error handling intent: {}", e.getMessage(), e);
            return createErrorResponse(e);
        }
    }

    private WebhookResponse createUnknownIntentResponse(String intent) {
        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("Tìm sách", "search_book"),
            new WebhookResponse.QuickReply("Mượn sách", "borrow_book"),
            new WebhookResponse.QuickReply("Trả sách", "return_book")
        );

        List<String> suggestions = Arrays.asList(
            "Tìm sách theo tác giả",
            "Tìm sách theo thể loại",
            "Xem sách đang mượn"
        );

        return new WebhookResponse(
            "Xin lỗi, tôi không hiểu yêu cầu của bạn. Bạn có thể thử một trong các tùy chọn sau:",
            quickReplies,
            null,
            null,
            suggestions
        );
    }

    private WebhookResponse createErrorResponse(Exception e) {
        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("Thử lại", "retry"),
            new WebhookResponse.QuickReply("Liên hệ hỗ trợ", "contact_support")
        );

        List<String> suggestions = Arrays.asList(
            "Quay lại trang chủ",
            "Xem hướng dẫn sử dụng",
            "Liên hệ thủ thư"
        );

        return new WebhookResponse(
            "Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau hoặc liên hệ với chúng tôi để được hỗ trợ.",
            quickReplies,
            null,
            null,
            suggestions
        );
    }
}
