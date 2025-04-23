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
import com.swayam.bugwise.utils.DTOConverter;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public Project createProject(ProjectRequestDTO request) {
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        User projectManager = userRepository.findById(request.getProjectManagerId())
                .orElseThrow(() -> new NoSuchElementException("Project Manager not found"));

        validateProjectManager(projectManager);

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOrganization(organization);
        project.setProjectManager(projectManager);
        project.setStatus(ProjectStatus.ACTIVE);

        if (request.getAssignedUserIds() != null && !request.getAssignedUserIds().isEmpty()) {
            Set<User> assignedUsers = userRepository.findAllByIdIn(request.getAssignedUserIds());
            project.setAssignedUsers(assignedUsers);
        }

        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Cacheable(value = "projects", key = "#projectId")
    public ProjectDTO getProject(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found"));
        ProjectDTO dto = DTOConverter.convertToDTO(project, ProjectDTO.class);
        dto.setOrganizationId(project.getOrganization().getId());
        dto.setAssignedUsers(project.getAssignedUsers().stream()
                .map(user -> DTOConverter.convertToDTO(user, UserDetailsDTO.class))
                .collect(Collectors.toSet()));
        return dto;
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public List<Project> getOrganizationProjects(String organizationId) {
        return projectRepository.findByOrganizationId(organizationId);
    }

    private void validateProjectManager(User user) {
        if (user.getRole() != UserRole.PROJECT_MANAGER) {
            throw new ValidationException(Map.of("error",
                    "Assigned user must have PROJECT_MANAGER role"));
        }
    }

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public ProjectDTO assignUsersToProject(String projectId, Set<String> userIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found"));

        Set<User> users = userRepository.findAllByIdIn(userIds);
        project.getAssignedUsers().addAll(users);

        Project updatedProject = projectRepository.save(project);
        ProjectDTO dto = DTOConverter.convertToDTO(updatedProject, ProjectDTO.class);
        dto.setAssignedUsers(updatedProject.getAssignedUsers().stream()
                .map(user -> DTOConverter.convertToDTO(user, UserDetailsDTO.class))
                .collect(Collectors.toSet()));
        return dto;
    }

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public ProjectDTO updateProject(String projectId, ProjectUpdateDTO request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found"));

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        // Update project manager if changed
        if (!project.getProjectManager().getId().equals(request.getProjectManagerId())) {
            User newProjectManager = userRepository.findById(request.getProjectManagerId())
                    .orElseThrow(() -> new NoSuchElementException("Project Manager not found"));
            validateProjectManager(newProjectManager);
            project.setProjectManager(newProjectManager);
        }

        if (request.getAssignedUserIds() != null) {
            Set<User> assignedUsers = userRepository.findAllByIdIn(request.getAssignedUserIds());
            project.setAssignedUsers(assignedUsers);
        }

        Project updatedProject = projectRepository.save(project);

        ProjectDTO dto = DTOConverter.convertToDTO(updatedProject, ProjectDTO.class);
        dto.setAssignedUsers(updatedProject.getAssignedUsers().stream()
                .map(user -> DTOConverter.convertToDTO(user, UserDetailsDTO.class))
                .collect(Collectors.toSet()));
        return dto;
    }

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public ProjectDTO removeUsersFromProject(String projectId, Set<String> userIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found"));

        project.getAssignedUsers().removeIf(user -> userIds.contains(user.getId()));

        Project updatedProject = projectRepository.save(project);
        ProjectDTO dto = DTOConverter.convertToDTO(updatedProject, ProjectDTO.class);
        dto.setAssignedUsers(updatedProject.getAssignedUsers().stream()
                .map(user -> DTOConverter.convertToDTO(user, UserDetailsDTO.class))
                .collect(Collectors.toSet()));
        return dto;
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public Page<Project> searchProjectsInOrganization(
            String organizationId,
            String searchTerm,
            Pageable pageable) {
        return projectRepository.searchProjectsInOrganization(organizationId, searchTerm, pageable);
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public long countProjectsByOrganization(String organizationId) {
        return projectRepository.countProjectsByOrganization(organizationId);
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public List<Project> findByProjectManager(String projectManagerId) {
        return projectRepository.findByProjectManagerId(projectManagerId);
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public List<ProjectDTO> getProjectsForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == UserRole.ADMIN) {
            return projectRepository.findAll().stream()
                    .map(project -> {
                        ProjectDTO dto = new ProjectDTO(
                                project.getId(),
                                project.getName(),
                                project.getDescription(),
                                project.getOrganization().getId(),
                                project.getStatus(),
                                DTOConverter.convertToDTO(project.getProjectManager(), UserDetailsDTO.class),
                                project.getAssignedUsers().stream().map(eachUser -> DTOConverter.convertToDTO(eachUser, UserDetailsDTO.class)).collect(Collectors.toSet())
                        );
                        dto.setAssignedUsers(project.getAssignedUsers().stream()
                                .map(u -> DTOConverter.convertToDTO(u, UserDetailsDTO.class))
                                .collect(Collectors.toSet()));
                        return dto;
                    }).collect(Collectors.toList());
        } else {
            return user.getAssignedProjects().stream()
                    .map(project -> {
                        ProjectDTO dto = new ProjectDTO(
                                project.getId(),
                                project.getName(),
                                project.getDescription(),
                                project.getOrganization().getId(),
                                project.getStatus(),
                                DTOConverter.convertToDTO(project.getProjectManager(), UserDetailsDTO.class),
                                project.getAssignedUsers().stream().map(eachUser -> DTOConverter.convertToDTO(eachUser, UserDetailsDTO.class)).collect(Collectors.toSet())
                        );
                        dto.setAssignedUsers(project.getAssignedUsers().stream()
                                .map(u -> DTOConverter.convertToDTO(u, UserDetailsDTO.class))
                                .collect(Collectors.toSet()));
                        return dto;
                    })
                    .collect(Collectors.toList());
        }
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public List<ProjectStatsDTO> getProjectStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Project> projects;

        if (user.getRole() == UserRole.ADMIN) {
            Set<String> organizationIds = user.getOrganizations().stream()
                    .map(Organization::getId)
                    .collect(Collectors.toSet());
            projects = projectRepository.findByOrganizationIdIn(organizationIds);
        } else {
            if (user.getRole() == UserRole.PROJECT_MANAGER) {
                projects = projectRepository.findByProjectManagerId(user.getId());
            } else {
                projects = projectRepository.findByAssignedBugsDeveloperId(user.getId());
            }
        }

        return projects.stream().map(project -> {
            Map<BugStatus, Long> statusCounts = project.getBugs().stream()
                    .collect(Collectors.groupingBy(Bug::getStatus, Collectors.counting()));
 
            Map<String, Integer> allStatusCounts = new HashMap<>();
             
            for (BugStatus status : BugStatus.values()) {
                allStatusCounts.put(status.name(), statusCounts.getOrDefault(status, 0L).intValue());
            }
            
            return new ProjectStatsDTO(
                    project.getName(),
                    project.getOrganization().getId(),
                    allStatusCounts
            );
        }).collect(Collectors.toList());
    }

}
