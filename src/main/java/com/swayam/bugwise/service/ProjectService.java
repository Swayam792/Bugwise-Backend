package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.ProjectDTO;
import com.swayam.bugwise.dto.ProjectRequestDTO;
import com.swayam.bugwise.dto.ProjectStatsDTO;
import com.swayam.bugwise.dto.UserDTO;
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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Cacheable(value = "projects", key = "#projectId")
    public ProjectDTO getProject(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found"));
        ProjectDTO dto = DTOConverter.convertToDTO(project, ProjectDTO.class);
        dto.setAssignedUsers(project.getAssignedUsers().stream()
                .map(user -> DTOConverter.convertToDTO(user, UserDTO.class))
                .collect(Collectors.toSet()));
        return dto;
    }

    public List<Project> getOrganizationProjects(String organizationId) {
        return projectRepository.findByOrganizationId(organizationId);
    }

    private void validateProjectManager(User user) {
        if (user.getRole() != UserRole.PROJECT_MANAGER) {
            throw new ValidationException(Map.of("error",
                    "Assigned user must have PROJECT_MANAGER role"));
        }
    }

    public ProjectDTO assignUsersToProject(String projectId, Set<String> userIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found"));

        Set<User> users = userRepository.findAllByIdIn(userIds);
        project.getAssignedUsers().addAll(users);

        Project updatedProject = projectRepository.save(project);
        ProjectDTO dto = DTOConverter.convertToDTO(updatedProject, ProjectDTO.class);
        dto.setAssignedUsers(updatedProject.getAssignedUsers().stream()
                .map(user -> DTOConverter.convertToDTO(user, UserDTO.class))
                .collect(Collectors.toSet()));
        return dto;
    }

    public ProjectDTO removeUsersFromProject(String projectId, Set<String> userIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found"));

        project.getAssignedUsers().removeIf(user -> userIds.contains(user.getId()));

        Project updatedProject = projectRepository.save(project);
        ProjectDTO dto = DTOConverter.convertToDTO(updatedProject, ProjectDTO.class);
        dto.setAssignedUsers(updatedProject.getAssignedUsers().stream()
                .map(user -> DTOConverter.convertToDTO(user, UserDTO.class))
                .collect(Collectors.toSet()));
        return dto;
    }

    public Page<Project> searchProjectsInOrganization(
            String organizationId,
            String searchTerm,
            Pageable pageable) {
        return projectRepository.searchProjectsInOrganization(organizationId, searchTerm, pageable);
    }

    public long countProjectsByOrganization(String organizationId) {
        return projectRepository.countProjectsByOrganization(organizationId);
    }

    public List<Project> findByProjectManager(String projectManagerId) {
        return projectRepository.findByProjectManagerId(projectManagerId);
    }

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
                                DTOConverter.convertToDTO(project.getProjectManager(), UserDTO.class),
                                project.getAssignedUsers().stream().map(eachUser -> DTOConverter.convertToDTO(eachUser, UserDTO.class)).collect(Collectors.toSet())
                        );
                        dto.setAssignedUsers(project.getAssignedUsers().stream()
                                .map(u -> DTOConverter.convertToDTO(u, UserDTO.class))
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
                                DTOConverter.convertToDTO(project.getProjectManager(), UserDTO.class),
                                project.getAssignedUsers().stream().map(eachUser -> DTOConverter.convertToDTO(eachUser, UserDTO.class)).collect(Collectors.toSet())
                        );
                        dto.setAssignedUsers(project.getAssignedUsers().stream()
                                .map(u -> DTOConverter.convertToDTO(u, UserDTO.class))
                                .collect(Collectors.toSet()));
                        return dto;
                    })
                    .collect(Collectors.toList());
        }
    }

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

            return new ProjectStatsDTO(
                    project.getName(),
                    statusCounts.getOrDefault(BugStatus.OPEN, 0L).intValue(),
                    statusCounts.getOrDefault(BugStatus.IN_PROGRESS, 0L).intValue(),
                    statusCounts.getOrDefault(BugStatus.RESOLVED, 0L).intValue()
            );
        }).collect(Collectors.toList());
    }

}
