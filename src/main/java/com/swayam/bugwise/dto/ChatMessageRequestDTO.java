package com.swayam.bugwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatMessageRequestDTO {
    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Bug ID is required")
    private String bugId;

    private String recipientId;
}