package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.*;
import com.swayam.bugwise.entity.Bug;
import com.swayam.bugwise.entity.Organization;
import com.swayam.bugwise.entity.Project;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.BugStatus;
import com.swayam.bugwise.enums.ProjectStatus;
import com.swayam.bugwise.enums.UserRole;
import com.swayam.bugwise.exception.ValidationException;
import com.swayam.bugwise.repository.jpa.OrganizationRepository;
import com.swayam.bugwise.repository.jpa.ProjectRepository;
import com.swayam.bugwise.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProjectService projectService;

    private Organization testOrg;
    private User projectManager;
    private User developer;
    private User user1;
    private User user2;
    private Project testProject;
    private ProjectRequestDTO testProjectRequest;

    @BeforeEach
    void setUp() {
        testOrg = new Organization();
        testOrg.setId("org1");
        testOrg.setName("Test Org");

        projectManager = new User();
        projectManager.setId("pm1");
        projectManager.setRole(UserRole.PROJECT_MANAGER);
        projectManager.setEmail("pm@test.com");

        developer = new User();
        developer.setId("dev1");
        developer.setRole(UserRole.DEVELOPER);
        developer.setEmail("dev@test.com");

        user1 = new User();
        user1.setId("user1");
        user1.setEmail("user1@test.com");

        user2 = new User();
        user2.setId("user2");
        user2.setEmail("user2@test.com");

        testProject = new Project();
        testProject.setId("proj1");
        testProject.setName("Test Project");
        testProject.setDescription("Test Description");
        testProject.setOrganization(testOrg);
        testProject.setProjectManager(projectManager);
        testProject.setAssignedUsers(new HashSet<>(Set.of(user1, user2)));
        testProject.setStatus(ProjectStatus.ACTIVE);

        testProjectRequest = new ProjectRequestDTO();
        testProjectRequest.setName("Test Project");
        testProjectRequest.setDescription("Test Description");
        testProjectRequest.setOrganizationId("org1");
        testProjectRequest.setProjectManagerId("pm1");
        testProjectRequest.setAssignedUserIds(Set.of("user1", "user2"));
    }

    @Test
    void createProject_Success() {
        when(organizationRepository.findById("org1")).thenReturn(Optional.of(testOrg));
        when(userRepository.findById("pm1")).thenReturn(Optional.of(projectManager));
        when(userRepository.findAllByIdIn(Set.of("user1", "user2"))).thenReturn(Set.of(user1, user2));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);

        Project result = projectService.createProject(testProjectRequest);

        assertNotNull(result);
        assertEquals("Test Project", result.getName());
        assertEquals("Test Description", result.getDescription());
        assertEquals(testOrg, result.getOrganization());
        assertEquals(projectManager, result.getProjectManager());
        assertEquals(2, result.getAssignedUsers().size());

        verify(organizationRepository, times(1)).findById("org1");
        verify(userRepository, times(1)).findById("pm1");
        verify(userRepository, times(1)).findAllByIdIn(Set.of("user1", "user2"));
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void createProject_InvalidProjectManagerRole_ThrowsException() {
        testProjectRequest.setProjectManagerId("dev1");

        when(userRepository.findById("dev1")).thenReturn(Optional.of(developer));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> projectService.createProject(testProjectRequest));

        assertTrue(exception.getMessage().contains("Assigned user must have PROJECT_MANAGER role"));
        verify(userRepository, times(1)).findById("dev1");
    }

    @Test
    void getProject_Success() {
        when(projectRepository.findById("proj1")).thenReturn(Optional.of(testProject));

        ProjectDTO result = projectService.getProject("proj1");

        assertNotNull(result);
        assertEquals("proj1", result.getId());
        assertEquals("Test Project", result.getName());
        assertEquals("org1", result.getOrganizationId());
        assertEquals(2, result.getAssignedUsers().size());

        verify(projectRepository, times(1)).findById("proj1");
    }

    @Test
    void assignUsersToProject_Success() {
        Set<String> userIds = Set.of("user3", "user4");

        User newUser1 = new User();
        newUser1.setId("user3");
        newUser1.setEmail("user3@test.com");
        User newUser2 = new User();
        newUser2.setId("user4");
        newUser2.setEmail("user4@test.com");

        when(projectRepository.findById("proj1")).thenReturn(Optional.of(testProject));
        when(userRepository.findAllByIdIn(userIds)).thenReturn(Set.of(newUser1, newUser2));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectDTO result = projectService.assignUsersToProject("proj1", userIds);

        assertNotNull(result);
        assertEquals(4, result.getAssignedUsers().size());
        verify(notificationService, times(1)).sendNotification(any(NotificationMessageDTO.class));
    }

    @Test
    void updateProject_Success() {
        ProjectUpdateDTO updateDTO = new ProjectUpdateDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setDescription("Updated Description");
        updateDTO.setStatus(ProjectStatus.ACTIVE);
        updateDTO.setProjectManagerId("newPm");
        updateDTO.setAssignedUserIds(Set.of("user1", "user2"));

        User newPm = new User();
        newPm.setId("newPm");
        newPm.setRole(UserRole.PROJECT_MANAGER);

        when(projectRepository.findById("proj1")).thenReturn(Optional.of(testProject));
        when(userRepository.findById("newPm")).thenReturn(Optional.of(newPm));
        when(userRepository.findAllByIdIn(Set.of("user1", "user2"))).thenReturn(Set.of(user1, user2));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectDTO result = projectService.updateProject("proj1", updateDTO);

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(ProjectStatus.ACTIVE, result.getStatus());
        assertEquals("newPm", result.getProjectManager().getId());
        assertEquals(2, result.getAssignedUsers().size());
    }

    @Test
    void removeUsersFromProject_Success() {
        Set<String> userIds = Set.of("user1", "user2");

        when(projectRepository.findById("proj1")).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectDTO result = projectService.removeUsersFromProject("proj1", userIds);

        assertNotNull(result);
        assertEquals(0, result.getAssignedUsers().size());
    }

    @Test
    void getProjectsForUser_AdminRole_ReturnsAllProjects() {
        User admin = new User();
        admin.setEmail("admin@test.com");
        admin.setRole(UserRole.ADMIN);

        Project project2 = new Project();
        project2.setId("proj2");
        project2.setOrganization(testOrg);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(projectRepository.findAll()).thenReturn(List.of(testProject, project2));

        List<ProjectDTO> result = projectService.getProjectsForUser("admin@test.com");

        assertEquals(2, result.size());
        verify(projectRepository, times(1)).findAll();
    }

    @Test
    void getProjectStats_DeveloperRole_ReturnsAssignedBugStats() {
        Bug openBug = new Bug();
        openBug.setStatus(BugStatus.OPEN);
        Bug fixedBug = new Bug();
        fixedBug.setStatus(BugStatus.RESOLVED);
        testProject.setBugs(Set.of(openBug, fixedBug));

        when(userRepository.findByEmail("dev@test.com")).thenReturn(Optional.of(developer));
        when(projectRepository.findByAssignedBugsDeveloperId("dev1")).thenReturn(List.of(testProject));

        List<ProjectStatsDTO> result = projectService.getProjectStats("dev@test.com");

        assertEquals(1, result.size());
        ProjectStatsDTO stats = result.get(0);
        assertEquals("Test Project", stats.getProjectName());
        assertEquals(1, stats.getStatusCounts().get(BugStatus.OPEN.name()));
        assertEquals(1, stats.getStatusCounts().get(BugStatus.RESOLVED.name()));
    }

    @Test
    void searchProjectsInOrganization_Success() {
        String searchTerm = "test";
        Pageable pageable = Pageable.unpaged();

        Page<Project> expectedPage = new PageImpl<>(List.of(testProject));

        when(projectRepository.searchProjectsInOrganization("org1", searchTerm, pageable))
                .thenReturn(expectedPage);

        Page<Project> result = projectService.searchProjectsInOrganization("org1", searchTerm, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("Test Project", result.getContent().get(0).getName());
    }
}