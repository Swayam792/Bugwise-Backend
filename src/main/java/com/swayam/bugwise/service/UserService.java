package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.UpdatePasswordRequestDTO;
import com.swayam.bugwise.dto.UpdateUserRequestDTO;
import com.swayam.bugwise.dto.UserDTO;
import com.swayam.bugwise.dto.UserDetailsDTO;
import com.swayam.bugwise.entity.Organization;
import com.swayam.bugwise.entity.Project;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.UserRole;
import com.swayam.bugwise.exception.UnauthorizedAccessException;
import com.swayam.bugwise.exception.ValidationException;
import com.swayam.bugwise.repository.jpa.OrganizationRepository;
import com.swayam.bugwise.repository.jpa.UserRepository;
import com.swayam.bugwise.utils.DTOConverter;

import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Lock;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public List<UserDetailsDTO> findActiveUsersByOrganizationAndRole(String organizationId, UserRole role) {
        return userRepository.findActiveUsersByOrganizationAndRole(organizationId, role)
                .stream()
                .map(user -> DTOConverter.convertToDTO(user, UserDetailsDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public List<UserDetailsDTO> findActiveUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> DTOConverter.convertToDTO(user, UserDetailsDTO.class))
                .collect(Collectors.toList());
    }

    public List<UserDetailsDTO> findDevelopers(String projectId, String organizationId) {
        List<User> userList = new ArrayList<>();
        if(projectId != null) {
            userList = userRepository.findByAssignedProjectsIdAndRole(projectId, UserRole.DEVELOPER);
        } else if(organizationId != null) {
            userList = userRepository.findByOrganizationsIdAndRole(organizationId, UserRole.DEVELOPER);
        }
        return userList.stream()
                .map(user -> DTOConverter.convertToDTO(user, UserDetailsDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public long countActiveUsersByOrganizationAndRole(String organizationId, UserRole role) {
        return userRepository.countActiveUsersByOrganizationsAndRole(organizationId, role);
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return user;
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public UserDetailsDTO getCurrentUserDetails(String email) { 
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return DTOConverter.convertToDTO(user, UserDetailsDTO.class);
    }

    public User getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return user;
    }

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public UserDetailsDTO updateUserDetails(String email, UpdateUserRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ValidationException(Map.of("email", "Email is already taken"));
            }
            user.setEmail(request.getEmail());
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        return DTOConverter.convertToDTO(userRepository.save(user), UserDetailsDTO.class);
    }

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public void updateUserPassword(String userId, UpdatePasswordRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ValidationException(Map.of("currentPassword", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public Map<UserRole, Long> getDescendantUserCount(String username) {
        User currentUser = userRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Map<UserRole, Long> descendantCount = new EnumMap<>(UserRole.class);

        if (currentUser.getRole() == UserRole.ADMIN) {
            List<Organization> administeredOrganizations = organizationRepository.findByAdmin(currentUser);
            for (Organization organization : administeredOrganizations) {
                descendantCount.put(UserRole.PROJECT_MANAGER,
                        descendantCount.getOrDefault(UserRole.PROJECT_MANAGER, 0L) +
                                userRepository.countActiveUsersByOrganizationsAndRole(organization.getId(), UserRole.PROJECT_MANAGER));
                descendantCount.put(UserRole.DEVELOPER,
                        descendantCount.getOrDefault(UserRole.DEVELOPER, 0L) +
                                userRepository.countActiveUsersByOrganizationsAndRole(organization.getId(), UserRole.DEVELOPER));
                descendantCount.put(UserRole.TESTER,
                        descendantCount.getOrDefault(UserRole.TESTER, 0L) +
                                userRepository.countActiveUsersByOrganizationsAndRole(organization.getId(), UserRole.TESTER));
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