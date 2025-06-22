package com.spkt.libraSys.service.chatbot;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {
    private String fulfillmentText;
    private Payload payload;

    public WebhookResponse(String fulfillmentText) {
        this.fulfillmentText = fulfillmentText;
        this.payload = new Payload();
    }

    public WebhookResponse(String fulfillmentText, List<QuickReply> quickReplies, List<Card> cards, 
                         Map<String, Object> customData, List<String> suggestions) {
        this.fulfillmentText = fulfillmentText;
        this.payload = new Payload(quickReplies, cards, customData, suggestions);
    }

    // Getters and Setters
    public String getFulfillmentText() {
        return fulfillmentText;
    }

    public void setFulfillmentText(String fulfillmentText) {
        this.fulfillmentText = fulfillmentText;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    // Payload class to contain all the response data
    public static class Payload {
        private List<QuickReply> quickReplies;
        private List<Card> cards;
        private Map<String, Object> customData;
        private List<String> suggestions;

        public Payload() {
        }

        public Payload(List<QuickReply> quickReplies, List<Card> cards, 
                      Map<String, Object> customData, List<String> suggestions) {
            this.quickReplies = quickReplies;
            this.cards = cards;
            this.customData = customData;
            this.suggestions = suggestions;
        }

        // Getters and Setters
        public List<QuickReply> getQuickReplies() {
            return quickReplies;
        }

        public void setQuickReplies(List<QuickReply> quickReplies) {
            this.quickReplies = quickReplies;
        }

        public List<Card> getCards() {
            return cards;
        }

        public void setCards(List<Card> cards) {
            this.cards = cards;
        }

        public Map<String, Object> getCustomData() {
            return customData;
        }

        public void setCustomData(Map<String, Object> customData) {
            this.customData = customData;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }
    }

    // Inner classes for structured data
    public static class QuickReply {
        private String text;
        private String payload;

        public QuickReply(String text, String payload) {
            this.text = text;
            this.payload = payload;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }
    }

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

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public List<Button> getButtons() {
            return buttons;
        }

        public void setButtons(List<Button> buttons) {
            this.buttons = buttons;
        }
    }

    public static class Button {
        private String text;
        private String payload;
        private String type;  // GET, POST, PUT, DELETE
        private String url;   // API endpoint

        public Button(String text, String payload) {
            this.text = text;
            this.payload = payload;
        }

        public Button(String text, String payload, String type, String url) {
            this.text = text;
            this.payload = payload;
            this.type = type;
            this.url = url;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
