package com.spkt.libraSys.service.loan;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoanRequest {

    @NotNull(message = "physicalDocId ID cannot be null")
    Long physicalDocId;  // Tài liệu cần mượn
}