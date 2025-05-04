package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.ChatMessageRequestDTO;
import com.swayam.bugwise.entity.ChatMessage;
import com.swayam.bugwise.repository.jpa.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatParticipantService participantService;

    @Transactional
    public ChatMessage saveMessage(ChatMessageRequestDTO messageDTO) {
        log.info("Saving message: {}", messageDTO);

        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID().toString());
        message.setBugId(messageDTO.getBugId());
        message.setContent(messageDTO.getContent());
        message.setSender(messageDTO.getSender());
        message.setCreatedAt(LocalDateTime.now());

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Include the tempId in the response to match with pending messages on frontend
        Map<String, Object> response = Map.of(
                "id", savedMessage.getId(),
                "bugId", savedMessage.getBugId(),
                "content", savedMessage.getContent(),
                "sender", savedMessage.getSender(),
                "createdAt", savedMessage.getCreatedAt(),
                "tempId", messageDTO.getTempId()
        );

        // Send to the topic that clients are subscribed to
        messagingTemplate.convertAndSend("/topic/bug." + messageDTO.getBugId(), response);

        return savedMessage;
    }

    public List<ChatMessage> getMessagesByBugId(String bugId) {
        return chatMessageRepository.findByBugIdOrderByCreatedAtAsc(bugId);
    }

    public void sendTypingNotification(String bugId, String username) {
        log.info("User {} is typing in bug {}", username, bugId);
        Map<String, Object> notification = Map.of(
                "username", username,
                "timestamp", LocalDateTime.now()
        );
        messagingTemplate.convertAndSend("/topic/bug." + bugId + ".typing", notification);
    }

//    public void sendJoinNotification(String bugId, String username) {
//        log.info("User {} joined bug chat {}", username, bugId);
//        Map<String, Object> notification = Map.of(
//                "username", username,
//                "timestamp", LocalDateTime.now()
//        );
//        messagingTemplate.convertAndSend("/topic/bug." + bugId + ".join", notification);
//    }
//
//    public void sendLeaveNotification(String bugId, String username) {
//        log.info("User {} left bug chat {}", username, bugId);
//        Map<String, Object> notification = Map.of(
//                "username", username,
//                "timestamp", LocalDateTime.now()
//        );
//        messagingTemplate.convertAndSend("/topic/bug." + bugId + ".leave", notification);
//    }

    public void markMessagesAsRead(String bugId, String username) {
        log.info("User {} marked messages as read in bug {}", username, bugId);
        Map<String, Object> notification = Map.of(
                "username", username,
                "timestamp", LocalDateTime.now()
        );
        messagingTemplate.convertAndSend("/topic/bug." + bugId + ".read", notification);
    }

    public void sendCurrentParticipants(String bugId, String username) {
        Set<String> participants = participantService.getCurrentParticipantsForBug(bugId);
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/bug." + bugId + ".participants",
                participants
        );
    }

    public void sendJoinNotification(String bugId, String username) {
        participantService.addParticipant(bugId, username);
        messagingTemplate.convertAndSend(
                "/topic/bug." + bugId + ".join",
                Map.of(
                        "username", username,
                        "participants", participantService.getCurrentParticipantsForBug(bugId)
                )
        );
    }

    public void sendLeaveNotification(String bugId, String username) {
        participantService.removeParticipant(bugId, username);
        messagingTemplate.convertAndSend(
                "/topic/bug." + bugId + ".leave",
                Map.of(
                        "username", username,
                        "participants", participantService.getCurrentParticipantsForBug(bugId)
                )
        );
    }
}