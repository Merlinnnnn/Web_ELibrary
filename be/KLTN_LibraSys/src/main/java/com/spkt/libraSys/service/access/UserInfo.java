package com.spkt.libraSys.service.access;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    private String userId;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private List<String> roles;
}
