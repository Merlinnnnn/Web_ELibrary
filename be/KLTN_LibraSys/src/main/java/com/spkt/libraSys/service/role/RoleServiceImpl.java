package com.spkt.libraSys.service.role;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleServiceImpl implements RoleService {

    RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public boolean hasRole(String userId, String roleName) {
        //return roleRepository.existsByNameAndUsersUserId(roleName,userId);
        return true;
    }

    @Override
    public Boolean isAdmin(UserEntity user) {
        return user.getRoleEntities().stream()
                .anyMatch(role -> role.getRoleName().equals("ADMIN"));
    }

    @Override
    public Boolean isAdminOrManager(UserEntity user) {
        return user.getRoleEntities().stream()
                .anyMatch(role -> (role.getRoleName().equals("ADMIN")||role.getRoleName().equals("MANAGER")));
    }
    @Override
    public void assignRoleToUser(String userId, List<String> roles) {
        UserEntity user = userRepository.findById(userId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<RoleEntity> roleEntities = roleRepository.findByRoleNameIn(roles);

        if (roleEntities.size() != roles.size()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND,"Role doesn't exist");
        }
        user.setRoleEntities(new HashSet<>(roleEntities));
        userRepository.save(user);

    }

}