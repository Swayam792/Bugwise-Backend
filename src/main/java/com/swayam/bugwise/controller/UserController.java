package com.swayam.bugwise.controller;

import com.swayam.bugwise.dto.UpdatePasswordRequestDTO;
import com.swayam.bugwise.dto.UpdateUserRequestDTO;
import com.swayam.bugwise.dto.UserDTO;
import com.swayam.bugwise.dto.UserDetailsDTO;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.UserRole;
import com.swayam.bugwise.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/active")
    public ResponseEntity<List<UserDetailsDTO>> getActiveUsersByOrganizationAndRole(
            @RequestParam String organizationId,
            @RequestParam UserRole role) {
        List<UserDetailsDTO> users = userService.findActiveUsersByOrganizationAndRole(organizationId, role);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/active/count")
    public ResponseEntity<Long> countActiveUsersByOrganizationAndRole(
            @RequestParam String organizationId,
            @RequestParam UserRole role) {
        long count = userService.countActiveUsersByOrganizationAndRole(organizationId, role);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDetailsDTO> getCurrentUserDetails(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUserDetails(authentication.getName()));
    }

    @GetMapping("/descendant-count")
    public ResponseEntity<Map<UserRole, Long>> getDescendantUserCount(Authentication authentication) {
        String username = authentication.getName();
        Map<UserRole, Long> descendantCount = userService.getDescendantUserCount(username);
        return ResponseEntity.ok(descendantCount);
    }

    @PutMapping("/me")
    public ResponseEntity<UserDetailsDTO> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequestDTO request,
            Authentication authentication) {
        String email = authentication.getName();
        UserDetailsDTO updatedUser = userService.updateUserDetails(email, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{userId}/password")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<Void> updatePassword(
            @PathVariable String userId,
            @Valid @RequestBody UpdatePasswordRequestDTO request) {
        userService.updateUserPassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/developers")
    public ResponseEntity<List<UserDetailsDTO>> getDevelopers(@RequestParam(value = "projectId", required = false) String projectId,
                                                    @RequestParam(value = "organizationId", required = false) String organizationId) {
        return ResponseEntity.ok(userService.findDevelopers(projectId, organizationId));
    }
}