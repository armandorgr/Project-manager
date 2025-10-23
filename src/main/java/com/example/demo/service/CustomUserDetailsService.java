package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserRepository userRepo;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepo){
        this.userRepo = userRepo;
    }

    public UserDetails registerUser(String username, String password, String email) throws DuplicateKeyException {
        try{
            return this.userRepo.save(new User(username, password, email));
        }catch (DataIntegrityViolationException e){
            this.logger.debug(String.format("Error de tipo %s", e.getClass().getName()));
            throw new DuplicateKeyException(String.format("User with email(%S) already exists", email));
        }
    }

    public UserDetails loadUserById(UUID id){
        return userRepo.findById(id).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
