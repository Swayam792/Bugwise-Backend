package com.swayam.bugwise.dto;

import com.swayam.bugwise.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String email;
    private UserRole role;
}