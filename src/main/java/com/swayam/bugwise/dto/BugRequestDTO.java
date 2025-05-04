package com.swayam.bugwise.dto;

import com.swayam.bugwise.enums.BugSeverity;
import com.swayam.bugwise.enums.BugType;
import com.swayam.bugwise.enums.DeveloperType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class BugRequestDTO {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Severity is required")
    private BugSeverity severity;

    @NotNull(message = "Project ID is required")
    private String projectId;

    private BugType bugType;
    private Integer expectedTimeHours;
    private Integer actualTimeHours;
}
