package com.swayam.bugwise.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotification(String userId, Object payload) {
        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                payload
        );
    }
}