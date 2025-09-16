package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.model.UserHasUser;
import com.example.demo.model.UserUserId;
import com.example.demo.repository.UserHasUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvitationService {
    private final UserHasUserRepository repository;

    public InvitationService(UserHasUserRepository repository){
        this.repository = repository;
    }

    public List<UserHasUser> getAllInvitationsByUser(User user){
        return repository.findAllByUser(user);
    }
    
    public UserHasUser sendInvitation(UserHasUser invitation){
        return repository.save(invitation);
    }

    public void deleteInvitation(UserUserId invitationId){
        repository.findById(invitationId).ifPresent(repository::delete);
    }
}
