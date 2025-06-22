package com.spkt.libraSys.service.chatbot.aspect;

import com.spkt.libraSys.service.chatbot.IntentHandler;
import com.spkt.libraSys.service.chatbot.WebhookRequest;
import com.spkt.libraSys.service.chatbot.WebhookResponse;
import com.spkt.libraSys.service.chatbot.annotation.RequiresAuth;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Aspect
@Component
@Slf4j
public class AuthCheckAspect {
    @Autowired
    private UserRepository userRepository;

    @Around("@within(com.spkt.libraSys.service.chatbot.annotation.RequiresAuth) && args(request)")
    public Object checkAuth(ProceedingJoinPoint joinPoint, WebhookRequest request) throws Throwable {
        IntentHandler handler = (IntentHandler) joinPoint.getTarget();
        RequiresAuth requiresAuth = handler.getClass().getAnnotation(RequiresAuth.class);
        
        // Lấy username từ request
        Map<String, Object> payload = request.getOriginalDetectIntentRequest().getPayload();

        String username = null;
        if (payload != null && payload.get("username") != null) {
            username = payload.get("username").toString();
        }

        log.info("AuthCheckAspect:Username: {}", username);
        Optional<UserEntity> user = userRepository.findByUsername(username);
        if(user.isEmpty()) {
            return createAuthRequiredResponse(requiresAuth.message());
        }

        // Nếu đã xác thực, tiếp tục xử lý intent
        return joinPoint.proceed();
    }

    private WebhookResponse createAuthRequiredResponse(String message) {
        WebhookResponse response = new WebhookResponse(message);
        response.getPayload().setQuickReplies(Arrays.asList(
            new WebhookResponse.QuickReply("Đăng nhập", "login"),
            new WebhookResponse.QuickReply("Đăng ký", "register")
        ));
        response.getPayload().setSuggestions(Arrays.asList(
            "Quên mật khẩu",
            "Liên hệ hỗ trợ",
            "Hướng dẫn sử dụng"
        ));
        return response;
    }
} 