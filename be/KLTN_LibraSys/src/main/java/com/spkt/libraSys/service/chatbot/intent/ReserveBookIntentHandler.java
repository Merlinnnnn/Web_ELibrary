package com.spkt.libraSys.service.chatbot.intent;

import com.spkt.libraSys.service.chatbot.IntentHandler;
import com.spkt.libraSys.service.chatbot.WebhookRequest;
import com.spkt.libraSys.service.chatbot.WebhookResponse;
import com.spkt.libraSys.service.document.DocumentService;
import com.spkt.libraSys.service.document.DocumentResponseDto;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ReserveBookIntentHandler implements IntentHandler {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private PhysicalDocumentService physicalDocumentService;

    @Override
    public String getIntentName() {
        return "ReserveBook";
    }

    @Override
    public String getToDesc() {
        return "Đặt trước tài liệu";
    }

    @Override
    public WebhookResponse handle(WebhookRequest request) {
        String documentId = getStringParameter(request, "document-id");
        if (documentId == null) {
            return createErrorResponse("Vui lòng cung cấp ID của tài liệu");
        }

        try {
            DocumentResponseDto document = documentService.getDocumentById(Long.parseLong(documentId));
            if (document == null) {
                return createErrorResponse("Không tìm thấy tài liệu với ID: " + documentId);
            }

            // Kiểm tra xem tài liệu có phải là tài liệu vật lý không
            if (document.getPhysicalDocument() == null) {
                return createErrorResponse("Chỉ có thể đặt trước tài liệu vật lý");
            }

            // Kiểm tra số lượng tài liệu còn lại
            if (document.getQuantity() <= 0) {
                return createErrorResponse("Tài liệu này hiện không còn sẵn có để đặt trước");
            }

            // Thực hiện đặt trước tài liệu
           // documentService.reserveDocument(Long.parseLong(documentId));
            
            List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
                new WebhookResponse.QuickReply("Tìm sách khác", "{\"eventName\": \"search_book\", \"parameters\": {}}"),
                new WebhookResponse.QuickReply("Xem sách mới", "{\"eventName\": \"new_books\", \"parameters\": {}}"),
                new WebhookResponse.QuickReply("Liên hệ thủ thư", "{\"eventName\": \"contact_librarian\", \"parameters\": {}}"),
                new WebhookResponse.QuickReply("⏰ Giờ mở cửa", "{\"eventName\": \"LibraryHours\", \"parameters\": {}}")
            );

            List<String> suggestions = Arrays.asList(
                "Kiểm tra lại tên sách",
                "Tìm sách tương tự",
                "Đặt trước sách"
            );

            return createSuccessResponse(
                String.format("Đã đặt trước tài liệu %s thành công. Vui lòng đến thư viện trong vòng 24 giờ để nhận tài liệu.", 
                document.getDocumentName()),
                quickReplies,
                null,
                null,
                suggestions
            );
        } catch (NumberFormatException e) {
            return createErrorResponse("ID tài liệu không hợp lệ");
        } catch (Exception e) {
            return createErrorResponse("Không thể đặt trước tài liệu: " + e.getMessage());
        }
    }
} 