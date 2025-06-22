package com.spkt.libraSys.service.dialogflow;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatResponse {
    private String reply;
    private List<QuickReply> quickReplies;
    private List<Card> cards;
    private Map<String, Object> customData;
    private List<String> suggestions;
    private String error;

    @Data
    public static class QuickReply {
        private String text;
        private String payload;

        public QuickReply(String text, String payload) {
            this.text = text;
            this.payload = payload;
        }
    }

    @Data
    public static class Card {
        private String title;
        private String subtitle;
        private String imageUrl;
        private List<Button> buttons;

        public Card(String title, String subtitle, String imageUrl, List<Button> buttons) {
            this.title = title;
            this.subtitle = subtitle;
            this.imageUrl = imageUrl;
            this.buttons = buttons;
        }
    }

    @Data
    public static class Button {
        private String type;
        private String text;
        private String payload;
        private String url;

        public Button(String text, String payload, String type, String url) {
            this.text = text;
            this.payload = payload;
            this.type = type;
            this.url = url;
        }
    }
} 