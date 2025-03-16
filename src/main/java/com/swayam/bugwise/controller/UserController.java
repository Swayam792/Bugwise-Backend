package com.swayam.bugwise.controller;

import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.UserRole;
import com.swayam.bugwise.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1//users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/active")
    public ResponseEntity<List<User>> getActiveUsersByOrganizationAndRole(
            @RequestParam String organizationId,
            @RequestParam UserRole role) {
        List<User> users = userService.findActiveUsersByOrganizationAndRole(organizationId, role);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/active/count")
    public ResponseEntity<Long> countActiveUsersByOrganizationAndRole(
            @RequestParam String organizationId,
            @RequestParam UserRole role) {
        long count = userService.countActiveUsersByOrganizationAndRole(organizationId, role);
        return ResponseEntity.ok(count);
    }
}