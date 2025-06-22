package com.spkt.libraSys.service.chatbot;

import java.util.Map;

/**
 * Interface for handling different types of chatbot intents.
 * Provides methods for intent processing and response generation.
 */
public interface IntentHandler {
    /**
     * Returns the name of the intent
     */
    String getIntentName();

    /**
     * Returns the description of the intent
     */
    String getToDesc();

    /**
     * Processes the intent and returns a response
     * @param request WebhookRequest from Dialogflow
     * @return WebhookResponse for Dialogflow
     */
    WebhookResponse handle(WebhookRequest request);

    /**
     * Checks if the intent matches the request
     * @param intentName Intent name from request
     * @return true if matches, false otherwise
     */
    default boolean matches(String intentName) {
        return getIntentName().equals(intentName);
    }

    /**
     * Gets a parameter from the request
     * @param request WebhookRequest
     * @param paramName Parameter name
     * @return Parameter value or null if it doesn't exist
     */
    default Object getParameter(WebhookRequest request, String paramName) {
        if (request == null || request.getQueryResult() == null || 
            request.getQueryResult().getParameters() == null) {
           return null;
        }
        return request.getQueryResult().getParameters().get(paramName);
    }

    /**
     * Gets a String parameter from the request
     * @param request WebhookRequest
     * @param paramName Parameter name
     * @return String parameter value or null if it doesn't exist
     */
    default String getStringParameter(WebhookRequest request, String paramName) {
        Object value = getParameter(request, paramName);
        return value != null ? value.toString() : null;
    }

    /**
     * Creates an error response
     * @param message Error message
     * @return WebhookResponse with error message
     */
    default WebhookResponse createErrorResponse(String message) {
        return new WebhookResponse(
            message,
            null,
            null,
            null,
            null
        );
    }

    /**
     * Creates a success response
     * @param message Success message
     * @param quickReplies List of quick replies
     * @param cards List of cards
     * @param customData Custom data
     * @param suggestions List of suggestions
     * @return WebhookResponse with complete information
     */
    default WebhookResponse createSuccessResponse(
            String message,
            java.util.List<WebhookResponse.QuickReply> quickReplies,
            java.util.List<WebhookResponse.Card> cards,
            Map<String, Object> customData,
            java.util.List<String> suggestions) {
        return new WebhookResponse(message, quickReplies, cards, customData, suggestions);
    }
}
