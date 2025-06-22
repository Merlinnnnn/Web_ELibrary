package com.spkt.libraSys.service.email;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
//@Entity(name ="emails_001")
public class EmailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    String toEmail;
    String subject;
    String body;
    String status = "SUCCESS";
    String attachment;
    LocalDateTime createdAt;
    Long transactionId;

}