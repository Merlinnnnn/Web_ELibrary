package com.spkt.libraSys.config;

import com.spkt.libraSys.ratelimit.WebSocketRateLimiterManager;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JWTUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Autowired
    private WebSocketRateLimiterManager limiterManager;

    public WebSocketAuthInterceptor(JWTUtil jwtUtil,
                                    CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {
        // Kiểm tra nếu request không phải là ServletServerHttpRequest
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false; // Chặn kết nối nếu không phải HTTP Request hợp lệ
        }
        HttpServletRequest httpRequest = servletRequest.getServletRequest();

        String ip = getClientIp(httpRequest); // bạn tự xử lý lấy IP thật
        RateLimiter limiter = limiterManager.getLimiterByIp(ip);

        if (!limiterManager.tryAcquire(limiter, "IP " + ip)) {
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return false;
        }

        // Lấy JWT từ query params hoặc headers
        String token = httpRequest.getParameter("token");
        if (token == null) {
            token = httpRequest.getHeader("Authorization"); // Lấy JWT từ Header nếu không có trong query params
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7); // Loại bỏ tiền tố "Bearer "
            }
        }

        if (token == null || jwtUtil.isTokenExpired(token)) {
            return false; // Token không hợp lệ hoặc hết hạn
        }

        String username = jwtUtil.extractUserName(token);


        CustomUserDetails userDetails =(CustomUserDetails) userDetailsService.loadUserByUsername(username);

        if (!jwtUtil.validateToken(token, userDetails)) {
            return false;
        }

        // Tạo Authentication từ UserDetails
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails.getUserId(), null, userDetails.getAuthorities());

        // Đặt vào SecurityContextHolder để có thể sử dụng trong WebSocket
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Lưu user vào session WebSocket để dùng sau này
        System.out.println( userDetails.getUserId());
        attributes.put("userId", userDetails.getUserId());
        attributes.put("auth", authentication);


        return true; // Cho phép kết nối WebSocket
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // Không cần xử lý gì thêm sau khi kết nối WebSocket thành công
    }
}
