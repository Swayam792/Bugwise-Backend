package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.*;
import com.swayam.bugwise.entity.Organization;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.UserRole;
import com.swayam.bugwise.exception.UnauthorizedAccessException;
import com.swayam.bugwise.exception.ValidationException;
import com.swayam.bugwise.repository.jpa.OrganizationRepository;
import com.swayam.bugwise.repository.jpa.ProjectRepository;
import com.swayam.bugwise.repository.jpa.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private User adminUser;
    private User regularUser;
    private Organization organization;
    private OrganizationRequestDTO organizationRequestDTO;
    private OrganizationStatsDTO organizationStatsDTO;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId("user1");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(UserRole.ADMIN);

        regularUser = new User();
        regularUser.setId("user2");
        regularUser.setEmail("user@example.com");
        regularUser.setRole(UserRole.DEVELOPER);

        organization = new Organization();
        organization.setId("org1");
        organization.setName("Test Org");
        organization.setDescription("Test Description");
        organization.setAdmin(adminUser);
        organization.setUsers(new HashSet<>(Set.of(adminUser)));
        organization.setProjects(new HashSet<>());

        organizationRequestDTO = new OrganizationRequestDTO();
        organizationRequestDTO.setName("Test Org");
        organizationRequestDTO.setDescription("Test Description");

        organizationStatsDTO = new OrganizationStatsDTO();
        organizationStatsDTO.setOrganizationId("org1");
        organizationStatsDTO.setOrganizationName("Test Org");
        organizationStatsDTO.setProjectCount(0);
        organizationStatsDTO.setMemberCount(1);
    }

    @Test
    void createOrganization_WithValidRequest_ShouldCreateOrganization() {
        when(organizationRepository.existsByName(any())).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(organization);

        organizationService.createOrganization(organizationRequestDTO, adminUser);

        verify(organizationRepository).existsByName(organizationRequestDTO.getName());
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void createOrganization_WithDuplicateName_ShouldThrowValidationException() {
        when(organizationRepository.existsByName(any())).thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> organizationService.createOrganization(organizationRequestDTO, adminUser));

        assertNotNull(exception.getErrors());
        assertTrue(exception.getErrors().containsKey("name"));
        assertEquals("Organization name already exists", exception.getErrors().get("name"));
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void getOrganization_WithAdminUser_ShouldReturnOrganizationDTO() {
        when(organizationRepository.findById("org1")).thenReturn(Optional.of(organization));

        OrganizationDTO result = organizationService.getOrganization("org1", adminUser);

        assertNotNull(result);
        assertEquals(organization.getId(), result.getId());
        assertEquals(organization.getName(), result.getName());
        assertEquals(organization.getDescription(), result.getDescription());
    }

    @Test
    void getOrganization_WithNonAdminUser_ShouldThrowUnauthorizedAccessException() {
        when(organizationRepository.findById("org1")).thenReturn(Optional.of(organization));

        assertThrows(UnauthorizedAccessException.class,
                () -> organizationService.getOrganization("org1", regularUser));
    }

    @Test
    void getOrganization_WithNonExistentId_ShouldThrowNoSuchElementException() {
        when(organizationRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> organizationService.getOrganization("invalid-id", adminUser));
    }

    @Test
    void getOrganizationsForUser_WithAdminUser_ShouldReturnAllOrganizations() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(organizationRepository.findAll()).thenReturn(List.of(organization));

        List<OrganizationDTO> result = organizationService.getOrganizationsForUser("admin@example.com");

        assertEquals(1, result.size());
        assertEquals(organization.getId(), result.get(0).getId());
    }

    @Test
    void getOrganizationsForUser_WithRegularUser_ShouldReturnUserOrganizations() {
        regularUser.setOrganizations(new HashSet<>(Set.of(organization)));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(regularUser));

        List<OrganizationDTO> result = organizationService.getOrganizationsForUser("user@example.com");

        assertEquals(1, result.size());
        assertEquals(organization.getId(), result.get(0).getId());
    }

    @Test
    void getOrganizationsForUser_WithNonExistentUser_ShouldThrowRuntimeException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> organizationService.getOrganizationsForUser("nonexistent@example.com"));
    }

    @Test
    void addUsersToOrganization_WithValidUsers_ShouldAddUsers() {
        Set<String> userIds = Set.of("user-2");
        when(organizationRepository.findById("org1")).thenReturn(Optional.of(organization));
        when(userRepository.findAllById(userIds)).thenReturn(List.of(regularUser));

        organizationService.addUsersToOrganization("org1", userIds);

        assertTrue(organization.getUsers().contains(regularUser));
        verify(organizationRepository).save(organization);
    }

    @Test
    void addUsersToOrganization_WithInvalidUsers_ShouldThrowNoSuchElementException() {
        Set<String> userIds = Set.of("invalid-user");
        when(organizationRepository.findById("org1")).thenReturn(Optional.of(organization));
        when(userRepository.findAllById(userIds)).thenReturn(Collections.emptyList());

        assertThrows(NoSuchElementException.class,
                () -> organizationService.addUsersToOrganization("org1", userIds));
    }

    @Test
    void getUsersInOrganization_ShouldReturnUserDetailsDTOs() {
        organization.getUsers().add(regularUser);
        when(organizationRepository.findById("org1")).thenReturn(Optional.of(organization));

        Set<UserDetailsDTO> result = organizationService.getUsersInOrganization("org1");

        assertEquals(2, result.size());
    }

    @Test
    void getOrganizationStats_ShouldReturnCorrectStats() {
        when(organizationRepository.findAll()).thenReturn(List.of(organization));

        List<OrganizationStatsDTO> result = organizationService.getOrganizationStats();

        assertEquals(1, result.size());
        assertEquals(organizationStatsDTO.getOrganizationId(), result.get(0).getOrganizationId());
        assertEquals(organizationStatsDTO.getMemberCount(), result.get(0).getMemberCount());
        assertEquals(organizationStatsDTO.getProjectCount(), result.get(0).getProjectCount());
    }

    @Test
    void updateOrganization_WithValidData_ShouldUpdateOrganization() {
        OrganizationRequestDTO updateDTO = new OrganizationRequestDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setDescription("Updated Description");

        when(organizationRepository.existsByName("Updated Name")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization savedOrg = invocation.getArgument(0);
            assertEquals("Updated Name", savedOrg.getName());
            assertEquals("Updated Description", savedOrg.getDescription());
            return savedOrg;
        });

        organizationService.updateOrganization("org1", updateDTO);

        verify(organizationRepository, times(1)).save(any(Organization.class));
        verify(organizationRepository, times(1)).existsByName("Updated Name");
    }

    @Test
    void updateOrganization_WithDuplicateName_ShouldThrowValidationException() {
        OrganizationRequestDTO updateDTO = new OrganizationRequestDTO();
        updateDTO.setName("Duplicate Name");
        updateDTO.setDescription("Updated Description");

        when(organizationRepository.existsByName("Duplicate Name")).thenReturn(true);

        assertThrows(ValidationException.class,
                () -> organizationService.updateOrganization("org1", updateDTO));

        verify(organizationRepository, never()).save(any());
    }

    @Test
    void deleteOrganization_ShouldDeleteOrganization() {
        when(organizationRepository.findById("org1")).thenReturn(Optional.of(organization));

        organizationService.deleteOrganization("org1");

        verify(organizationRepository).delete(organization);
    }
}