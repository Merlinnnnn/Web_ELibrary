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

import java.util.Arrays;
import java.util.List;

@Service
public class SearchDocumentIntentHandler implements IntentHandler {

    @Autowired
    private DocumentService documentService;

    @Override
    public String getIntentName() {
        return "SearchDocument";
    }

    @Override
    public String getToDesc() {
        return "Tìm kiếm tài liệu";
    }

    @Override
    public WebhookResponse handle(WebhookRequest request) {
        String documentName = getStringParameter(request, "document-name");
        String author = getStringParameter(request, "author");
        
        if (documentName == null && author == null) {
            return createErrorResponse("Vui lòng cung cấp tên tài liệu hoặc tác giả để tìm kiếm");
        }

        // Tạo PageRequest với kích thước trang là 5
        PageRequest pageRequest = PageRequest.of(0, 5);
        Page<DocumentResponseDto> documents = documentService.getAllDocuments(pageRequest);

        if (documents.isEmpty()) {
            return createErrorResponse("Không tìm thấy tài liệu nào phù hợp với tiêu chí tìm kiếm");
        }

        StringBuilder responseText = new StringBuilder("Tìm thấy các tài liệu sau:\n\n");
        boolean foundAny = false;
        
        for (DocumentResponseDto doc : documents) {
            boolean matchesName = documentName == null || 
                doc.getDocumentName().toLowerCase().contains(documentName.toLowerCase());
            boolean matchesAuthor = author == null || 
                doc.getAuthor().toLowerCase().contains(author.toLowerCase());
                
            if (matchesName && matchesAuthor) {
                foundAny = true;
                responseText.append(String.format(
                    "Tên tài liệu: %s\n" +
                    "Tác giả: %s\n" +
                    "Nhà xuất bản: %s\n" +
                    "Ngôn ngữ: %s\n" +
                    "Số lượng còn lại: %d\n\n",
                    doc.getDocumentName(),
                    doc.getAuthor(),
                    doc.getPublisher(),
                    doc.getLanguage(),
                    doc.getQuantity()
                ));
            }
        }

        if (!foundAny) {
            return createErrorResponse("Không tìm thấy tài liệu nào phù hợp với tiêu chí tìm kiếm");
        }

        List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
            new WebhookResponse.QuickReply("Tìm sách", "{\"eventName\": \"search_book\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("Mượn sách", "{\"eventName\": \"borrow_book\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("Trả sách", "{\"eventName\": \"return_book\", \"parameters\": {}}"),
            new WebhookResponse.QuickReply("⏰ Giờ mở cửa", "{\"eventName\": \"LibraryHours\", \"parameters\": {}}")
        );

        List<String> suggestions = Arrays.asList(
            "Tìm sách theo tác giả",
            "Tìm sách theo thể loại",
            "Xem sách đang mượn"
        );

        return createSuccessResponse(
            responseText.toString(),
            quickReplies,
            null,
            null,
            suggestions
        );
    }
} 