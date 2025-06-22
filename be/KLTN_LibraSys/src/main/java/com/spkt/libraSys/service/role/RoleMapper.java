package com.spkt.libraSys.service.role;


import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

//    Role toRole(RoleCreateRequest request);
    RoleResponse toRoleResponse(RoleEntity role);
}