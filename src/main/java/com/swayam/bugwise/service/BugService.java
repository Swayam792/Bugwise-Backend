package com.swayam.bugwise.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.swayam.bugwise.dto.*;
import com.swayam.bugwise.entity.Bug;
import com.swayam.bugwise.entity.BugDocument;
import com.swayam.bugwise.entity.Project;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.BugSeverity;
import com.swayam.bugwise.enums.BugStatus;
import com.swayam.bugwise.enums.UserRole;
import com.swayam.bugwise.exception.ValidationException;
import com.swayam.bugwise.repository.elasticsearch.BugDocumentRepository;
import com.swayam.bugwise.repository.jpa.BugRepository;
import com.swayam.bugwise.repository.jpa.ProjectRepository;
import com.swayam.bugwise.repository.jpa.UserRepository;
import com.swayam.bugwise.utils.DTOConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.elasticsearch.core.SearchHits;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.springframework.data.elasticsearch.core.SearchHit;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BugService {
    private final BugRepository bugRepository;
    private final BugDocumentRepository bugDocumentRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public Bug createBug(BugRequestDTO request) {
        User currentUser = getCurrentUser();
        validateUserCanCreateBug(currentUser);

        Bug bug = new Bug();
        bug.setTitle(request.getTitle());
        bug.setDescription(request.getDescription());
        bug.setStatus(BugStatus.NEW);
        bug.setSeverity(request.getSeverity());

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new NoSuchElementException("Project not found with id " + request.getProjectId()));

        bug.setProject(project);

        Bug savedBug = bugRepository.save(bug);
        indexBugInElasticsearch(savedBug);
        return savedBug;
    }

    @CacheEvict(value = "bugs", key = "#bugId")
    public Bug updateBug(String bugId, BugRequestDTO request) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found"));
        validateUserCanUpdateBug(bug);

        bug.setTitle(request.getTitle());
        bug.setDescription(request.getDescription());
        bug.setSeverity(request.getSeverity());

        Bug updatedBug = bugRepository.save(bug);
        indexBugInElasticsearch(updatedBug);
        return updatedBug;
    }

    @Cacheable(value = "bugs", key = "#bugId")
    public BugDTO getBug(String bugId) {
        log.info("Fetching bug from database: {}", bugId);
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found with id: " + bugId));
        return DTOConverter.convertToDTO(bug, BugDTO.class);
    }

    public List<BugDocument> searchBugs(String query) {
        return bugDocumentRepository.findByTitleContainingOrDescriptionContaining(query);
    }

    @CacheEvict(value = "bugs", key = "#bugId")
    public Bug assignBug(String bugId, String developerId) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found"));
        User developer = userRepository.findById(developerId)
                .orElseThrow(() -> new NoSuchElementException("Developer not found"));

        validateDeveloperAssignment(developer);

        bug.setAssignedDeveloper(developer);
        bug.setStatus(BugStatus.ASSIGNED);

        Bug updatedBug = bugRepository.save(bug);
        indexBugInElasticsearch(updatedBug);
        return updatedBug;
    }

    @CacheEvict(value = "bugs", key = "#bugId")
    public Bug updateBugStatus(String bugId, BugStatus newStatus) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found"));
        validateStatusTransition(bug, newStatus);

        bug.setStatus(newStatus);
        Bug updatedBug = bugRepository.save(bug);
        indexBugInElasticsearch(updatedBug);
        return updatedBug;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    private void validateUserCanCreateBug(User user) {
        if (!EnumSet.of(UserRole.TESTER, UserRole.DEVELOPER).contains(user.getRole())) {
            throw new ValidationException(Map.of("error", "User does not have permission to create bugs"));
        }
    }

    private void validateUserCanUpdateBug(Bug bug) {
        User currentUser = getCurrentUser();
        boolean isAssignedDeveloper = bug.getAssignedDeveloper() != null &&
                bug.getAssignedDeveloper().getId().equals(currentUser.getId());
        boolean isProjectManager = currentUser.getRole() == UserRole.PROJECT_MANAGER &&
                bug.getProject().getProjectManager().getId().equals(currentUser.getId());

        if (!isAssignedDeveloper && !isProjectManager && currentUser.getRole() != UserRole.ADMIN) {
            throw new ValidationException(Map.of("error", "User does not have permission to update this bug"));
        }
    }

    private void validateDeveloperAssignment(User developer) {
        if (developer.getRole() != UserRole.DEVELOPER) {
            throw new ValidationException(Map.of("error", "User must have developer role for bug assignment"));
        }
    }

    private void validateStatusTransition(Bug bug, BugStatus newStatus) {
        if (bug.getStatus() == BugStatus.CLOSED && newStatus != BugStatus.REOPENED) {
            throw new ValidationException(Map.of("error", "Closed bugs can only be reopened"));
        }
    }

    private void indexBugInElasticsearch(Bug bug) {
        BugDocument bugDocument = new BugDocument();
        bugDocument.setId(bug.getId());
        bugDocument.setTitle(bug.getTitle());
        bugDocument.setDescription(bug.getDescription());
        bugDocument.setStatus(bug.getStatus());
        bugDocument.setSeverity(bug.getSeverity());

        bugDocumentRepository.save(bugDocument);
    }

    public Page<Bug> searchBugsInProject(String projectId, String searchTerm, Pageable pageable) {
        return bugRepository.searchBugsInProject(projectId, searchTerm, pageable);
    }

    public List<BugStatisticsDTO> getBugStatistics(String projectId) {
        return bugRepository.getBugStatisticsByProject(projectId);
    }

    public List<Bug> findActiveByProjectAndSeverity(String projectId, BugSeverity severity) {
        return bugRepository.findActiveByProjectAndSeverity(projectId, severity, BugStatus.CLOSED, BugStatus.RESOLVED);
    }
}