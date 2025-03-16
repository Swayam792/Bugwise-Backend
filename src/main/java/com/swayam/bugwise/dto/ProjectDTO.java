package com.swayam.bugwise.dto;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.swayam.bugwise.entity.Organization;
import com.swayam.bugwise.entity.User;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private String name;
    private String description;
    private UserDTO projectManager;
}
