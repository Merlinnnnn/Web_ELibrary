package com.spkt.libraSys.service.token;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvalidatedTokenServiceImpl implements InvalidatedTokenService {
    InvalidatedTokenRepository blacklistedTokenRepository;
    @Override
    public boolean isJtiBlacklisted(String jti) {
        return blacklistedTokenRepository.findByJti(jti).isPresent();
    }

    @Override
    public void saveBlacklistedToken(String jti, long expirationTimeMillis) {
        blacklistedTokenRepository.save(new InvalidatedToken(jti, Instant.ofEpochMilli(expirationTimeMillis)));

    }
}
