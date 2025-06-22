package com.spkt.libraSys.service.token;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

@Entity
@Table(name = "InvalidatedToken")
@Builder
@AllArgsConstructor
public class InvalidatedToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String jti;

    @Column(nullable = false)
    private Instant expiryDate;

    public InvalidatedToken() {}

    public InvalidatedToken(String jti, Instant expiryDate) {
        this.jti = jti;
        this.expiryDate = expiryDate;
    }

    public Long getId() { return id; }
    public String getJti() { return jti; }
    public Instant getExpiryDate() { return expiryDate; }
}