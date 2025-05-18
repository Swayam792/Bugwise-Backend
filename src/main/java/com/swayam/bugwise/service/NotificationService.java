package com.swayam.bugwise.service;

import com.swayam.bugwise.config.RabbitMQConfig;
import com.swayam.bugwise.dto.NotificationMessageDTO;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.entity.UserNotification;
import com.swayam.bugwise.enums.NotificationChannel;
import com.swayam.bugwise.repository.jpa.UserNotificationRepository;
import com.swayam.bugwise.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RabbitTemplate rabbitTemplate;
    private final UserNotificationRepository notificationRepository;
    private final WebSocketService webSocketService;

    @Transactional(readOnly = true)
    public List<UserNotification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<UserNotification> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsRead(userId);
    }

    public void sendNotification(NotificationMessageDTO message) {
        log.info("Sending in-app notification to {} users: {}",
                message.getRecipients().size(), message.getType());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.IN_APP_EXCHANGE,
                RabbitMQConfig.IN_APP_ROUTING_KEY,
                message
        );
    }

}