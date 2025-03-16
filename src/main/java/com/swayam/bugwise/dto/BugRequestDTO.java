package com.swayam.bugwise.dto;

import com.swayam.bugwise.enums.BugSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
}
