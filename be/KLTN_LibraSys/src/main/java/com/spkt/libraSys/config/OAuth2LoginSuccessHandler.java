package com.spkt.libraSys.config;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthResponse;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.access.UserInfo;
import com.spkt.libraSys.service.role.RoleEntity;
import com.spkt.libraSys.service.role.RoleRepository;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import com.spkt.libraSys.service.user.UserStatus;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JWTUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;



    @Value("${my-config.client-url}")
    private String clientUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        // Get user info from Google
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        // Process login through AuthService
        var authResponse = this.handleGoogleLogin(email, name);

        // Check if request is from mobile app
        String userAgent = request.getHeader("User-Agent");
        boolean isMobileApp = userAgent != null &&
                (userAgent.contains("Android") || userAgent.contains("iOS"));

        if (isMobileApp) {
            // Return deep link for mobile app
            String redirectUrl = String.format("libraryapp://oauth2/callback/google?token=%s&userInfo=%s",
                    authResponse.getToken(),
                    URLEncoder.encode(authResponse.getUserInfo().toString(), StandardCharsets.UTF_8));
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } else {
            // Redirect to web frontend
            String redirectUrl = clientUrl + "/auth/callback?token=" + authResponse.getToken() +
                    "&userInfo=" + URLEncoder.encode(authResponse.getUserInfo().toString(), StandardCharsets.UTF_8);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
    }

    AuthResponse handleGoogleLogin(String email, String name){
        UserEntity userEntity = userRepository.findByUsername(email)
                .orElseGet(() -> {
                    // Create new user if doesn't exist
                    UserEntity newUser = new UserEntity();
                    newUser.setUsername(email);
                    newUser.setFirstName(name);
                    newUser.setLastName("");
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    newUser.setIsActive(UserStatus.ACTIVE);

                    // Set default role (you might want to adjust this)
                    RoleEntity userRole = roleRepository.findByRoleName("USER")
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                    newUser.setRoleEntities(Collections.singleton(userRole));

                    return userRepository.save(newUser);
                });

        // Generate JWT token
        String token = jwtUtil.generateToken(userEntity);

        // Create UserInfo
        UserInfo userInfo = UserInfo.builder()
                .userId(userEntity.getUserId())
                .username(userEntity.getUsername())
                .firstName(userEntity.getFirstName())
                .lastName(userEntity.getLastName())
                .fullName(userEntity.getFirstName() + " " + userEntity.getLastName())
                .roles(userEntity.getRoleEntities().stream()
                        .map(role -> role.getRoleName())
                        .collect(Collectors.toList()))
                .build();

        return AuthResponse.builder()
                .token(token)
                .userInfo(userInfo)
                .build();
    }
} 