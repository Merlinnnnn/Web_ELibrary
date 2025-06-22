package com.spkt.libraSys.service.user;

import com.spkt.libraSys.service.role.RoleEntity;
import com.spkt.libraSys.service.role.RoleMapper;
import org.mapstruct.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "userId", ignore = true) // ID tự động tạo
    @Mapping(target = "password", ignore = true) // Password mã hóa trong service
    @Mapping(target = "roleEntities", ignore = true) // Roles được gán trong service
    @Mapping(target = "registrationDate", expression = "java(UserMapper.getCurrentDate())") // ✅ Sửa lỗi LocalDate
    @Mapping(target = "isActive", expression = "java(UserStatus.ACTIVE)") // ✅ Sửa lỗi Enum
    UserEntity toUser(UserRequest request);

    @Mapping(target = "roles", source = "roleEntities")
    UserResponse toUserResponse(UserEntity user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "roleEntities", ignore = true)
    @Mapping(target = "isActive",ignore = true)
    void updateUser(@MappingTarget UserEntity user, UserUpdateRequest request);


    default List<String> mapRoles(Set<RoleEntity> roles) {
        return roles.stream()
                .map(RoleEntity::getRoleName)
                .collect(Collectors.toList());
    }


    static LocalDate getCurrentDate() {
        return LocalDate.now();
    }
}