package com.example.demo.repository;

import com.example.demo.model.BlackListedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface BlackListedTokenRepository extends JpaRepository<BlackListedToken, String> {
    boolean existsByTokenAndExpiryAfter(String token, Instant now);

    @Transactional
    @Modifying
    @Query("DELETE FROM BlackListedToken b WHERE b.expiry < ?1")
    void deleterExpiredTokens(Instant now);

    @Transactional
    @Modifying
    void deleteByToken(String token);
}
