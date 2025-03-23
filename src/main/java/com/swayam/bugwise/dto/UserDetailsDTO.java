package com.swayam.bugwise.dto;

import com.swayam.bugwise.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDTO {
    private String userId;
    private UserRole role;
    private List<String> organizationIds;
    private List<String> projectIds;
}
