package com.swayam.bugwise.utils;

import com.swayam.bugwise.config.RabbitMQConfig;
import com.swayam.bugwise.dto.NotificationMessageDTO;
import com.swayam.bugwise.entity.UserNotification;
import com.swayam.bugwise.repository.jpa.UserNotificationRepository;
import com.swayam.bugwise.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final UserNotificationRepository notificationRepository;
    private final WebSocketService webSocketService;

    @RabbitListener(queues = RabbitMQConfig.IN_APP_QUEUE)
    @Transactional
    public void consumeInAppNotification(NotificationMessageDTO message) {
        log.info("Processing in-app notification to {}", message.getRecipients());

        message.getRecipients().forEach(userId -> {
            try {
                UserNotification notification = new UserNotification();
                notification.setUserId(userId);
                notification.setType(message.getType());
                notification.setTitle(message.getTitle());
                notification.setContent(message.getContent());

                notification.setMetadata(convertMetadata(message.getMetadata()));

                notification.setRead(false);
                notification.setCreatedAt(LocalDateTime.now());

                UserNotification savedNotification = notificationRepository.save(notification);

                webSocketService.sendNotification(
                        userId,
                        Map.of(
                                "id", savedNotification.getId(),
                                "type", savedNotification.getType(),
                                "title", savedNotification.getTitle(),
                                "content", savedNotification.getContent(),
                                "isRead", savedNotification.isRead(),
                                "createdAt", savedNotification.getCreatedAt(),
                                "metadata", savedNotification.getMetadata() // Now safe for JSON serialization
                        )
                );

                log.debug("In-app notification processed for user {}", userId);
            } catch (Exception e) {
                log.error("Failed to process in-app notification for user {}: {}", userId, e.getMessage());
            }
        });
    }

    private Map<String, String> convertMetadata(Map<String, Object> originalMetadata) {
        if (originalMetadata == null) {
            return Collections.emptyMap();
        }

        Map<String, String> stringMetadata = new HashMap<>();
        originalMetadata.forEach((key, value) -> {
            stringMetadata.put(key, value != null ? value.toString() : null);
        });
        return stringMetadata;
    }
}