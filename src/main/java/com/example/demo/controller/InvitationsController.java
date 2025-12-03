package com.example.demo.controller;

import com.example.demo.controller.dto.InvitationResponseDto;
import com.example.demo.controller.responses.Response;
import com.example.demo.mapper.InvitationMapper;
import com.example.demo.model.User;
import com.example.demo.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Invitations", description = "Endpoints to manage project invitations")
@RestController
@RequestMapping("/api/invitations")
public class InvitationsController {
    private final InvitationService invitationService;
    private final InvitationMapper invitationMapper;

    public InvitationsController(InvitationService invitationService, InvitationMapper invitationMapper) {
        this.invitationService = invitationService;
        this.invitationMapper = invitationMapper;
    }

    @Operation(
            summary = "Get all invitations for the current user",
            description = "Retrieves all invitations received by the authenticated user.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of invitations retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Response.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized - user not authenticated"
                    )
            }
    )
    @GetMapping()
    public ResponseEntity<Response<List<InvitationResponseDto>>> getInvitations(Authentication authentication){
        User currentUser = (User) authentication.getPrincipal();
        List<InvitationResponseDto> invitations = this.invitationService.getAllInvitationsByUser(currentUser).stream().map(this.invitationMapper::toResponse).toList();
        Response<List<InvitationResponseDto>> response = new Response<>("SUCCESS", "Invitations", invitations, null);
        return ResponseEntity.ok(response);
    }
}
