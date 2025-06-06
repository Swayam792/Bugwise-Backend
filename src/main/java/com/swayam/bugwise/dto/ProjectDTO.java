package com.swayam.bugwise.dto;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.swayam.bugwise.entity.Organization;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.ProjectStatus;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private String id;
    private String name;
    private String description;
    private String organizationId;
    private ProjectStatus status;
    private UserDetailsDTO projectManager;
    private Set<UserDetailsDTO> assignedUsers;
}
