package com.spkt.libraSys.service.access;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class VerificationResponse {
    private boolean verified;
    private String email; 
    private String verifiedAt; 
}
