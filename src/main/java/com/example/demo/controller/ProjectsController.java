package com.example.demo.controller;

import com.example.demo.controller.anotations.projects.RequireProjectRole;
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

/**
 * REST controller responsible for managing projects and related actions such as
 * invitations, membership, and user access.
 *
 * Provides endpoints for CRUD operations on projects and for handling invitations
 * to join projects.
 */
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
            CustomUserDetailsService usersService,
            InvitationService invitationService
    ) {
        this.projectMapper = projectMapper;
        this.projectService = projectService;
        this.usersService = usersService;
        this.invitationService = invitationService;
    }

    /**
     * Updates an existing project with new data.
     *
     * @param dto       DTO containing updated project fields
     * @param projectId UUID of the project to update
     * @return ResponseEntity with updated project data and HTTP 200 OK
     */
    @RequireProjectRole(ProjectRole.ADMIN)
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponseDto>> updateProject(
            @RequestBody @Valid UpdateProjectDto dto,
            @PathVariable("id") UUID projectId
    ) {
        Project project = this.projectService.getOneById(projectId);
        this.projectMapper.updateProjectFromDto(dto, project);
        Project updatedProject = this.projectService.saveProject(project);

        ApiResponse<ProjectResponseDto> response =
                new ApiResponse<>("SUCCESS", "Project updated", this.projectMapper.toResponse(updatedProject), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a project if the authenticated user has ADMIN privileges on it.
     *
     * @param authentication current authenticated user
     * @param projectId      UUID of the project to delete
     * @return ResponseEntity with HTTP 204 NO_CONTENT if successful, or 403 if unauthorized
     */
    @RequireProjectRole(ProjectRole.ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponseDto>> deleteProject(
            Authentication authentication,
            @PathVariable("id") UUID projectId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        UserHasProjects relation = this.projectService.getRelation(new UserProjectId(currentUser.getId(), projectId));
        Project project = relation.getProject();
        this.projectService.deleteProject(project);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Retrieves all projects the authenticated user is a member of.
     *
     * @param authentication current authenticated user
     * @return ResponseEntity with list of projects and HTTP 200 OK
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Project>>> getAll(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<Project> projects = this.projectService.getAllProjectsByUser(currentUser.getId());
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "All projects", projects, null));
    }

    /**
     * Retrieves a single project by its ID.
     *
     * @param projectId UUID of the project
     * @return ResponseEntity with project details and HTTP 200 OK
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponseDto>> getOneById(@PathVariable("id") UUID projectId) {
        Project project = this.projectService.getOneById(projectId);
        ApiResponse<ProjectResponseDto> response =
                new ApiResponse<>("SUCCESS", "Project data found", this.projectMapper.toResponse(project), null);
        return ResponseEntity.ok(response);
    }

    /**
     * Performs a query-based search of projects belonging to the current user.
     *
     * @param dto            ProjectQueryDto containing the search query
     * @param authentication current authenticated user
     * @return ResponseEntity with matching projects and HTTP 200 OK
     */
    @GetMapping("/query")
    public ResponseEntity<ApiResponse<List<Project>>> getOneByQuery(
            @RequestBody @Valid ProjectQueryDto dto,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        List<Project> project = this.projectService.getOneByQuery(dto.query(), currentUser.getId());
        ApiResponse<List<Project>> response =
                new ApiResponse<>("SUCCESS", "Query results", project, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Sends an invitation to another user to join the given project.
     *
     * @param projectId     UUID of the project
     * @param dto           InvitationDto containing receiver info
     * @param authentication current authenticated user
     * @return ResponseEntity with HTTP 201 CREATED if invitation was sent successfully
     */
    @RequireProjectRole(ProjectRole.USER)
    @PostMapping("/{id}/invite")
    public ResponseEntity<ApiResponse<String>> invite(
            @PathVariable("id") UUID projectId,
            @RequestBody @Valid InvitationDto dto,
            Authentication authentication
    ) {
        Project project = this.projectService.getOneById(projectId);
        User currentUser = (User) authentication.getPrincipal();
        User receiver = (User) this.usersService.loadUserByEmailOrUsername(dto.term());

        UserHasUser invitation = new UserHasUser(
                currentUser,
                receiver,
                project,
                String.format("%s has invited you to join %s", currentUser.getUsername(), project.getName())
        );

        this.invitationService.sendInvitation(invitation);
        ApiResponse<String> response =
                new ApiResponse<>("SUCCESS", "Invitation sent successfully", null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Handles a user's response (accept or decline) to a project invitation.
     *
     * @param projectId          UUID of the project
     * @param invitationResponse enum value representing the user's decision
     * @param authentication     current authenticated user
     * @return ResponseEntity with HTTP 200 OK if processed successfully
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<String>> join(
            @PathVariable("id") UUID projectId,
            @RequestParam InvitationResponseType invitationResponse,
            Authentication authentication
    ) {
        Project project = this.projectService.getOneById(projectId);
        User currentUser = (User) authentication.getPrincipal();
        UserHasUser invitation = this.invitationService.findByReceiverAndProject(currentUser, project);

        if (invitationResponse == InvitationResponseType.ACCEPT) {
            this.projectService.addUserToProject(new UserHasProjects(currentUser, project, ProjectRole.USER));
        }

        this.invitationService.deleteInvitation(invitation);
        ApiResponse<String> response = new ApiResponse<>("SUCCESS", "Invitation response processed", null, null);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new project and assigns the authenticated user as ADMIN.
     *
     * @param dto            DTO containing new project data
     * @param authentication current authenticated user
     * @return ResponseEntity with created project and HTTP 201 CREATED
     */
    @Transactional
    @PostMapping
    public ResponseEntity<ApiResponse<Project>> create(
            @RequestBody @Valid CreateProjectDto dto,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        Project project = projectMapper.toEntity(dto);
        project.setStartDate(Instant.now());
        Project saved = this.projectService.saveProject(project);

        this.projectService.addUserToProject(new UserHasProjects(currentUser, saved, ProjectRole.ADMIN));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("SUCCESS", "Project created", saved, null));
    }
}
