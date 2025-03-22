package com.swayam.bugwise.controller;

import com.swayam.bugwise.dto.OrganizationDTO;
import com.swayam.bugwise.dto.OrganizationRequestDTO;
import com.swayam.bugwise.entity.Organization;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService organizationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Organization> createOrganization(@Valid @RequestBody OrganizationRequestDTO request) {
        return ResponseEntity.ok(organizationService.createOrganization(request));
    }

    @GetMapping("/{organizationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizationDTO> getOrganization(
            @PathVariable String organizationId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(organizationService.getOrganization(organizationId, user));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrganizationDTO>> getOrganizationsCreatedByAdmin(Authentication authentication) {
        String adminId = authentication.getName();
        return ResponseEntity.ok(organizationService.getOrganizationsCreatedByAdmin(adminId));
    }
}

