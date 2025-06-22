package com.spkt.libraSys.config;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.role.RoleEntity;
import com.spkt.libraSys.service.user.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Tìm người dùng trong cơ sở dữ liệu
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        //check user active
        switch (userEntity.getIsActive()) {
            case PENDING:
                throw new AppException(ErrorCode.INSUFFICIENT_PERMISSIONS, "Người dùng chưa được xác minh");
            case LOCKED:
                throw new AppException(ErrorCode.INSUFFICIENT_PERMISSIONS, "Tài khoản bị khóa");
            case DELETED:
                throw new AppException(ErrorCode.INSUFFICIENT_PERMISSIONS, "Tài khoản đã bị xóa");
            default:
                break;
        }
        String[] rolesArray = userEntity.getRoleEntities().stream()
                .map(RoleEntity::getRoleName)
                .toArray(String[]::new);

        List<SimpleGrantedAuthority> authorities = userEntity.getRoleEntities().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleName()))
                .collect(Collectors.toList());

        return new CustomUserDetails(
                userEntity.getUserId(),
                userEntity.getUsername(),
                userEntity.getPassword(),
                authorities
        );
    }
}
