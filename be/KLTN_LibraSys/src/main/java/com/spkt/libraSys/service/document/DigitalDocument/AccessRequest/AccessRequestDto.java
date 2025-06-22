package com.spkt.libraSys.service.document.DigitalDocument.AccessRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccessRequestDto {
    @NotNull(message = "Digital ID is required")
    private Long digitalId;
}
