package com.spkt.libraSys.service.role;

import lombok.Data;

import java.util.List;

@Data
public class AssignRoleRequest {
    private String userId;
    private List<String> roles;
}
