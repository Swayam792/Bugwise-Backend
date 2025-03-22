package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.ProjectDTO;
import com.swayam.bugwise.dto.ProjectRequestDTO;
import com.swayam.bugwise.entity.Organization;
import com.swayam.bugwise.entity.Project;
import com.swayam.bugwise.entity.User;
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

        return projectRepository.save(project);
    }

    @Cacheable(value = "projects", key = "#projectId")
    public ProjectDTO getProject(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found"));
        return DTOConverter.convertToDTO(project, ProjectDTO.class);
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

    public List<ProjectDTO> getAllProjectsForAdmin(String adminUsername) {
        String adminId = userRepository.getIdByUserName(adminUsername);

        List<Organization> organizations = organizationRepository.findByAdminId(adminId);

        return organizations.stream()
                .flatMap(org -> projectRepository.findByOrganizationId(org.getId()).stream())
                .map((project) -> DTOConverter.convertToDTO(project, ProjectDTO.class))
                .collect(Collectors.toList());
    }
}
