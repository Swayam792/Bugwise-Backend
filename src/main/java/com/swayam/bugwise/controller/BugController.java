package com.swayam.bugwise.controller;

import com.swayam.bugwise.dto.BugDTO;
import com.swayam.bugwise.dto.BugRequestDTO;
import com.swayam.bugwise.dto.BugStatisticsDTO;
import com.swayam.bugwise.entity.Bug;
import com.swayam.bugwise.entity.BugDocument;
import com.swayam.bugwise.enums.BugSeverity;
import com.swayam.bugwise.enums.BugStatus;
import com.swayam.bugwise.service.AIAnalysisService;
import com.swayam.bugwise.service.BugService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bugs")
@RequiredArgsConstructor
public class BugController {
    private final BugService bugService;
    private final AIAnalysisService aiAnalysisService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TESTER', 'DEVELOPER')")
    public ResponseEntity<Bug> createBug(@Valid @RequestBody BugRequestDTO request) {
        Bug bug = bugService.createBug(request);
        aiAnalysisService.analyzeBug(bug.getId());
        return ResponseEntity.ok(bug);
    }

    @PutMapping("/{bugId}")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'PROJECT_MANAGER')")
    public ResponseEntity<Bug> updateBug(@PathVariable String bugId, @Valid @RequestBody BugRequestDTO request) {
        return ResponseEntity.ok(bugService.updateBug(bugId, request));
    }

    @GetMapping("/{bugId}")
    public ResponseEntity<BugDTO> getBug(@PathVariable String bugId) {
        return ResponseEntity.ok(bugService.getBug(bugId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<BugDocument>> searchBugs(@RequestParam String query) {
        return ResponseEntity.ok(bugService.searchBugs(query));
    }

    @PostMapping("/{bugId}/assign")
    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'ADMIN')")
    public ResponseEntity<Bug> assignBug(@PathVariable String bugId, @RequestParam String developerId) {
        return ResponseEntity.ok(bugService.assignBug(bugId, developerId));
    }

    @PostMapping("/{bugId}/status")
    @PreAuthorize("hasAnyRole('DEVELOPER', 'TESTER', 'PROJECT_MANAGER')")
    public ResponseEntity<Bug> updateBugStatus(@PathVariable String bugId, @RequestParam BugStatus status) {
        return ResponseEntity.ok(bugService.updateBugStatus(bugId, status));
    }

    @GetMapping("/project/{projectId}/search")
    public ResponseEntity<Page<Bug>> searchBugsInProject(
            @PathVariable String projectId,
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(bugService.searchBugsInProject(projectId, searchTerm, pageable));
    }

    @GetMapping("/project/{projectId}/statistics")
    public ResponseEntity<List<BugStatisticsDTO>> getBugStatistics(@PathVariable String projectId) {
        return ResponseEntity.ok(bugService.getBugStatistics(projectId));
    }

    @GetMapping("/project/{projectId}/active")
    public ResponseEntity<List<Bug>> getActiveBugs(
            @PathVariable String projectId,
            @RequestParam BugSeverity severity) {
        return ResponseEntity.ok(bugService.findActiveByProjectAndSeverity(projectId, severity));
    }

    @GetMapping("/my-bugs/latest")
    public ResponseEntity<Page<BugDTO>> getMyBugs(
            Authentication authentication,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "5") Integer size) {

        Page<BugDTO> bugs = bugService.getBugsForUser(authentication.getName(), page, size);
        return ResponseEntity.ok(bugs);
    }

    @GetMapping("/my-bugs/statistics")
    public ResponseEntity<List<BugStatisticsDTO>> getMyBugStatistics(Authentication authentication) {
        return ResponseEntity.ok(bugService.getBugStatisticsForUser(authentication.getName()));
    }
}
