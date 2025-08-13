package com.example.demo.repository;

import com.example.demo.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByToken(String token);

    @Query("{ 'userId': ?0 }")
    List<RefreshToken> findByUserId(String userId);

    void deleteByUserId(String userId);

    @Query(value = "{ 'expiryDate': { $lt: ?0 } }", delete = true)
    void deleteExpiredTokens(Instant now);
}
