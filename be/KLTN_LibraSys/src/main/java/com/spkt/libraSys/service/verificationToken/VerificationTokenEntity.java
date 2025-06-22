package com.spkt.libraSys.service.verificationToken;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "verification_tokens")
public class VerificationTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    Long tokenId;

    @Column(name = "email", nullable = false)
    String email;

    @Column(name = "token", nullable = false, unique = true)
    String token;

    @Column(name = "expiry_date", nullable = false)
    LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    TokenType type;

    public enum TokenType {
        VERIFY_EMAIL,
        RESET_PASSWORD
    }
}
