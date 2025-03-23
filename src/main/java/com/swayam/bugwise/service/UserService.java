package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.UserDetailsDTO;
import com.swayam.bugwise.entity.Organization;
import com.swayam.bugwise.entity.Project;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.UserRole;
import com.swayam.bugwise.exception.UnauthorizedAccessException;
import com.swayam.bugwise.repository.jpa.OrganizationRepository;
import com.swayam.bugwise.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public List<User> findActiveUsersByOrganizationAndRole(String organizationId, UserRole role) {
        return userRepository.findActiveUsersByOrganizationAndRole(organizationId, role);
    }

    public long countActiveUsersByOrganizationAndRole(String organizationId, UserRole role) {
        return userRepository.countActiveUsersByOrganizationsAndRole(organizationId, role.name());
    }

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }

    public UserDetailsDTO getCurrentUserDetails(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userId = user.getId();
        UserRole role = user.getRole();
        List<String> organizationIds = user.getOrganizations().stream()
                .map(Organization::getId)
                .collect(Collectors.toList());

        List<String> projectIds = user.getManagedProjects().stream()
                .map(Project::getId)
                .collect(Collectors.toList());

        return new UserDetailsDTO(userId, role, organizationIds, projectIds);
    }

    public Map<UserRole, Long> getDescendantUserCount(String username) {
        User currentUser = getUserByUsername(username);
        Map<UserRole, Long> descendantCount = new EnumMap<>(UserRole.class);

        if (currentUser.getRole() == UserRole.ADMIN) {
            List<Organization> administeredOrganizations = organizationRepository.findByAdmin(currentUser);
            for (Organization organization : administeredOrganizations) {
                descendantCount.put(UserRole.PROJECT_MANAGER,
                        descendantCount.getOrDefault(UserRole.PROJECT_MANAGER, 0L) +
                                userRepository.countActiveUsersByOrganizationsAndRole(organization.getId(), UserRole.PROJECT_MANAGER.name()));
                descendantCount.put(UserRole.DEVELOPER,
                        descendantCount.getOrDefault(UserRole.DEVELOPER, 0L) +
                                userRepository.countActiveUsersByOrganizationsAndRole(organization.getId(), UserRole.DEVELOPER.name()));
                descendantCount.put(UserRole.TESTER,
                        descendantCount.getOrDefault(UserRole.TESTER, 0L) +
                                userRepository.countActiveUsersByOrganizationsAndRole(organization.getId(), UserRole.TESTER.name()));
            }
        } else if (currentUser.getRole() == UserRole.PROJECT_MANAGER) {
            Set<Project> managedProjects = currentUser.getManagedProjects();
            Set<String> projectIds = managedProjects.stream().map(Project::getId).collect(Collectors.toSet());

            descendantCount.put(UserRole.DEVELOPER,
                    userRepository.countActiveUsersByProjectsAndRole(projectIds, UserRole.DEVELOPER));
            descendantCount.put(UserRole.TESTER,
                    userRepository.countActiveUsersByProjectsAndRole(projectIds, UserRole.TESTER));
        } else {
            throw new UnauthorizedAccessException("You do not have permission to view descendant user counts.");
        }

        return descendantCount;
    }
}