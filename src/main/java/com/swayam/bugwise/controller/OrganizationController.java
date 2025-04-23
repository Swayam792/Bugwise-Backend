package com.swayam.bugwise.controller;

import com.swayam.bugwise.dto.*;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.repository.jpa.UserRepository;
import com.swayam.bugwise.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService organizationService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createOrganization(@Valid @RequestBody OrganizationRequestDTO request, Authentication authentication) {
        User admin = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new NoSuchElementException("Admin not found"));
        organizationService.createOrganization(request, admin);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{organizationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizationDTO> getOrganization(
            @PathVariable String organizationId,
            Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new NoSuchElementException("User not found"));
        return ResponseEntity.ok(organizationService.getOrganization(organizationId, user));
    }

    @GetMapping("/my-organizations")
    public ResponseEntity<List<OrganizationDTO>> getMyOrganizations(Authentication authentication) {
        List<OrganizationDTO> organizations = organizationService.getOrganizationsForUser(authentication.getName());
        return ResponseEntity.ok(organizations);
    }

    @PostMapping("/{organizationId}/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addUsersToOrganization(
            @PathVariable String organizationId,
            @Valid @RequestBody Set<String> userIds) {
        organizationService.addUsersToOrganization(organizationId, userIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{organizationId}/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeUserFromOrganization(
            @PathVariable String organizationId,
            @PathVariable String userId) {
        organizationService.removeUserFromOrganization(organizationId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{organizationId}/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROJECT_MANAGER')")
    public ResponseEntity<Set<UserDetailsDTO>> getUsersInOrganization(
            @PathVariable String organizationId) {
        return ResponseEntity.ok(organizationService.getUsersInOrganization(organizationId));
    }

    @PutMapping("/{organizationId}")
    @PreAuthorize("hasRole('ADMIN') ")
    public ResponseEntity<OrganizationDTO> updateOrganization(
            @PathVariable String organizationId,
            @Valid @RequestBody OrganizationRequestDTO request) {
        organizationService.updateOrganization(organizationId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{organizationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrganization(@PathVariable String organizationId) {
        organizationService.deleteOrganization(organizationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{organizationId}/projects")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProjectDTO>> getOrganizationProjects(
            @PathVariable String organizationId) {
        return ResponseEntity.ok(organizationService.getOrganizationProjects(organizationId));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrganizationStatsDTO>> getOrganizationStats() {
        return ResponseEntity.ok(organizationService.getOrganizationStats());
    }
}