package com.swayam.bugwise.dto;

import com.swayam.bugwise.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class ProjectUpdateDTO {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    private ProjectStatus status;

    @NotBlank(message = "Project Manager ID is required")
    private String projectManagerId;

    private Set<String> assignedUserIds;
}