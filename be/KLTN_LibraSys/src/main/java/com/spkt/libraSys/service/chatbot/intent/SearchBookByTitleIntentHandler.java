package com.spkt.libraSys.service.chatbot.intent;

import com.spkt.libraSys.service.chatbot.IntentHandler;
import com.spkt.libraSys.service.chatbot.WebhookRequest;
import com.spkt.libraSys.service.chatbot.WebhookResponse;
import com.spkt.libraSys.service.document.DocumentService;
import com.spkt.libraSys.service.document.DocumentResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchBookByTitleIntentHandler implements IntentHandler {

    @Autowired
    private DocumentService documentService;

    @Override
    public String getIntentName() {
        return "SearchBookByTitle";
    }

    @Override
    public String getToDesc() {
        return "Tìm kiếm sách theo tiêu đề";
    }

    @Override
    public WebhookResponse handle(WebhookRequest request) {
        String bookTitle = getStringParameter(request, "bookTitle");
        if (bookTitle == null) {
            return createErrorResponse("Vui lòng cung cấp tên sách cần tìm");
        }
        System.out.println("searchBook:bookTitle: " + bookTitle);

        // Tạo PageRequest với kích thước trang là 10 để đảm bảo có đủ sách
        PageRequest pageRequest = PageRequest.of(0, 10);
        // Lấy danh sách sách với thông tin cần thiết
        Page<DocumentResponseDto> documents = documentService.getAllDocuments(pageRequest);
        
        if (documents.isEmpty()) {
            return createErrorResponse("Xin lỗi, không tìm thấy sách nào với tiêu đề " + bookTitle);
        }

        List<WebhookResponse.Card> cards = new ArrayList<>();
        String searchTitle = bookTitle.toLowerCase();
        
        // Tìm các sách có tiêu đề gần với từ khóa tìm kiếm
        List<DocumentResponseDto> relevantBooks = documents.getContent().stream()
            .filter(doc -> {
                String docTitle = doc.getDocumentName().toLowerCase();
                return docTitle.contains(searchTitle) || 
                       searchTitle.contains(docTitle) ||
                       calculateSimilarity(docTitle, searchTitle) > 0.5;
            })
            .collect(Collectors.toList());
            
        // Nếu không tìm thấy sách phù hợp, lấy thêm sách
        if (relevantBooks.isEmpty()) {
            // Lấy thêm sách từ danh sách gốc
            relevantBooks = documents.getContent().stream()
                .limit(3)
                .collect(Collectors.toList());
        }
        
        // Thêm sách vào danh sách kết quả
        for (DocumentResponseDto doc : relevantBooks) {
            if (cards.size() >= 3) break;
            
            List<WebhookResponse.Button> buttons = new ArrayList<>();
            WebhookResponse.Button button = new WebhookResponse.Button(
                "Xem chi tiết",
                null,
                "GET",
                "/api/v1/documents/" + doc.getDocumentId()
            );
            buttons.add(button);
            
            WebhookResponse.Card card = new WebhookResponse.Card(
                doc.getDocumentName(),
                "Tác giả: " + doc.getAuthor(),
                doc.getCoverImage(),
                buttons
            );
            
            cards.add(card);
        }
        
        if (cards.isEmpty()) {
            return createErrorResponse("Xin lỗi, không tìm thấy sách nào với tiêu đề " + bookTitle);
        }
        
        String responseText;
        if (relevantBooks.size() > 0 && relevantBooks.get(0).getDocumentName().toLowerCase().contains(searchTitle)) {
            responseText = String.format("Tìm thấy %d sách phù hợp với tiêu đề '%s':", relevantBooks.size(), bookTitle);
        } else {
            responseText = String.format("Không tìm thấy sách phù hợp với tiêu đề '%s'. Dưới đây là một số sách có thể bạn quan tâm:", bookTitle);
        }
        
        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("🔍 Tìm sách khác", "{\"eventName\": \"SearchBookByTitle\", \"parameters\": {\"bookTitle\": \"\"}}"),
            new WebhookResponse.QuickReply("📚 Mượn sách", "{\"eventName\": \"BorrowBook\", \"parameters\": {\"bookTitle\": \"" + bookTitle + "\"}}"),
            new WebhookResponse.QuickReply("📖 Xem sách mới", "{\"eventName\": \"SearchTopBook\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("⏰ Giờ mở cửa", "{\"eventName\": \"LibraryHours\", \"parameters\": {}}")
        );

        List<String> suggestions = Arrays.asList(
            "Tìm sách theo tác giả",
            "Tìm sách theo thể loại",
            "Xem sách đang mượn"
        );
        
        return createSuccessResponse(
            responseText,
            quickReplies,
            cards,
            null,
            suggestions
        );
    }
    
    // Tính độ tương đồng giữa 2 chuỗi
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        
        // Chuyển về chữ thường và loại bỏ khoảng trắng
        s1 = s1.toLowerCase().replaceAll("\\s+", "");
        s2 = s2.toLowerCase().replaceAll("\\s+", "");
        
        if (s1.equals(s2)) return 1.0;
        
        // Tính độ dài của chuỗi ngắn nhất
        int minLength = Math.min(s1.length(), s2.length());
        if (minLength == 0) return 0.0;
        
        // Đếm số ký tự giống nhau
        int matches = 0;
        for (int i = 0; i < minLength; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                matches++;
            }
        }
        
        // Tính tỷ lệ tương đồng
        return (double) matches / Math.max(s1.length(), s2.length());
    }
} 