package com.spkt.libraSys.service.dialogflow;

import java.util.Map;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String sessionId;
    private String eventName;
    private Map<String, Object> parameters;
} 