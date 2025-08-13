package com.example.demo.service;

import com.example.demo.model.BlackListedToken;
import com.example.demo.repository.BlackListedTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class TokenBlacklistService {
    private final BlackListedTokenRepository repository;

    public TokenBlacklistService(BlackListedTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void blacklistToken(String token, long ttlMillis) {
        Instant expiry = Instant.now().plusMillis(ttlMillis);
        repository.save(new BlackListedToken(token, expiry));
    }

    public void unBlackListToken(String token){
        repository.deleteByToken(token);
    }

    public boolean isTokenBlackListed(String token) {
        cleanupExpiredTokens();
        return repository.existsByTokenAndExpiryAfter(token, Instant.now());
    }

    @Transactional
    public void cleanupExpiredTokens() {
        repository.deleterExpiredTokens(Instant.now());
    }
}
