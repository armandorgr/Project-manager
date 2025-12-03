package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    @Query("SELECT u FROM User u WHERE u.username = :term OR u.email = :term")
    Optional<User> findByUsernameOrEmail(@Param("term") String term);
    @Modifying
    @Transactional
    @Query("DELETE FROM User")
    void deleteAll();
}
