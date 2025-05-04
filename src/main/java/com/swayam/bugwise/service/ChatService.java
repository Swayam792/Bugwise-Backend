package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.ChatMessageRequestDTO;
import com.swayam.bugwise.dto.ParticipantsUpdateDTO;
import com.swayam.bugwise.entity.ChatMessage;
import com.swayam.bugwise.enums.MessageType;
import com.swayam.bugwise.enums.ParticipantAction;
import com.swayam.bugwise.exception.ResourceNotFoundException;
import com.swayam.bugwise.repository.jpa.BugRepository;
import com.swayam.bugwise.repository.jpa.ChatMessageRepository;
import com.swayam.bugwise.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final BugRepository bugRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserRepository userRepository;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<String>> activeBugParticipants = new ConcurrentHashMap<>();

    private static final String CHAT_MESSAGES_TOPIC = "chat-messages";
    private static final String SYSTEM_MESSAGES_TOPIC = "system-messages";
    private static final String TYPING_NOTIFICATIONS_TOPIC = "typing-notifications";
    private static final String READ_RECEIPTS_TOPIC = "read-receipts";
    private static final String PARTICIPANTS_UPDATES_TOPIC = "participants-updates";

    public ChatMessage sendMessage(ChatMessageRequestDTO request) {
        try {
            if (!bugRepository.existsById(request.getBugId())) {
                throw new NoSuchElementException("Bug not found with id: " + request.getBugId());
            }

            userRepository.findByEmail(request.getSender()).orElseThrow(
                    () -> new NoSuchElementException("Sender not found")
            );

            ChatMessage message = ChatMessage.createMessage(
                    request.getContent(),
                    request.getBugId(),
                    request.getSender(),
                    MessageType.CHAT
            );

            ChatMessage savedMessage = chatMessageRepository.save(message);

            // Only chat messages go to the chat-messages topic
            kafkaTemplate.send(CHAT_MESSAGES_TOPIC, savedMessage);

            return savedMessage;
        } catch (Exception e) {
            log.error("Error sending message: ", e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    @KafkaListener(topics = CHAT_MESSAGES_TOPIC, groupId = "bugwise-chat-group")
    public void listenChatMessages(@Payload ChatMessage message, @Headers MessageHeaders headers) {
        try {
            if (message.getBugId() != null) {
                messagingTemplate.convertAndSend("/topic/bug." + message.getBugId(), message);
            } else {
                log.warn("Received ChatMessage without BugId: {}", message.getId());
            }
        } catch (Exception e) {
            log.error("Error broadcasting chat message: ", e);
        }
    }

    @KafkaListener(topics = SYSTEM_MESSAGES_TOPIC, groupId = "bugwise-chat-group")
    public void listenSystemMessages(@Payload ChatMessage message, @Headers MessageHeaders headers) {
        try {
            if (message.getBugId() != null) {
                messagingTemplate.convertAndSend("/topic/bug." + message.getBugId(), message);
            } else {
                log.warn("Received System Message without BugId: {}", message.getId());
            }
        } catch (Exception e) {
            log.error("Error broadcasting system message: ", e);
        }
    }

    public void handleUserJoin(String bugId, String email) {
        try {
            CopyOnWriteArrayList<String> participants = activeBugParticipants.computeIfAbsent(
                    bugId, k -> new CopyOnWriteArrayList<>()
            );

            if (participants.addIfAbsent(email)) {
                if (!bugRepository.existsById(bugId)) {
                    throw new ResourceNotFoundException("Bug not found with id: " + bugId);
                }

                ChatMessage joinMessage = ChatMessage.builder()
                        .id(UUID.randomUUID().toString())
                        .bugId(bugId)
                        .sender(email)
                        .type(MessageType.JOIN)
                        .createdAt(LocalDateTime.now())
                        .build();

                // System messages (JOIN/LEAVE) go to system-messages topic
                kafkaTemplate.send(SYSTEM_MESSAGES_TOPIC, joinMessage);

                ParticipantsUpdateDTO updateDTO = new ParticipantsUpdateDTO(
                        bugId,
                        new ArrayList<>(participants),
                        email,
                        ParticipantAction.JOIN
                );

                kafkaTemplate.send(PARTICIPANTS_UPDATES_TOPIC, updateDTO);
            }
        } catch (Exception e) {
            log.error("Error handling user join for bug {}: ", bugId, e);
            throw new RuntimeException("Failed to handle user join", e);
        }
    }

    @KafkaListener(topics = PARTICIPANTS_UPDATES_TOPIC, groupId = "bugwise-chat-group")
    public void listenParticipantsUpdates(ParticipantsUpdateDTO updateDTO) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/bug." + updateDTO.getBugId() + ".participants",
                    updateDTO.getUsername()
            );
        } catch (Exception e) {
            log.error("Error broadcasting participants update: ", e);
        }
    }

    public void handleUserLeave(String bugId, String email) {
        try {
            CopyOnWriteArrayList<String> participants = activeBugParticipants.get(bugId);
            if (participants != null && participants.remove(email)) {

                if (!bugRepository.existsById(bugId)) {
                    throw new ResourceNotFoundException("Bug not found with id: " + bugId);
                }

                ChatMessage leaveMessage = ChatMessage.builder()
                        .id(UUID.randomUUID().toString())
                        .bugId(bugId)
                        .sender(email)
                        .type(MessageType.LEAVE)
                        .createdAt(LocalDateTime.now())
                        .build();

                // System messages (JOIN/LEAVE) go to system-messages topic
                kafkaTemplate.send(SYSTEM_MESSAGES_TOPIC, leaveMessage);

                ParticipantsUpdateDTO updateDTO = new ParticipantsUpdateDTO(
                        bugId,
                        new ArrayList<>(participants),
                        email,
                        ParticipantAction.LEAVE
                );

                kafkaTemplate.send(PARTICIPANTS_UPDATES_TOPIC, updateDTO);

                if (participants.isEmpty()) {
                    activeBugParticipants.remove(bugId);
                }
            }
        } catch (Exception e) {
            log.error("Error handling user leave for bug {}: ", bugId, e);
            throw new RuntimeException("Failed to handle user leave", e);
        }
    }

    public void sendTypingNotification(String bugId, String email) {
        try {
            if (!activeBugParticipants.containsKey(bugId) ||
                    !activeBugParticipants.get(bugId).contains(email)) {
                return;
            }

            ChatMessage typingMessage = ChatMessage.builder()
                    .bugId(bugId)
                    .sender(email)
                    .type(MessageType.TYPING)
                    .createdAt(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TYPING_NOTIFICATIONS_TOPIC, typingMessage);
        } catch (Exception e) {
            log.error("Error sending typing notification for bug {}: ", bugId, e);
            throw new RuntimeException("Failed to send typing notification", e);
        }
    }

    @KafkaListener(topics = TYPING_NOTIFICATIONS_TOPIC, groupId = "bugwise-chat-group")
    public void listenTypingNotifications(ChatMessage typingMessage) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/bug." + typingMessage.getBugId() + "/typing",
                    typingMessage
            );
        } catch (Exception e) {
            log.error("Error broadcasting typing notification: ", e);
        }
    }

    @Transactional
    public void markMessagesAsRead(String bugId, String userId) {
        try {
            List<ChatMessage> unreadMessages = chatMessageRepository
                    .findUnreadMessagesByBugAndSender(bugId, userId);

            if (!unreadMessages.isEmpty()) {
                unreadMessages.forEach(message -> message.markAsRead(userId));
                chatMessageRepository.saveAll(unreadMessages);

                kafkaTemplate.send(READ_RECEIPTS_TOPIC, unreadMessages);
            }
        } catch (Exception e) {
            log.error("Error marking messages as read for bug {} and user {}: ", bugId, userId, e);
            throw new RuntimeException("Failed to mark messages as read", e);
        }
    }

    @KafkaListener(topics = READ_RECEIPTS_TOPIC, groupId = "bugwise-chat-group")
    public void listenReadReceipts(List<ChatMessage> readMessages) {
        try {
            readMessages.forEach(message -> {
                messagingTemplate.convertAndSend(
                        "/topic/bug." + message.getBugId() + "/read-receipts",
                        message.getId()
                );
            });
        } catch (Exception e) {
            log.error("Error broadcasting read receipts: ", e);
        }
    }

    public List<ChatMessage> getMessagesForBug(String bugId) {
        return chatMessageRepository.findByBugIdOrderByCreatedAtDesc(bugId);
    }
}