package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.*;
import com.swayam.bugwise.entity.*;
import com.swayam.bugwise.enums.*;
import com.swayam.bugwise.exception.ValidationException;
import com.swayam.bugwise.repository.elasticsearch.BugDocumentRepository;
import com.swayam.bugwise.repository.jpa.BugRepository;
import com.swayam.bugwise.repository.jpa.ProjectRepository;
import com.swayam.bugwise.repository.jpa.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BugServiceTest {

    @Mock
    private BugRepository bugRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    BugDocumentRepository bugDocumentRepository;

    @Mock
    private Pageable pageable;

    @InjectMocks
    private BugService bugService;

    private BugRequestDTO bugRequest;
    private Bug bug;
    private Project project;
    private User user;
    private User projectManager;
    private Organization organization;
    private Comment comment;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId("org123");
        organization.setName("Test Org");
        organization.setDescription("Test Organization Description");

        projectManager = new User();
        projectManager.setId("pm123");
        projectManager.setEmail("pm@gmail.com");
        projectManager.setRole(UserRole.PROJECT_MANAGER);
        projectManager.setFirstName("Manager");
        projectManager.setLastName("User");
        projectManager.setPassword("password");

        project = new Project();
        project.setId("project123");
        project.setName("Test Project");
        project.setDescription("Test Description");
        project.setOrganization(organization);
        project.setProjectManager(projectManager);
        project.setStatus(ProjectStatus.ACTIVE);

        user = new User();
        user.setId("user123");
        user.setEmail("test@gmail.com");
        user.setRole(UserRole.DEVELOPER);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("password");
        user.setDeveloperType(DeveloperType.FULL_STACK);
        user.setOrganizations(Set.of(organization));
        user.setAssignedProjects(Set.of(project));

        bug = new Bug();
        bug.setId("bug123");
        bug.setTitle("Test Bug");
        bug.setDescription("Test Description");
        bug.setStatus(BugStatus.NEW);
        bug.setSeverity(BugSeverity.MEDIUM);
        bug.setProject(project);
        bug.setReportedBy(user);
        bug.setBugType(BugType.BACKEND);
        bug.setExpectedTimeHours(5);
        bug.setActualTimeHours(3);
        bug.setAssignedDeveloper(new HashSet<>(Set.of(user)));

        comment = new Comment();
        comment.setId("comment123");
        comment.setContent("Test comment");
        comment.setBug(bug);
        comment.setUser(user);
        bug.setComments(Set.of(comment));

        bugRequest = new BugRequestDTO();
        bugRequest.setTitle("Test");
        bugRequest.setDescription("Test Description");
        bugRequest.setSeverity(BugSeverity.MEDIUM);
        bugRequest.setProjectId("project123");
        bugRequest.setBugType(BugType.BACKEND);
        bugRequest.setExpectedTimeHours(5);
        bugRequest.setActualTimeHours(3);
    }

    @Test
    void createBug_Success() {
        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(user));
        when(projectRepository.findById(bugRequest.getProjectId()))
                .thenReturn(Optional.of(project));
        when(bugRepository.save(any(Bug.class)))
                .thenReturn(bug);

        Bug result = bugService.createBug(bugRequest, "test@gmail.com");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("bug123", result.getId());
        Assertions.assertEquals("Test Bug", result.getTitle());
        Assertions.assertEquals(BugStatus.NEW, result.getStatus());
        verify(bugRepository, times(1)).save(any(Bug.class));
        verify(notificationService, times(1)).sendNotification(any());
    }

    @Test
    void getBug_Success() {
        when(bugRepository.findById("bug123"))
                .thenReturn(Optional.of(bug));

        BugDTO result = bugService.getBug("bug123");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("bug123", result.getId());
        Assertions.assertEquals("Test Bug", result.getTitle());
        Assertions.assertEquals("Test Description", result.getDescription());
        Assertions.assertEquals("Test Org", result.getOrganizationName());
        Assertions.assertEquals("pm@gmail.com", result.getProjectManagerName());
        Assertions.assertEquals(BugType.BACKEND, result.getBugType());
        Assertions.assertEquals(5, result.getExpectedTimeHours());
        Assertions.assertEquals(3, result.getActualTimeHours());
    }

    @Test
    void updateBug_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("pm@gmail.com");
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        when(bugRepository.findById("bug123"))
                .thenReturn(Optional.of(bug));
        when(bugRepository.save(any(Bug.class)))
                .thenReturn(bug);
        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(projectManager));
        when(bugDocumentRepository.save(any(BugDocument.class))).thenReturn(new BugDocument());

        bugRequest.setTitle("Updated Title");
        bugRequest.setDescription("Updated Description");
        bugRequest.setSeverity(BugSeverity.HIGH);

        BugDTO result = bugService.updateBug("bug123", bugRequest, "pm@gmail.com");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Updated Title", result.getTitle());
        Assertions.assertEquals("Updated Description", result.getDescription());
        Assertions.assertEquals(BugSeverity.HIGH, result.getSeverity());
        verify(bugRepository, times(1)).save(any(Bug.class));
        verify(bugDocumentRepository, times(1)).save(any(BugDocument.class));
        verify(notificationService, times(1)).sendNotification(any());
    }

    @Test
    void updateBugStatus_Success() {
        when(bugRepository.findById("bug123"))
                .thenReturn(Optional.of(bug));
        when(bugRepository.save(any(Bug.class)))
                .thenReturn(bug);

        BugDTO result = bugService.updateBugStatus("bug123", BugStatus.OPEN, "test@gmail.com");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(BugStatus.OPEN, result.getStatus());
        verify(bugRepository, times(1)).save(any(Bug.class));
        verify(notificationService, times(1)).sendNotification(any());
    }

    @Test
    void assignBugToDevelopers_Success() {
        User developer2 = new User();
        developer2.setId("user456");
        developer2.setEmail("dev2@gmail.com");
        developer2.setRole(UserRole.DEVELOPER);
        developer2.setDeveloperType(DeveloperType.BACKEND);

        Set<User> developers = new HashSet<>(Set.of(user, developer2));

        when(bugRepository.findById("bug123")).thenReturn(Optional.of(bug));
        when(userRepository.findAllByEmailIn(anySet())).thenReturn(developers);
        when(bugRepository.save(any(Bug.class))).thenReturn(bug);

        BugDTO result = bugService.assignBugToDevelopers("bug123", List.of("test@gmail.com", "dev2@gmail.com"));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, bug.getAssignedDeveloper().size());
        verify(bugRepository, times(1)).save(any(Bug.class));
        verify(notificationService, times(1)).sendNotification(any());
    }

    @Test
    void getBugStatistics_Success() {
        BugStatisticsDTO stats = new BugStatisticsDTO(BugStatus.NEW, 5L);
        when(bugRepository.getBugStatisticsByProject("project123")).thenReturn(List.of(stats));

        List<BugStatisticsDTO> result = bugService.getBugStatistics("project123");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(BugStatus.NEW, result.get(0).getStatus());
        Assertions.assertEquals(5L, result.get(0).getCount());
    }

    @Test
    void findActiveByProjectAndSeverity_Success() {
        Page<Bug> bugPage = new PageImpl<>(Collections.singletonList(bug));
        when(bugRepository.findActiveByProjectAndSeverity(
                "project123", BugSeverity.MEDIUM, Set.of(BugStatus.CLOSED, BugStatus.RESOLVED), pageable))
                .thenReturn(bugPage);

        Page<BugDTO> result = bugService.findActiveByProjectAndSeverity("project123", BugSeverity.MEDIUM, pageable);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals("Test Bug", result.getContent().get(0).getTitle());
    }

    @Test
    void getBugsForUser_Admin_Success() {
        User admin = new User();
        admin.setId("admin123");
        admin.setEmail("admin@gmail.com");
        admin.setRole(UserRole.ADMIN);
        admin.setOrganizations(Set.of(organization));

        Page<Bug> bugPage = new PageImpl<>(Collections.singletonList(bug));
        when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.of(admin));
        when(bugRepository.findByProjectOrganizationIdIn(anySet(), any(Pageable.class))).thenReturn(bugPage);

        Page<BugDTO> result = bugService.getBugsForUser("admin@gmail.com", pageable);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals("Test Bug", result.getContent().get(0).getTitle());
    }

    @Test
    void getBugStatisticsForUser_Developer_Success() {
        BugStatisticsDTO stats = new BugStatisticsDTO(BugStatus.NEW, 3L);
        when(userRepository.findByEmail("dev@gmail.com")).thenReturn(Optional.of(user));
        when(bugRepository.findBugStatisticsByProjectIdIn(anySet())).thenReturn(List.of(stats));

        List<BugStatisticsDTO> result = bugService.getBugStatisticsForUser("dev@gmail.com");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(BugStatus.NEW, result.get(0).getStatus());
        Assertions.assertEquals(3L, result.get(0).getCount());
    }

    @Test
    void validateStatusTransition_ClosedToOther_ThrowsException() {
        bug.setStatus(BugStatus.CLOSED);
        when(bugRepository.findById("bug123")).thenReturn(Optional.of(bug));

        Assertions.assertThrows(ValidationException.class, () -> {
            bugService.updateBugStatus("bug123", BugStatus.OPEN, "test@gmail.com");
        });
    }

    @Test
    void getAssignedBugsForDeveloperInProject_NotAssigned_ThrowsException() {
        User otherDeveloper = new User();
        otherDeveloper.setId("other123");
        otherDeveloper.setEmail("other@gmail.com");
        otherDeveloper.setRole(UserRole.DEVELOPER);

        when(userRepository.findByEmail("other@gmail.com")).thenReturn(Optional.of(otherDeveloper));

        Assertions.assertThrows(ValidationException.class, () -> {
            bugService.getAssignedBugsForDeveloperInProject("other@gmail.com", "project123", pageable);
        });
    }

    @Test
    void createBug_WithComments_Success() {
        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(user));
        when(projectRepository.findById(bugRequest.getProjectId()))
                .thenReturn(Optional.of(project));
        when(bugRepository.save(any(Bug.class)))
                .thenReturn(bug);

        Bug result = bugService.createBug(bugRequest, "test@gmail.com");

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getComments());
        Assertions.assertEquals(1, result.getComments().size());
    }
}