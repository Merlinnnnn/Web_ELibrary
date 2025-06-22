package com.spkt.libraSys.service.access;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class AuthResponse {
    String token;
    UserInfo userInfo;
}
