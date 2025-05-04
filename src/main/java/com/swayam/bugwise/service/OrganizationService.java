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
import com.swayam.bugwise.utils.DTOConverter;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public void createOrganization(OrganizationRequestDTO request, User admin) {
        if (organizationRepository.existsByName(request.getName())) {
            throw new ValidationException(Map.of("name", "Organization name already exists"));
        }

        Organization organization = new Organization();
        organization.setName(request.getName());
        organization.setDescription(request.getDescription());
        organization.setAdmin(admin);
        organization.setUsers(new HashSet<>());
        organization.getUsers().add(admin);

        organizationRepository.save(organization);
    }

    @Cacheable(value = "organizations", key = "#organizationId")
    public OrganizationDTO getOrganization(String organizationId, User user) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        if (!organization.isAdmin(user)) {
            throw new UnauthorizedAccessException("You are not authorized to access this organization");
        }

        return DTOConverter.convertToDTO(organization, OrganizationDTO.class);
    }

    public List<OrganizationDTO> getOrganizationsForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == UserRole.ADMIN) {
            return organizationRepository.findAll().stream()
                    .map(org -> new OrganizationDTO(org.getId(), org.getName(), org.getDescription()))
                    .collect(Collectors.toList());
        } else {
            return user.getOrganizations().stream()
                    .map(org -> new OrganizationDTO(org.getId(), org.getName(), org.getDescription()))
                    .collect(Collectors.toList());
        }
    }

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public Organization createOrganization(OrganizationDTO organizationDTO) {
        if (organizationRepository.existsByName(organizationDTO.getName())) {
            throw new ValidationException(Map.of("name", "Organization name already exists"));
        }

        Organization organization = new Organization();
        organization.setName(organizationDTO.getName());
        organization.setDescription(organizationDTO.getDescription());

        return organizationRepository.save(organization);
    }

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public void updateOrganization(String id, OrganizationRequestDTO organizationDTO) {
        if (organizationRepository.existsByName(organizationDTO.getName())) {
            throw new ValidationException(Map.of("name", "Organization name already exists"));
        }

        Organization organization = new Organization();
        organization.setName(organizationDTO.getName());
        organization.setDescription(organizationDTO.getDescription());

        organizationRepository.save(organization);
    }

    public void addUsersToOrganization(String organizationId, Set<String> userIds) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        Set<User> usersToAdd = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toSet());

        if (usersToAdd.size() != userIds.size()) {
            throw new NoSuchElementException("One or more users not found");
        }

        organization.getUsers().addAll(usersToAdd);
        organizationRepository.save(organization);
    }

    public Set<UserDetailsDTO> getUsersInOrganization(String organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        return organization.getUsers().stream()
                .map(user -> DTOConverter.convertToDTO(user, UserDetailsDTO.class))
                .collect(Collectors.toSet());
    }

    public boolean isAdmin(User user, String organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));
        return organization.isAdmin(user);
    }

    @Transactional
    public void removeUserFromOrganization(String organizationId, String userId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found with id: " + organizationId));

        if (organization.getAdmin() != null && organization.getAdmin().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Cannot remove admin user from organization");
        }

        organization.getUsers().removeIf(user -> user.getId().equals(userId));
        organizationRepository.save(organization);
    }

    @Transactional
    public void deleteOrganization(String organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found with id: " + organizationId));

        organizationRepository.delete(organization);
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getOrganizationProjects(String organizationId) {
        return projectRepository.findByOrganizationId(organizationId).stream()
                .map(project -> {
                    ProjectDTO dto = new ProjectDTO();
                    dto.setId(project.getId());
                    dto.setName(project.getName());
                    dto.setDescription(project.getDescription());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrganizationStatsDTO> getOrganizationStats() {
        List<Organization> organizations = organizationRepository.findAll();

        return organizations.stream().map(org -> {
            OrganizationStatsDTO stats = new OrganizationStatsDTO();
            stats.setOrganizationId(org.getId());
            stats.setOrganizationName(org.getName());
            stats.setProjectCount(org.getProjects().size());
            stats.setMemberCount(org.getUsers().size());
            return stats;
        }).collect(Collectors.toList());
    }
}