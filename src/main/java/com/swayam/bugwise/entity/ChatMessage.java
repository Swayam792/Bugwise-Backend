package com.swayam.bugwise.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.swayam.bugwise.enums.MessageType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chat_messages")
@Builder
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id"
)
public class ChatMessage implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String bugId;

    @Column(nullable = false)
    private String sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type = MessageType.CHAT;

    @ElementCollection
    @CollectionTable(name = "message_read_status",
            joinColumns = @JoinColumn(name = "message_id"))
    @JsonIgnore
    private Set<String> readBy = new HashSet<>();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ElementCollection
    @CollectionTable(name = "message_action_items",
            joinColumns = @JoinColumn(name = "message_id"))
    private Set<String> actionItems = new HashSet<>();

    @Column
    private Boolean hasActionItems = false;

    public void markAsRead(String userId) {
        if (readBy == null) {
            readBy = new HashSet<>();
        }
        readBy.add(userId);
    }

    public static ChatMessage createMessage(String content, String bugId, String senderId, MessageType type) {
        ChatMessage message = new ChatMessage();
        message.setContent(content);
        message.setBugId(bugId);
        message.setSender(senderId);
        message.setType(type);
        message.setCreatedAt(LocalDateTime.now());
        message.setReadBy(new HashSet<>());
        return message;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", bug=" + bugId +
                ", sender=" + sender +
                ", type=" + type +
                ", createdAt=" + createdAt +
                '}';
    }
}