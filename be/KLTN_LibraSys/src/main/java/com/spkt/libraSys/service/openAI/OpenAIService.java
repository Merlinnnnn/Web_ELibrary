package com.spkt.libraSys.service.openAI;

import com.spkt.libraSys.service.chatbot.IntentHandlerRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudinary.json.JSONException;
import org.cloudinary.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIService {

    private final IntentHandlerRegistry intentHandlerRegistry;


    @Value("${OpenAI.apiKey}")
    protected String apiKey;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SYSTEM_PROMPT_INTENT = """
        Bạn là một trợ lý thông minh cho hệ thống thư viện số. Dưới đây là các intent hệ thống hỗ trợ:
        - get_document_title: Lấy tên cuốn sách
        - search_documents: Tìm tài liệu theo chủ đề
        - get_author_info: Lấy thông tin tác giả

        Dựa trên câu hỏi của người dùng, hãy phân tích và trả về intent phù hợp nhất trong định dạng JSON như sau:
        {"intent": "<intent_name>"}
        Nếu không xác định được, trả về: {"intent": "unknown"}
        """;


    /**
     * Tóm tắt nội dung văn bản
     */
    public String summarizeTextWithOpenAI(String text, String previousContext) {
        String prompt = previousContext + "\nTóm tắt văn bản sau một cách ngắn gọn, dễ hiểu:\n" + text;

        String response = callOpenAIChatAPI(List.of(
                new Message("user", prompt)
        ));

        return response != null ? response : "Không thể tóm tắt nội dung.";
    }

    /**
     * Dự đoán intent của người dùng
     */
    public String inferIntentFromUserQuery(String userQuery) {
        String systemPrompt = intentHandlerRegistry.generateSystemPrompt();
        String response = callOpenAIChatAPI(List.of(
                new Message("system", systemPrompt),
                new Message("user", userQuery)
        ));

        if (response == null) return "unknown";

        try {
            JSONObject json = new JSONObject(response.trim());
            return json.optString("intent", "unknown");
        } catch (JSONException e) {
            return "unknown";
        }
    }

    /**
     * Hàm chung gọi API chat completions của OpenAI
     */
    private String callOpenAIChatAPI(List<Message> messages) {
        try {
            // Tạo nội dung request
            List<Map<String, String>> msgList = new ArrayList<>();
            for (Message msg : messages) {
                msgList.add(Map.of("role", msg.role, "content", msg.content));
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", msgList);
            requestBody.put("temperature", 0.2);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    OPENAI_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map body = response.getBody();
            if (body != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (!choices.isEmpty()) {
                    Map msg = (Map) choices.get(0).get("message");
                    return (String) msg.get("content");
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // có thể log
        }
        return null;
    }

    public ModerationResult moderateDocumentContent(String fullText) {
        List<String> chunks = splitTextIntoChunks(fullText, 1000); // chia theo 1000 ký tự

        for (String chunk : chunks) {
            String prompt = String.format("""
            Bạn là một chuyên gia kiểm duyệt nội dung cho hệ thống thư viện số.

            Nhiệm vụ của bạn là phân tích đoạn văn dưới đây để xác định xem nó có chứa nội dung không phù hợp hay không, bao gồm nhưng không giới hạn ở các yếu tố như:
            - Bạo lực, khủng bố, tra tấn
            - Kích động thù hận, phân biệt chủng tộc, kỳ thị giới tính
            - Quấy rối, lạm dụng tình dục, khiêu dâm
            - Tin giả, xuyên tạc lịch sử, mê tín dị đoan
            - Nội dung trái đạo đức, vi phạm pháp luật hoặc không phù hợp với sinh viên

            Nếu đoạn văn có nội dung không phù hợp, hãy trả về:
            {"status": "REJECTED", "reason": "mô tả ngắn gọn lý do"}

            Nếu đoạn văn phù hợp và không vi phạm, trả về:
            {"status": "APPROVED"}

            Đây là đoạn văn cần kiểm duyệt:
            "%s"
            """, chunk);

            String response = callOpenAIChatAPI(List.of(new Message("user", prompt)));

            if (response != null) {
                try {
                    JSONObject json = new JSONObject(response.trim());
                    if ("REJECTED".equalsIgnoreCase(json.optString("status"))) {
                        return new ModerationResult("REJECTED", json.optString("reason", "Nội dung không phù hợp."));
                    }
                } catch (JSONException e) {
                    log.warn("⚠️ Không phân tích được phản hồi từ AI: {}", response);
                }
            }
        }

        return new ModerationResult("APPROVED", null);
    }
    private List<String> splitTextIntoChunks(String text, int maxChars) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }

    /**
     * Class nội bộ cho message
     */
    private static class Message {
        String role;
        String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class ModerationResult {
        @Getter
        public final String status;
        @Getter
        public final String reason;

        public ModerationResult(String status, String reason) {
            this.status = status;
            this.reason = reason;
        }


        public boolean isRejected() {
            return "REJECTED".equalsIgnoreCase(status);
        }
    }

}
