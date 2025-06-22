package com.spkt.libraSys.service.verificationToken;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity, Long> {
    VerificationTokenEntity findByToken(String token);

    void deleteAllByEmailAndType(String email, VerificationTokenEntity.TokenType type);

    List<VerificationTokenEntity> findAllByEmailAndType(String email, VerificationTokenEntity.TokenType type);
}
