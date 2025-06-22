package com.spkt.libraSys.service.dialogflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.dialogflow.v2.*;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import com.google.protobuf.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DialogflowService {

    @org.springframework.beans.factory.annotation.Value("${dialogfow.project_id}")
    private String projectId;
    
    private final SessionsClient sessionsClient;
    
    @Autowired
    public DialogflowService(SessionsClient sessionsClient) {
        this.sessionsClient = sessionsClient;
    }

    public ChatResponse detectIntent(String message, String sessionId, String eventName, Map<String, Object> parameters) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "unknown";
        log.info("User: {}", username);

        try {
            SessionName session = SessionName.of(projectId, sessionId != null ? sessionId : "default-session");

            // Build parameters for EventInput
            Struct.Builder paramStructBuilder = Struct.newBuilder();
            if (parameters != null) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    paramStructBuilder.putFields(entry.getKey(),
                            Value.newBuilder().setStringValue(entry.getValue().toString()).build());
                }
            }

            QueryInput queryInput;
            if (eventName != null && !eventName.isEmpty()) {
                // Prioritize event, attach parameters to event
                EventInput eventInput = EventInput.newBuilder()
                        .setName(eventName)
                        .setLanguageCode("vi")
                        .setParameters(paramStructBuilder.build()) // Important
                        .build();

                queryInput = QueryInput.newBuilder()
                        .setEvent(eventInput)
                        .build();
            } else {
                // Fallback to text input
                queryInput = QueryInput.newBuilder()
                        .setText(TextInput.newBuilder().setText(message).setLanguageCode("vi").build())
                        .build();
            }

            // Custom payload 
            Struct customPayload = Struct.newBuilder()
                    .putFields("username", Value.newBuilder().setStringValue(username).build())
                    .build();

            QueryParameters queryParams = QueryParameters.newBuilder()
                    .setPayload(customPayload)
                    .build();

            DetectIntentRequest request = DetectIntentRequest.newBuilder()
                    .setSession(session.toString())
                    .setQueryInput(queryInput)
                    .setQueryParams(queryParams)
                    .build();

            log.info("Webhook request JSON:\n{}", JsonFormat.printer().print(request.getQueryInput()));

            DetectIntentResponse response = sessionsClient.detectIntent(request);
            QueryResult queryResult = response.getQueryResult();
            
            if (queryResult == null) {
                return createErrorResponse("Không có kết quả truy vấn được trả về");
            }
            // Log entire response in pretty JSON format
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setReply(queryResult.getFulfillmentText());
            
            if (queryResult.hasWebhookPayload()) {
                Struct webhookPayload = queryResult.getWebhookPayload();
                Map<String, Value> fields = webhookPayload.getFieldsMap();
                
                // Process quick replies
                if (fields.containsKey("quickReplies")) {
                    List<ChatResponse.QuickReply> quickReplies = new ArrayList<>();
                    Value quickRepliesValue = fields.get("quickReplies");
                    if (quickRepliesValue.hasListValue()) {
                        quickRepliesValue.getListValue().getValuesList().forEach(value -> {
                            if (value.hasStructValue()) {
                                Map<String, Value> quickReplyFields = value.getStructValue().getFieldsMap();
                                String text = quickReplyFields.get("text").getStringValue();
                                String replyPayload = quickReplyFields.get("payload").getStringValue();
                                quickReplies.add(new ChatResponse.QuickReply(text, replyPayload));
                            }
                        });
                    }
                    chatResponse.setQuickReplies(quickReplies);
                }
                
                // Process cards
                if (fields.containsKey("cards")) {
                    List<ChatResponse.Card> cards = new ArrayList<>();
                    Value cardsValue = fields.get("cards");
                    if (cardsValue.hasListValue()) {
                        cardsValue.getListValue().getValuesList().forEach(value -> {
                            if (value.hasStructValue()) {
                                Map<String, Value> cardFields = value.getStructValue().getFieldsMap();
                                String title = cardFields.get("title").getStringValue();
                                String subtitle = cardFields.get("subtitle").getStringValue();
                                String imageUrl = cardFields.get("imageUrl").getStringValue();
                                List<ChatResponse.Button> buttons = new ArrayList<>();
                                
                                if (cardFields.containsKey("buttons")) {
                                    cardFields.get("buttons").getListValue().getValuesList().forEach(buttonValue -> {
                                        if (buttonValue.hasStructValue()) {
                                            Map<String, Value> buttonFields = buttonValue.getStructValue().getFieldsMap();
                                            String buttonText = buttonFields.get("text").getStringValue();
                                            String buttonPayload = buttonFields.get("payload").getStringValue();
                                            String buttonLink = buttonFields.get("url").getStringValue();
                                            String buttonType = buttonFields.get("type").getStringValue();
                                            buttons.add(new ChatResponse.Button(buttonText, buttonPayload,buttonType, buttonLink));
                                        }
                                    });
                                }
                                
                                cards.add(new ChatResponse.Card(title, subtitle, imageUrl, buttons));
                            }
                        });
                    }
                    chatResponse.setCards(cards);
                }
                
                // Process custom data
                if (fields.containsKey("customData")) {
                    Value customDataValue = fields.get("customData");
                    if (customDataValue.hasStructValue()) {
                        Map<String, Object> customData = new HashMap<>();
                        customDataValue.getStructValue().getFieldsMap().forEach((key, value) -> {
                            if (value.hasStringValue()) {
                                customData.put(key, value.getStringValue());
                            } else if (value.hasNumberValue()) {
                                customData.put(key, value.getNumberValue());
                            } else if (value.hasBoolValue()) {
                                customData.put(key, value.getBoolValue());
                            }
                        });
                        chatResponse.setCustomData(customData);
                    }
                }
                
                // Process suggestions
                if (fields.containsKey("suggestions")) {
                    List<String> suggestions = new ArrayList<>();
                    Value suggestionsValue = fields.get("suggestions");
                    if (suggestionsValue.hasListValue()) {
                        suggestionsValue.getListValue().getValuesList().forEach(value -> {
                            if (value.hasStringValue()) {
                                suggestions.add(value.getStringValue());
                            }
                        });
                    }
                    chatResponse.setSuggestions(suggestions);
                }
            }
            
            return chatResponse;
            
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Không thể xử lý tin nhắn: " + e.getMessage());
        }
    }
    
    private ChatResponse createErrorResponse(String errorMessage) {
        ChatResponse response = new ChatResponse();
        response.setError(errorMessage);
        return response;
    }
} 