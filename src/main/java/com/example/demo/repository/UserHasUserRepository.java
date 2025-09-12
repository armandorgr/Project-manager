package com.example.demo.repository;

import com.example.demo.model.UserHasUser;
import com.example.demo.model.UserUserId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHasUserRepository extends JpaRepository<UserHasUser, UserUserId> {
}
