package com.swayam.bugwise.controller;

import com.swayam.bugwise.dto.*;
import com.swayam.bugwise.entity.Bug;
import com.swayam.bugwise.entity.BugDocument;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.BugSeverity;
import com.swayam.bugwise.enums.BugStatus;
import com.swayam.bugwise.service.AIAnalysisService;
import com.swayam.bugwise.service.BugService;
import com.swayam.bugwise.utils.DTOConverter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bugs")
@RequiredArgsConstructor
public class BugController {
    private final BugService bugService;
    private final AIAnalysisService aiAnalysisService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TESTER', 'DEVELOPER', 'PROJECT_MANAGER')")
    public ResponseEntity<Bug> createBug(@Valid @RequestBody BugRequestDTO request, Authentication authentication) {
        Bug bug = bugService.createBug(request, authentication.getName());
        return ResponseEntity.ok(bug);
    }

    @PutMapping("/{bugId}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'PROJECT_MANAGER')")
    public ResponseEntity<BugDTO> updateBug(@PathVariable String bugId, @Valid @RequestBody BugRequestDTO request) {
        return ResponseEntity.ok(bugService.updateBug(bugId, request));
    }

    @GetMapping("/{bugId}")
    public ResponseEntity<BugDTO> getBug(@PathVariable String bugId) {
        return ResponseEntity.ok(bugService.getBug(bugId));
    }

    @PostMapping("/{bugId}/status")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'TESTER', 'PROJECT_MANAGER')")
    public ResponseEntity<BugDTO> updateBugStatus(@PathVariable String bugId, @RequestParam BugStatus status) {
        return ResponseEntity.ok(bugService.updateBugStatus(bugId, status));
    }

    @GetMapping("/project/{projectId}/search")
    public ResponseEntity<Page<BugDTO>> searchBugsInProject(
            @PathVariable String projectId,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bugService.searchBugsInProject(projectId, searchTerm, status, pageable));
    }

    @GetMapping("/project/{projectId}/assigned")
    public ResponseEntity<Page<BugDTO>> getAssignedBugsForDeveloperInProject(
            Authentication authentication,
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bugService.getAssignedBugsForDeveloperInProject(
                authentication.getName(), projectId, pageable));
    }

    @GetMapping("/project/{projectId}/statistics")
    public ResponseEntity<List<BugStatisticsDTO>> getBugStatistics(@PathVariable String projectId) {
        return ResponseEntity.ok(bugService.getBugStatistics(projectId));
    }

    @GetMapping("/project/{projectId}/active")
    public ResponseEntity<Page<BugDTO>> getActiveBugs(
            @PathVariable String projectId,
            @RequestParam BugSeverity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BugDTO> activeBugs = bugService.findActiveByProjectAndSeverity(projectId, severity, pageable);

        return ResponseEntity.ok(activeBugs);
    }

    @GetMapping("/my-bugs/latest")
    public ResponseEntity<Page<BugDTO>> getMyBugs(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bugService.getBugsForUser(authentication.getName(), pageable));
    }

    @GetMapping("/my-bugs/assigned")
    public ResponseEntity<Page<BugDTO>> getMyAssignedBugs(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bugService.getAssignedBugsForDeveloper(authentication.getName(), pageable));
    }

    @GetMapping("/my-bugs/statistics")
    public ResponseEntity<List<BugStatisticsDTO>> getMyBugStatistics(Authentication authentication) {
        return ResponseEntity.ok(bugService.getBugStatisticsForUser(authentication.getName()));
    }

    @GetMapping("/{bugId}/suggestions")
    public ResponseEntity<BugSuggestionDTO> getBugSuggestions(@PathVariable String bugId) {
        return ResponseEntity.ok(aiAnalysisService.getBugSuggestions(bugId));
    }
}