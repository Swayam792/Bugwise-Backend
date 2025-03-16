package com.swayam.bugwise.dto;

import com.swayam.bugwise.enums.MessageType;
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
public class CommentDTO implements Serializable {
    private String id;
    private String content;
    private UserDTO sender;
    private MessageType type;
    private Set<String> readBy = new HashSet<>();
    private LocalDateTime createdAt;
}
