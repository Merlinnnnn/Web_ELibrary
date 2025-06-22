package com.spkt.libraSys.service.chatbot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing and registering intent handlers.
 * Provides functionality to scan and collect all intent handlers from the Spring context.
 */
@Service
public class IntentHandlerRegistry {

    private final ApplicationContext applicationContext;

    @Autowired
    public IntentHandlerRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    // Scan all IntentHandlers in Spring context
    public List<IntentHandler> getAllIntentHandlers() {
        return applicationContext.getBeansOfType(IntentHandler.class)
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    // Generate SYSTEM_PROMPT_INTENT from the list of IntentHandlers
    public String generateSystemPrompt() {
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("Bạn là một trợ lý thông minh cho hệ thống thư viện số. Dưới đây là các intent hệ thống hỗ trợ:\n");

        List<IntentHandler> handlers = getAllIntentHandlers();

        for (IntentHandler handler : handlers) {
            // Get intent name from getIntentName()
            String intentName = handler.getIntentName();
            // Use toString() to get description
            String description = handler.getToDesc();
            systemPrompt.append(String.format("- %s: %s\n", intentName, description));
        }

        systemPrompt.append("\nDựa trên câu hỏi của người dùng, hãy phân tích và trả về intent phù hợp nhất trong định dạng JSON như sau:\n");
        systemPrompt.append("{\"intent\": \"<intent_name>\"}\n");
        systemPrompt.append("Nếu không xác định được, trả về: {\"intent\": \"unknown\"}");

        return systemPrompt.toString();
    }
}
