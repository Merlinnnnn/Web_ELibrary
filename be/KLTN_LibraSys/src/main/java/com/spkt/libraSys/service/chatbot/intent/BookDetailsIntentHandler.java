package com.spkt.libraSys.service.chatbot.intent;

import com.spkt.libraSys.service.chatbot.IntentHandler;
import com.spkt.libraSys.service.chatbot.WebhookRequest;
import com.spkt.libraSys.service.chatbot.WebhookResponse;
import com.spkt.libraSys.service.chatbot.annotation.RequiresAuth;
import com.spkt.libraSys.service.document.DocumentService;
import com.spkt.libraSys.service.document.DocumentResponseDto;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeEntity;
import com.spkt.libraSys.service.document.course.CourseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookDetailsIntentHandler implements IntentHandler {

    @Autowired
    private DocumentService documentService;

    @Override
    public String getIntentName() {
        return "BookDetails";
    }

    @Override
    public String getToDesc() {
        return "Xem thông tin chi tiết sách";
    }

    private String getCategoryDisplay(String category) {
        if (category == null) return "Chưa cập nhật";
        
        switch (category) {
            case "PHYSICAL":
                return "Sách vật lý";
            case "DIGITAL":
                return "Tài liệu số";
            case "BOTH":
                return "Cả sách vật lý và tài liệu số";
            default:
                return category;
        }
    }

    private String getVisibilityStatusDisplay(String status) {
        if (status == null) return "Chưa cập nhật";
        
        switch (status) {
            case "PUBLIC":
                return "Công khai";
            case "RESTRICTED_VIEW":
                return "Xem có giới hạn";
            case "PRIVATE":
                return "Riêng tư";
            default:
                return status;
        }
    }

    @Override
    public WebhookResponse handle(WebhookRequest request) {
        // Kiểm tra nếu request là từ button click
        log.info("Request from button click: {}", request);
        String buttonPayload = getStringParameter(request, "payload");
        if (buttonPayload != null) {
            return handleButtonAction(buttonPayload, request);
        }

        String bookTitle = getStringParameter(request, "book-title");
        if (bookTitle == null) {
            // Events khi chưa có tên sách
            List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
                new WebhookResponse.QuickReply("🔍 Tìm sách", "{\"eventName\": \"SearchBookByTitle\", \"parameters\": {\"bookTitle\": \"\"}}"),
                new WebhookResponse.QuickReply("📚 Sách mới", "{\"eventName\": \"SearchTopBook\", \"parameters\": {}}"),
                new WebhookResponse.QuickReply("⏰ Giờ mở cửa", "{\"eventName\": \"LibraryHours\", \"parameters\": {}}")
            );

            // Suggestions cho các tìm kiếm phổ biến
            List<String> suggestions = Arrays.asList(
                "Tìm sách theo tác giả",
                "Tìm sách theo thể loại",
                "Xem sách đang mượn"
            );

            return createSuccessResponse(
                "Vui lòng cung cấp tên sách cần xem thông tin",
                quickReplies,
                null,
                null,
                suggestions
            );
        }
        
        try {
            DocumentResponseDto document = documentService.searchByTitle(bookTitle);

            if (document == null) {
                // Events khi không tìm thấy sách
                List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
                    new WebhookResponse.QuickReply("🔍 Tìm lại", "{\"eventName\": \"SearchBookByTitle\", \"parameters\": {\"bookTitle\": \"\"}}"),
                    new WebhookResponse.QuickReply("📚 Xem sách mới", "{\"eventName\": \"SearchTopBook\", \"parameters\": {}}")
                );

                // Suggestions cho các tùy chọn khác
                List<String> suggestions = Arrays.asList(
                    "Kiểm tra lại tên sách",
                    "Xem sách phổ biến",
                    "Tìm sách theo thể loại"
                );

                return createSuccessResponse(
                    "Không tìm thấy sách với tên: " + bookTitle,
                    quickReplies,
                    null,
                    null,
                    suggestions
                );
            }

            // Tạo card hiển thị thông tin sách
            List<WebhookResponse.Button> buttons = new ArrayList<>();
            if (document.getPhysicalDocument() != null) {
                buttons.add(new WebhookResponse.Button(
                        "📚 Mượn sách",
                        "{\"physicalDocId\": \"" + document.getPhysicalDocument().getPhysicalDocumentId() + "\"}",
                        "POST",
                        "/api/v1/loans"
                ));
            }
//            buttons.add(new WebhookResponse.Button(
//                    "📖 Đặt giữ sách",
//                    "{\"documentId\": \"" + document.getDocumentId() + "\", \"documentName\": \"" + document.getDocumentName() + "\", \"action\": \"reserve\"}",
//                    "POST",
//                    "/api/v1/loans/reserve"
//            ));
//            buttons.add(new WebhookResponse.Button(
//                    "🔍 Xem chi tiết",
//                    "{\"documentId\": \"" + document.getDocumentId() + "\", \"documentName\": \"" + document.getDocumentName() + "\"}",
//                    "GET",
//                    "/api/v1/documents/" + document.getDocumentId()
//            ));

            List<WebhookResponse.Card> cards = Arrays.asList(
                    new WebhookResponse.Card(
                            document.getDocumentName(),
                            document.getAuthor() != null ? document.getAuthor() : "Chưa cập nhật",
                            document.getCoverImage() != null ? document.getCoverImage() : "https://example.com/default-cover.jpg",
                            buttons
                    )
            );

            System.out.println("================================");
            System.out.println(cards);
            System.out.println("================================");
            // Events cho các hành động phổ biến
            List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
                new WebhookResponse.QuickReply("📚 Mượn sách", "{\"eventName\": \"BorrowBook\", \"parameters\": {\"bookTitle\": \"" + document.getDocumentName() + "\"}}"),
                new WebhookResponse.QuickReply("📖 Đặt giữ sách", "{\"eventName\": \"ReserveBook\", \"parameters\": {\"bookTitle\": \"" + document.getDocumentName() + "\"}}"),
                new WebhookResponse.QuickReply("💻 Tải tài liệu số", "{\"eventName\": \"DownloadDigital\", \"parameters\": {\"bookTitle\": \"" + document.getDocumentName() + "\"}}"),
                new WebhookResponse.QuickReply("⭐ Đánh giá sách", "{\"eventName\": \"RateBook\", \"parameters\": {\"bookTitle\": \"" + document.getDocumentName() + "\"}}")
            );

            // Custom data để lưu thông tin sách
            Map<String, Object> customData = new HashMap<>();
            customData.put("documentName", document.getDocumentName());
            customData.put("author", document.getAuthor());
            customData.put("category", document.getDocumentCategory());

            // Suggestions cho các tùy chọn khác
            List<String> suggestions = Arrays.asList(
                "Tìm sách tương tự",
                "Xem sách cùng tác giả",
                "Xem sách cùng thể loại"
            );

            StringBuilder responseText = new StringBuilder();
            responseText.append("📚 Thông tin chi tiết sách:\n\n");
            
            // Thông tin cơ bản
            responseText.append("📖 Thông tin cơ bản:\n");
            responseText.append(String.format("- Tên sách: %s\n", document.getDocumentName()));
            responseText.append(String.format("- Tác giả: %s\n", document.getAuthor() != null ? document.getAuthor() : "Chưa cập nhật"));
            responseText.append(String.format("- Nhà xuất bản: %s\n", document.getPublisher() != null ? document.getPublisher() : "Chưa cập nhật"));
            responseText.append(String.format("- Ngày xuất bản: %s\n", document.getPublishedDate() != null ? document.getPublishedDate() : "Chưa cập nhật"));
            responseText.append(String.format("- Ngôn ngữ: %s\n", document.getLanguage() != null ? document.getLanguage() : "Chưa cập nhật"));
            
            // Thông tin mô tả
            if (document.getDescription() != null && !document.getDescription().isEmpty()) {
                responseText.append("\n📝 Mô tả:\n");
                responseText.append(document.getDescription()).append("\n");
            }
            
            // Thông tin vật lý
            if (document.getPhysicalDocument() != null) {
                responseText.append("\n📚 Thông tin vật lý:\n");
                responseText.append(String.format("- Số lượng: %d\n", document.getPhysicalDocument().getQuantity()));
                responseText.append(String.format("- Số lượng còn lại: %d\n", document.getPhysicalDocument().getAvailableCopies()));
                if (document.getPhysicalDocument().getIsbn() != null) {
                    responseText.append(String.format("- ISBN: %s\n", document.getPhysicalDocument().getIsbn()));
                }
            }
            
            // Thông tin số
            if (document.getDigitalDocument() != null) {
                responseText.append("\n💻 Thông tin số:\n");
                responseText.append(String.format("- Trạng thái truy cập: %s\n", 
                    getVisibilityStatusDisplay(document.getDigitalDocument().getVisibilityStatus())));
            }
            
            // Thông tin phê duyệt
            responseText.append("\n✅ Thông tin phê duyệt:\n");
            responseText.append(String.format("- Trạng thái: %s\n", document.getApprovalStatus()));
            
            // Danh mục
            responseText.append("\n🏷️ Danh mục:\n");
            responseText.append(String.format("- Loại tài liệu: %s\n", getCategoryDisplay(document.getDocumentCategory())));
            if (document.getDocumentTypes() != null && !document.getDocumentTypes().isEmpty()) {
                String types = document.getDocumentTypes().stream()
                    .map(DocumentTypeEntity::getTypeName)
                    .collect(Collectors.joining(", "));
                responseText.append("- Thể loại: ").append(types).append("\n");
            }
            if (document.getCourses() != null && !document.getCourses().isEmpty()) {
                String courses = document.getCourses().stream()
                    .map(CourseResponse::getCourseName)
                    .collect(Collectors.joining(", "));
                responseText.append("- Khóa học: ").append(courses).append("\n");
            }

            return createSuccessResponse(
                responseText.toString(),
                quickReplies,
                cards,
                customData,
                suggestions
            );
        } catch (Exception e) {
            // Events khi có lỗi
            List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
                new WebhookResponse.QuickReply("🔄 Thử lại", "{\"eventName\": \"BookDetails\", \"parameters\": {\"book-title\": \"" + bookTitle + "\"}}"),
                new WebhookResponse.QuickReply("📞 Liên hệ hỗ trợ", "{\"eventName\": \"DefaultFallbackIntent\", \"parameters\": {}}")
            );

            // Suggestions cho các tùy chọn khác
            List<String> suggestions = Arrays.asList(
                "Quay lại trang chủ",
                "Xem hướng dẫn sử dụng",
                "Liên hệ thủ thư"
            );

            return createSuccessResponse(
                "Đã có lỗi xảy ra khi lấy thông tin sách. Vui lòng thử lại sau.",
                quickReplies,
                null,
                null,
                suggestions
            );
        }
    }

    private WebhookResponse handleButtonAction(String payload, WebhookRequest request) {
        String bookTitle = getStringParameter(request, "book-title");
        if (bookTitle == null) {
            return createErrorResponse("Không tìm thấy thông tin sách");
        }

        try {
            DocumentResponseDto document = documentService.searchByTitle(bookTitle);
            if (document == null) {
                return createErrorResponse("Không tìm thấy sách");
            }

            switch (payload) {
                case "borrow_book":
                    // Quick replies cho mượn sách
                    List<WebhookResponse.QuickReply> borrowQuickReplies = Arrays.asList(
                        new WebhookResponse.QuickReply("✅ Xác nhận mượn", "confirm_borrow"),
                        new WebhookResponse.QuickReply("❌ Hủy", "cancel_borrow")
                    );

                    // Suggestions cho mượn sách
                    List<String> borrowSuggestions = Arrays.asList(
                        "Xem thời gian mượn",
                        "Xem quy định mượn sách",
                        "Xem sách tương tự"
                    );

                    // Custom data cho mượn sách
                    Map<String, Object> borrowCustomData = new HashMap<>();
                    borrowCustomData.put("action", "borrow");
                    borrowCustomData.put("documentName", document.getDocumentName());
                    borrowCustomData.put("documentId", document.getDocumentId());

                    return createSuccessResponse(
                        String.format("Bạn muốn mượn sách '%s'?\n\n" +
                            "📚 Thông tin mượn sách:\n" +
                            "- Thời gian mượn: 14 ngày\n" +
                            "- Số lượng còn lại: %d",
                            document.getDocumentName(),
                            document.getPhysicalDocument().getAvailableCopies()),
                        borrowQuickReplies,
                        null,
                        borrowCustomData,
                        borrowSuggestions
                    );

                case "reserve_book":
                    // Quick replies cho đặt giữ sách
                    List<WebhookResponse.QuickReply> reserveQuickReplies = Arrays.asList(
                        new WebhookResponse.QuickReply("✅ Xác nhận đặt giữ", "confirm_reserve"),
                        new WebhookResponse.QuickReply("❌ Hủy", "cancel_reserve")
                    );

                    // Suggestions cho đặt giữ sách
                    List<String> reserveSuggestions = Arrays.asList(
                        "Xem thời gian giữ sách",
                        "Xem quy định đặt giữ",
                        "Xem sách tương tự"
                    );

                    // Custom data cho đặt giữ sách
                    Map<String, Object> reserveCustomData = new HashMap<>();
                    reserveCustomData.put("action", "reserve");
                    reserveCustomData.put("documentName", document.getDocumentName());
                    reserveCustomData.put("documentId", document.getDocumentId());

                    return createSuccessResponse(
                        String.format("Bạn muốn đặt giữ sách '%s'?\n\n" +
                            "📚 Thông tin đặt giữ:\n" +
                            "- Thời gian giữ: 3 ngày\n" +
                            "- Số lượng còn lại: %d",
                            document.getDocumentName(),
                            document.getPhysicalDocument().getAvailableCopies()),
                        reserveQuickReplies,
                        null,
                        reserveCustomData,
                        reserveSuggestions
                    );

                default:
                    return createErrorResponse("Hành động không hợp lệ");
            }
        } catch (Exception e) {
            return createErrorResponse("Đã có lỗi xảy ra: " + e.getMessage());
        }
    }
} 