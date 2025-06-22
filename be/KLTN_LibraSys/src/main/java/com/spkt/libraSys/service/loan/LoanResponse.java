package com.spkt.libraSys.service.loan;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoanResponse {
    Long transactionId;
    String documentId;
    Long physicalDocId;
    String documentName;
    String username;
    String librarianId;
    LocalDateTime loanDate;
    LocalDateTime dueDate;
    LocalDateTime returnDate;
    LoanStatus status;
    LoanEntity.Condition returnCondition;
    double fineAmount;
    LoanEntity.PaymentStatus paymentStatus;
    LocalDateTime paidAt;
}