package com.example.demo.data;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepo;

    @Test
    void shouldFindByUserName(){
        User testUser = new User();
        testUser.setUsername("testuser");
        userRepo.save(testUser);
        Optional<User> found = userRepo.findByUsername("testuser");
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }

    @AfterEach
    void cleanDb(){
        this.userRepo.deleteAll();
    }
}
