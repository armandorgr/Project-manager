package com.example.demo.repository;

import com.example.demo.model.Project;
import com.example.demo.model.User;
import com.example.demo.model.UserHasUser;
import com.example.demo.model.UserUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserHasUserRepository extends JpaRepository<UserHasUser, UserUserId> {
    List<UserHasUser> findAllByUser(User user);
    Optional<UserHasUser> findByUserAndProject(User user, Project project);
    @Modifying
    @Transactional
    @Query("DELETE FROM UserHasUser")
    void deleteAll();
}
