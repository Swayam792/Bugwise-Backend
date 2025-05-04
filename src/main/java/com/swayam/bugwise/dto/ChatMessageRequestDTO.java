package com.swayam.bugwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatMessageRequestDTO {
    @NotBlank(message = "Content is required")
    private String content;

    private String bugId;

    @NotNull(message = "Sender is required")
    private String sender;

    private String tempId;
}