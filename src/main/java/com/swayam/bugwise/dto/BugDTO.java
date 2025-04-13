package com.swayam.bugwise.dto;

import com.swayam.bugwise.entity.ChatMessage;
import com.swayam.bugwise.entity.Comment;
import com.swayam.bugwise.entity.Project;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.BugSeverity;
import com.swayam.bugwise.enums.BugStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BugDTO implements Serializable {
    private String id;
    private String title;
    private String description;
    private BugStatus status;
    private BugSeverity severity;
    private ProjectDTO project;
    private UserDetailsDTO assignedDeveloper;
    private UserDetailsDTO reportedBy;
    private String organizationId;
    private String organizationName;
    private String projectManagerId;
    private String projectManagerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
