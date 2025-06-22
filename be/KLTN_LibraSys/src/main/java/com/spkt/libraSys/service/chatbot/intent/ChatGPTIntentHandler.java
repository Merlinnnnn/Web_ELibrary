package com.spkt.libraSys.service.chatbot.intent;

import com.spkt.libraSys.service.chatbot.IntentHandler;
import com.spkt.libraSys.service.chatbot.WebhookRequest;
import com.spkt.libraSys.service.chatbot.WebhookResponse;
import com.spkt.libraSys.service.openAI.OpenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class ChatGPTIntentHandler implements IntentHandler {

    @Autowired
    private OpenAIService openAIService;

    @Override
    public String getIntentName() {
        return "ChatGPT_Generic";
    }

    @Override
    public String getToDesc() {
        return "Trợ lý AI thông minh cho hệ thống thư viện số";
    }

    @Override
    public WebhookResponse handle(WebhookRequest request) {
        String userMessage = extractUserMessage(request);
        
        // Tạo prompt dựa trên ngữ cảnh và yêu cầu của người dùng
        String prompt = """
                Bạn là một trợ lý AI thông minh cho hệ thống thư viện số. Bạn có thể hỗ trợ người dùng với các chức năng sau:

                1. Tìm kiếm và xem thông tin sách:
                   - Tìm sách theo tiêu đề (SearchBookByTitle)
                   - Tìm sách theo tác giả (SearchBookByAuthor)
                   - Xem chi tiết sách (BookDetails)
                   - Xem sách phổ biến (SearchTopBook)
                   - Tóm tắt sách (SummarizeBook, SummarizeBookByTitle)

                2. Quản lý mượn trả sách:
                   - Mượn sách (BorrowBook)
                   - Trả sách (ReturnBook)
                   - Gia hạn sách (RenewBook)
                   - Đặt giữ sách (ReserveBook)
                   - Kiểm tra sách đang mượn (CheckBorrowedBooks)

                3. Thông tin thư viện:
                   - Giờ mở cửa (LibraryHours)
                   - Quy định thư viện (LibraryRules)
                   - Hướng dẫn thanh toán (PaymentGuide)
                   - Kiểm tra tiền phạt (CheckFine)

                4. Hỗ trợ chung:
                   - Tư vấn sách phù hợp với sở thích và nhu cầu
                   - Giải thích cách sử dụng các tính năng của thư viện
                   - Hướng dẫn quy trình mượn/trả sách
                   - Giới thiệu sách mới hoặc sách nổi bật
                   - Trả lời các câu hỏi về chính sách thư viện
                   - Hỗ trợ tìm kiếm tài liệu học tập
                   - Đề xuất sách theo chủ đề hoặc môn học
                   - Giải thích cách truy cập tài liệu số
                   - Thông tin về các sự kiện và hoạt động của thư viện

                Yêu cầu của người dùng:
                %s

                Hãy trả lời một cách thân thiện, chuyên nghiệp và hữu ích. Nếu câu hỏi liên quan đến một chức năng cụ thể, hãy hướng dẫn người dùng sử dụng chức năng đó.
                """.formatted(userMessage);

        try {
            String response = openAIService.inferIntentFromUserQuery(prompt);
            
            // Các nút tác vụ nhanh
            List<WebhookResponse.QuickReply> quickReplies = Arrays.asList(
                new WebhookResponse.QuickReply("🔍 Tìm sách", "{\"eventName\": \"search_book\", \"parameters\": {}}"),
                new WebhookResponse.QuickReply("📚 Mượn sách", "{\"eventName\": \"borrow_book\", \"parameters\": {}}"),
                new WebhookResponse.QuickReply("📖 Đặt giữ sách", "{\"eventName\": \"reserve_book\", \"parameters\": {}}"),
                new WebhookResponse.QuickReply("💻 Tài liệu số", "{\"eventName\": \"digital_docs\", \"parameters\": {}}"),
                new WebhookResponse.QuickReply("⏰ Giờ mở cửa", "{\"eventName\": \"LibraryHours\", \"parameters\": {}}")
            );

            // Các gợi ý tìm kiếm
            List<String> suggestions = Arrays.asList(
                "📋 Hướng dẫn sử dụng thư viện",
                "📞 Liên hệ thủ thư",
                "📚 Sách mới cập nhật",
                "🎯 Sách theo chủ đề",
                "📖 Sách theo môn học"
            );

            return createSuccessResponse(
                response != null ? response : "Xin lỗi, tôi gặp sự cố khi xử lý yêu cầu. Vui lòng thử lại sau.",
                quickReplies,
                null,
                null,
                suggestions
            );
        } catch (Exception e) {
            return createErrorResponse("Xin lỗi, tôi gặp sự cố khi xử lý yêu cầu. Vui lòng thử lại sau hoặc liên hệ thủ thư để được hỗ trợ.");
        }
    }

    private String extractUserMessage(WebhookRequest request) {
        // Lấy thông tin từ parameters
        Map<String, Object> parameters = request.getQueryResult().getParameters();
        return parameters != null ? parameters.toString() : "Không có yêu cầu cụ thể";
    }
}
