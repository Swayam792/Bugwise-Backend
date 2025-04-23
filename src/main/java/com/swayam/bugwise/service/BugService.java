package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.*;
import com.swayam.bugwise.entity.*;
import com.swayam.bugwise.enums.BugSeverity;
import com.swayam.bugwise.enums.BugStatus;
import com.swayam.bugwise.enums.DeveloperType;
import com.swayam.bugwise.enums.UserRole;
import com.swayam.bugwise.exception.ValidationException;
import com.swayam.bugwise.repository.elasticsearch.BugDocumentRepository;
import com.swayam.bugwise.repository.jpa.BugRepository;
import com.swayam.bugwise.repository.jpa.ProjectRepository;
import com.swayam.bugwise.repository.jpa.UserRepository;
import com.swayam.bugwise.utils.DTOConverter;

import co.elastic.clients.elasticsearch.security.get_role.Role;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.elasticsearch.core.SearchHits;
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
    private final ElasticsearchOperations elasticsearchOperations;

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public Bug createBug(BugRequestDTO request, String email) {
        User currentUser = userRepository.findByEmail(email).orElseThrow(() ->
                new NoSuchElementException("User not found"));

        Bug bug = new Bug();
        bug.setTitle(request.getTitle());
        bug.setDescription(request.getDescription());
        bug.setStatus(BugStatus.NEW);
        bug.setSeverity(request.getSeverity());
        bug.setReportedBy(currentUser);

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new NoSuchElementException("Project not found with id " + request.getProjectId()));

        bug.setProject(project);

        Bug savedBug = bugRepository.save(bug);
        indexBugInElasticsearch(savedBug);
        return savedBug;
    }

    @CachePut(value = "bugs", key = "#bugId")
    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public BugDTO updateBug(String bugId, BugRequestDTO request) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found"));
        validateUserCanUpdateBug(bug);

        bug.setTitle(request.getTitle());
        bug.setDescription(request.getDescription());
        bug.setSeverity(request.getSeverity());

        if (request.getBugType() != null) {
            bug.setBugType(request.getBugType());
        }

        if (request.getExpectedTimeHours() != null) {
            bug.setExpectedTimeHours(request.getExpectedTimeHours());
        }

        Bug updatedBug = bugRepository.save(bug);
        indexBugInElasticsearch(updatedBug);
        return DTOConverter.convertToDTO(updatedBug, BugDTO.class);
    }

    @Cacheable(value = "bugs", key = "#bugId")
    public BugDTO getBug(String bugId) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found with id: " + bugId));

        BugDTO dto = new BugDTO();
        dto.setId(bug.getId());
        dto.setTitle(bug.getTitle());
        dto.setDescription(bug.getDescription());
        dto.setStatus(bug.getStatus());
        dto.setSeverity(bug.getSeverity());
        dto.setCreatedAt(bug.getCreatedAt());
        dto.setUpdatedAt(bug.getUpdatedAt());
        dto.setBugType(bug.getBugType());
        dto.setActualTimeHours(bug.getActualTimeHours());
        dto.setExpectedTimeHours(bug.getExpectedTimeHours());

        Project project = bug.getProject();
        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setId(project.getId());
        projectDTO.setName(project.getName());
        projectDTO.setDescription(project.getDescription());
        dto.setProject(projectDTO);

        if (project.getOrganization() != null) {
            dto.setOrganizationId(project.getOrganization().getId());
            dto.setOrganizationName(project.getOrganization().getName());
        }

        if (project.getProjectManager() != null) {
            dto.setProjectManagerId(project.getProjectManager().getId());
            dto.setProjectManagerName(project.getProjectManager().getUsername());
        }

        if (bug.getReportedBy() != null) {
            UserDetailsDTO reportedByDTO = new UserDetailsDTO();
            reportedByDTO.setId(bug.getReportedBy().getId());
            reportedByDTO.setRole(bug.getReportedBy().getRole());
            reportedByDTO.setEmail(bug.getReportedBy().getEmail());
            reportedByDTO.setFirstName(bug.getReportedBy().getFirstName());
            reportedByDTO.setLastName(bug.getReportedBy().getLastName());
            dto.setReportedBy(reportedByDTO);
        }

        if (!bug.getAssignedDeveloper().isEmpty()) {
            User developer = bug.getAssignedDeveloper().iterator().next();
            UserDetailsDTO developerDTO = new UserDetailsDTO();
            developerDTO.setId(developer.getId());
            developerDTO.setRole(developer.getRole());
            developerDTO.setEmail(developer.getEmail());
            developerDTO.setFirstName(developer.getFirstName());
            developerDTO.setLastName(developer.getLastName());
            dto.setAssignedDeveloper(developerDTO);
        }

        return dto;
    }

    private void indexBugInElasticsearch(Bug bug) {
        BugDocument bugDocument = new BugDocument();
        bugDocument.setId(bug.getId());
        bugDocument.setTitle(bug.getTitle());
        bugDocument.setDescription(bug.getDescription());
        bugDocument.setStatus(bug.getStatus());
        bugDocument.setSeverity(bug.getSeverity());
        bugDocument.setProjectId(bug.getProject().getId());
        bugDocument.setProjectName(bug.getProject().getName());
        bugDocument.setCreatedAt(bug.getCreatedAt());
        bugDocument.setUpdatedAt(bug.getUpdatedAt());
        bugDocument.setBugType(bug.getBugType());
        bugDocument.setExpectedTimeHours(bug.getExpectedTimeHours());
        bugDocument.setActualTimeHours(bug.getActualTimeHours());

        if (bug.getAssignedDeveloper() != null) {
            bugDocument.setAssignedDeveloperId(bug.getAssignedDeveloper().stream().map(e -> e.getId()).collect(Collectors.joining(",")));
            bugDocument.setAssignedDeveloperEmail(bug.getAssignedDeveloper().stream().map(e -> e.getEmail()).collect(Collectors.joining(",")));
        }

        if (bug.getReportedBy() != null) {
            bugDocument.setReportedById(bug.getReportedBy().getId());
        }

        if (bug.getProject().getOrganization() != null) {
            BugDocument.OrganizationRef orgRef = new BugDocument.OrganizationRef();
            orgRef.setId(bug.getProject().getOrganization().getId());
            orgRef.setName(bug.getProject().getOrganization().getName());
            bugDocument.setOrganization(orgRef);
        }

        bugDocumentRepository.save(bugDocument);
    }

    @CachePut(value = "bugs", key = "#bugId")
    public BugDTO assignBugToDevelopers(String bugId, List<String> developerEmails) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found"));

        Set<User> developers = userRepository.findAllByEmailIn(new HashSet<>(developerEmails));

        bug.setAssignedDeveloper((Set<User>) developers);
        bug.setStatus(BugStatus.OPEN);

        Bug updatedBug = bugRepository.save(bug);
        indexBugInElasticsearch(updatedBug);
        return DTOConverter.convertToDTO(updatedBug, BugDTO.class);
    }

    @CacheEvict(value = "bugs", key = "#bugId")
    public BugDTO updateBugStatus(String bugId, BugStatus newStatus) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found"));
        validateStatusTransition(bug, newStatus);

        bug.setStatus(newStatus);
        Bug updatedBug = bugRepository.save(bug);
        indexBugInElasticsearch(updatedBug);
        return DTOConverter.convertToDTO(updatedBug, BugDTO.class);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
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
                bug.getAssignedDeveloper().contains(currentUser.getId());
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

    public List<BugStatisticsDTO> getBugStatistics(String projectId) {
        return bugRepository.getBugStatisticsByProject(projectId);
    }

    public Page<BugDTO> findActiveByProjectAndSeverity(String projectId, BugSeverity severity, Pageable pageable) {
        Set<BugStatus> excludedStatuses = Set.of(BugStatus.CLOSED, BugStatus.RESOLVED);
        Page<Bug> bugs = bugRepository.findActiveByProjectAndSeverity(projectId, severity, excludedStatuses, pageable);
        return bugs.map(bug -> DTOConverter.convertToDTO(bug, BugDTO.class));
    }


    public Page<BugDTO> getBugsForUser(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == UserRole.ADMIN) {
            Set<String> organizationIds = user.getOrganizations().stream()
                    .map(Organization::getId)
                    .collect(Collectors.toSet());

            return bugRepository.findByProjectOrganizationIdIn(organizationIds, pageable)
                    .map(bug -> {
                        BugDTO dto = DTOConverter.convertToDTO(bug, BugDTO.class);
                        dto.setProject(DTOConverter.convertToDTO(bug.getProject(), ProjectDTO.class));
                        dto.setOrganizationName(bug.getProject().getOrganization().getName());
                        if (bug.getProject().getProjectManager() != null) {
                            dto.setProjectManagerName(bug.getProject().getProjectManager().getUsername());
                        }
                        return dto;
                    });
        } else {
            Set<String> projectIds = user.getAssignedProjects().stream()
                    .map(Project::getId)
                    .collect(Collectors.toSet());

            projectIds.addAll(user.getManagedProjects().stream()
                    .map(Project::getId)
                    .collect(Collectors.toSet()));

            return bugRepository.findByProjectIdIn(projectIds, pageable)
                    .map(bug -> {
                        BugDTO dto = DTOConverter.convertToDTO(bug, BugDTO.class);
                        dto.setProject(DTOConverter.convertToDTO(bug.getProject(), ProjectDTO.class));
                        dto.setOrganizationName(bug.getProject().getOrganization().getName());
                        if (bug.getProject().getProjectManager() != null) {
                            dto.setProjectManagerName(bug.getProject().getProjectManager().getUsername());
                        }
                        return dto;
                    });
        }
    }

    public List<BugStatisticsDTO> getBugStatisticsForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == UserRole.ADMIN) {
            return bugRepository.findBugStatistics();
        } else if (user.getRole() == UserRole.PROJECT_MANAGER || user.getRole() == UserRole.DEVELOPER || user.getRole() == UserRole.TESTER) {
            Set<String> projectIds = user.getManagedProjects().stream()
                    .map(Project::getId)
                    .collect(Collectors.toSet());
            return bugRepository.findBugStatisticsByProjectIdIn(projectIds);
        } else {
            throw new RuntimeException("Invalid role");
        }
    }

    public Page<BugDTO> searchBugsInProject(String projectId, String searchTerm, String status, Pageable pageable) {
        List<String> bugIds;

        if (searchTerm != null && !searchTerm.isEmpty()) {
            bugIds = searchBugDocumentsInProject(projectId, searchTerm);
            log.info("Bug IDs from search: {}", bugIds);

            if (bugIds.isEmpty()) {
                return Page.empty(pageable);
            }
        } else {
            bugIds = Collections.emptyList();
        }

        Specification<Bug> spec = Specification.where((root, query, cb) ->
                cb.equal(root.get("project").get("id"), projectId));

        if (!bugIds.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    root.get("id").in(bugIds));
        }

        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), BugStatus.valueOf(status)));
        }

        return bugRepository.findAll(spec, pageable)
                .map(bug -> DTOConverter.convertToDTO(bug, BugDTO.class));
    }

    private List<String> searchBugDocumentsInProject(String projectId, String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return Collections.emptyList();
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m.term(t -> t
                                        .field("projectId")
                                        .value(projectId)
                                ))
                                .must(m -> m.multiMatch(mm -> mm
                                        .fields("title", "description")
                                        .query(searchTerm)
                                ))
                        )
                )
                .build();

        SearchHits<BugDocument> hits = elasticsearchOperations.search(query, BugDocument.class);

        return hits.stream()
                .map(SearchHit::getId)
                .collect(Collectors.toList());
    }

    public Page<BugDTO> getAssignedBugsForDeveloper(String developerId, Pageable pageable) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .term(t -> t
                                .field("assignedDeveloperId")
                                .value(developerId)
                        )
                )
                .build();

        SearchHits<BugDocument> hits = elasticsearchOperations.search(query, BugDocument.class);
        List<String> bugIds = hits.stream()
                .map(SearchHit::getId)
                .collect(Collectors.toList());

        if (bugIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return bugRepository.findByIdIn(bugIds, pageable)
                .map(bug -> DTOConverter.convertToDTO(bug, BugDTO.class));
    }

    public Page<BugDTO> getAssignedBugsForDeveloperInProject(String email, String projectId, Pageable pageable) {
        User developer = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        boolean isAssignedToProject = developer.getAssignedProjects().stream()
                .anyMatch(p -> p.getId().equals(projectId));

        if (!isAssignedToProject) {
            throw new ValidationException(Map.of("error", "Developer is not assigned to this project"));
        }

        Page<Bug> bugs = bugRepository.findByProjectIdAndAssignedDeveloperId(projectId, developer.getId(), pageable);

        return bugs.map(bug -> {
            BugDTO dto = DTOConverter.convertToDTO(bug, BugDTO.class);
            dto.setProject(DTOConverter.convertToDTO(bug.getProject(), ProjectDTO.class));
            dto.setOrganizationName(bug.getProject().getOrganization().getName());
            if (bug.getProject().getProjectManager() != null) {
                dto.setProjectManagerName(bug.getProject().getProjectManager().getUsername());
            }
            return dto;
        });
    }
}