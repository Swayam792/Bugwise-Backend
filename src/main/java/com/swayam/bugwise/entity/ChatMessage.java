package com.swayam.bugwise.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class ChatMessage implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bug_id", nullable = false)
    @JsonBackReference
    private Bug bug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @JsonBackReference
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    @JsonBackReference
    private User recipient;

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

    public void markAsRead(String userId) {
        if (readBy == null) {
            readBy = new HashSet<>();
        }
        readBy.add(userId);
    }

    public static ChatMessage createMessage(String content, Bug bug, User sender, MessageType type) {
        ChatMessage message = new ChatMessage();
        message.setContent(content);
        message.setBug(bug);
        message.setSender(sender);
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
                ", bug=" + (bug != null ? bug.getId() : null) +
                ", sender=" + (sender != null ? sender.getId() : null) +
                ", recipient=" + (recipient != null ? recipient.getId() : null) +
                ", type=" + type +
                ", createdAt=" + createdAt +
                '}';
    }
}