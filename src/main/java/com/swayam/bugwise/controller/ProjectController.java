package com.swayam.bugwise.controller;

import com.swayam.bugwise.dto.ProjectDTO;
import com.swayam.bugwise.dto.ProjectRequestDTO;
import com.swayam.bugwise.entity.Project;
import com.swayam.bugwise.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<Project> createProject(@Valid @RequestBody ProjectRequestDTO request) {
        return ResponseEntity.ok(projectService.createProject(request));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> getProject(@PathVariable String projectId) {
        return ResponseEntity.ok(projectService.getProject(projectId));
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<Project>> getOrganizationProjects(@PathVariable String organizationId) {
        return ResponseEntity.ok(projectService.getOrganizationProjects(organizationId));
    }

    @GetMapping("/organization/{organizationId}/search")
    public Page<Project> searchProjectsInOrganization(
            @PathVariable String organizationId,
            @RequestParam String searchTerm,
            Pageable pageable) {
        return projectService.searchProjectsInOrganization(organizationId, searchTerm, pageable);
    }

    @GetMapping("/organization/{organizationId}/count")
    public long countProjectsByOrganization(@PathVariable String organizationId) {
        return projectService.countProjectsByOrganization(organizationId);
    }

    @GetMapping("/manager/{projectManagerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public List<Project> findByProjectManager(@PathVariable String projectManagerId) {
        return projectService.findByProjectManager(projectManagerId);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProjectDTO>> getAllProjectsForAdmin(Authentication authentication) {
        return ResponseEntity.ok(projectService.getAllProjectsForAdmin(authentication.getName()));
    }
}
