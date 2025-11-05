package com.example.demo.controller;

import com.example.demo.controller.dto.InvitationResponseDto;
import com.example.demo.controller.responses.ApiResponse;
import com.example.demo.mapper.InvitationMapper;
import com.example.demo.model.User;
import com.example.demo.service.InvitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
public class InvitationsController {
    private final InvitationService invitationService;
    private final InvitationMapper invitationMapper;

    public InvitationsController(InvitationService invitationService, InvitationMapper invitationMapper) {
        this.invitationService = invitationService;
        this.invitationMapper = invitationMapper;
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<InvitationResponseDto>>> getInvitations(Authentication authentication){
        User currentUser = (User) authentication.getPrincipal();
        List<InvitationResponseDto> invitations = this.invitationService.getAllInvitationsByUser(currentUser).stream().map(this.invitationMapper::toResponse).toList();
        ApiResponse<List<InvitationResponseDto>> response = new ApiResponse<>("SUCCESS", "Invitations", invitations, null);
        return ResponseEntity.ok(response);
    }
}
