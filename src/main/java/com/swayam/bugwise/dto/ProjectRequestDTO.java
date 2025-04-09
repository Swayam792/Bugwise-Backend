package com.swayam.bugwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class ProjectRequestDTO {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Organization ID is required")
    private String organizationId;

    @NotNull(message = "Project Manager ID is required")
    private String projectManagerId;

    private Set<String> assignedUserIds;
}
