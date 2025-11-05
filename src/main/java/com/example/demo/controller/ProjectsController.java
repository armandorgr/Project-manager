package com.example.demo.controller;

import com.example.demo.controller.dto.*;
import com.example.demo.controller.responses.ApiResponse;
import com.example.demo.mapper.ProjectMapper;
import com.example.demo.model.*;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.InvitationService;
import com.example.demo.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/project")
public class ProjectsController {
    private final ProjectMapper projectMapper;
    private final ProjectService projectService;
    private final CustomUserDetailsService usersService;
    private final InvitationService invitationService;

    public ProjectsController(
            ProjectMapper projectMapper,
            ProjectService projectService,
            CustomUserDetailsService usersService, InvitationService invitationService
    ) {
        this.projectMapper = projectMapper;
        this.projectService = projectService;
        this.usersService = usersService;
        this.invitationService = invitationService;
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponseDto>> updateProject(@RequestBody @Valid UpdateProjectDto dto, @PathVariable("id") UUID projectId) {
        // Find project to update
        Project project = this.projectService.getOneById(projectId);
        this.projectMapper.updateProjectFromDto(dto, project);
        Project updatedProject = this.projectService.saveProject(project);
        ApiResponse<ProjectResponseDto> response = new ApiResponse<>("SUCCESS", "Project updated", this.projectMapper.toResponse(updatedProject), null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponseDto>> deleteProject(Authentication authentication, @PathVariable("id") UUID projectId) {
        User currentUser = (User) authentication.getPrincipal();
        UserHasProjects relation = this.projectService.getRelation(new UserProjectId(currentUser.getId(), projectId));
        if (relation.getRole() != ProjectRole.ADMIN) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("ERROR", "ROLE NOT HIGH ENOUGH", null, null));
        }
        Project project = relation.getProject();
        this.projectService.deleteProject(project);
        ApiResponse<ProjectResponseDto> response = new ApiResponse<>("SUCCESS", "Project deleted", this.projectMapper.toResponse(project), null);
        return ResponseEntity.ok(response);
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<Project>>> getAll(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<Project> projects = this.projectService.getAllProjectsByUser(currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<List<Project>>("SUCCESS", "All projects", projects, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponseDto>> getOneById(@PathVariable("id") UUID projectId) {
        Project project = this.projectService.getOneById(projectId);
        ApiResponse<ProjectResponseDto> response = new ApiResponse<>("SUCCESS", "Project data found", this.projectMapper.toResponse(project), null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/query")
    public ResponseEntity<ApiResponse<List<Project>>> getOneByQuery(@RequestBody() @Valid ProjectQueryDto dto, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<Project> project = this.projectService.getOneByQuery(dto.query(), currentUser.getId());
        ApiResponse<List<Project>> response = new ApiResponse<>("RESULT", "QUERY RESULTS", project, null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<ApiResponse<String>> invite(
            @PathVariable("id") UUID projectId,
            @RequestBody @Valid InvitationDto dto,
            Authentication authentication
    ){
        Project project = this.projectService.getOneById(projectId);
        User currentUser = (User) authentication.getPrincipal();
        User receiver = (User) this.usersService.loadUserByEmailOrUsername(dto.term());
        UserHasUser invitation = new UserHasUser(currentUser, receiver, project, String.format("%s has invited you to join %s", currentUser.getUsername(), project.getName()));
        this.invitationService.sendInvitation(invitation);
        ApiResponse<String> response = new ApiResponse<>("SUCCESS", "Invitation sent successfully", null, null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/join")
    public  ResponseEntity<ApiResponse<String>> join(@PathVariable("id") UUID projectId, @RequestParam InvitationResponseType invitationResponse, Authentication authentication){
        Project project = this.projectService.getOneById(projectId);
        User currentUser = (User) authentication.getPrincipal();
        UserHasUser invitation = this.invitationService.findByReceiverAndProject(currentUser, project);
        if(invitationResponse == InvitationResponseType.ACCEPT){
            this.projectService.addUserToProject(new UserHasProjects(currentUser, project, ProjectRole.USER));
        }
        this.invitationService.deleteInvitation(invitation);
        ApiResponse<String> response = new ApiResponse<>("SUCCESS", "Invitation responded", null, null);
        return ResponseEntity.ok(response);
    }

    @Transactional
    @PostMapping()
    public ResponseEntity<ApiResponse<Project>> create(@RequestBody @Valid CreateProjectDto dto, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Project project = projectMapper.toEntity(dto);
        project.setStartDate(Instant.now());
        Project saved = this.projectService.saveProject(project);
        this.projectService.addUserToProject(new UserHasProjects(currentUser, saved, ProjectRole.ADMIN));
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>("SUCCESS", "Project created", saved, null));
    }
}
