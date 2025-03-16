package com.swayam.bugwise.repository;

import com.swayam.bugwise.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByBugIdOrderByCreatedAtDesc(UUID bugId);

    List<ChatMessage> findUnreadMessagesByBugAndUser(UUID bugId, UUID userId);
}
