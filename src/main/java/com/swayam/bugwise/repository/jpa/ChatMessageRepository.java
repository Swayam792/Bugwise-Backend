package com.swayam.bugwise.repository.jpa;

import com.swayam.bugwise.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findByBugIdOrderByCreatedAtDesc(String bugId);

    @Query(value = "SELECT cm FROM ChatMessage cm " +
            "WHERE cm.bug.id = :bugId " +
            "AND cm.sender.id = :userId " +
            "AND :userId NOT IN (SELECT r FROM cm.readBy r)", nativeQuery = true)
    List<ChatMessage> findUnreadMessagesByBugAndSender(@Param("bugId") String bugId, @Param("userId") String userId);

}
