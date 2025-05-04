package com.swayam.bugwise.controller;

import com.swayam.bugwise.dto.ChatMessageRequestDTO;
import com.swayam.bugwise.entity.ChatMessage;
import com.swayam.bugwise.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @MessageMapping("/bug.chat.send.{bugId}")
    public void sendMessage(
            @DestinationVariable String bugId,
            @Valid @Payload ChatMessageRequestDTO chatMessage,
            Authentication authentication) {
        chatMessage.setBugId(bugId);
        chatMessage.setSender(authentication.getName());

        chatService.saveMessage(chatMessage);
    }

    @MessageMapping("/bug.chat.typing.{bugId}")
    public void sendTypingNotification(
            @DestinationVariable String bugId,
            Authentication authentication) {
        chatService.sendTypingNotification(bugId, authentication.getName());
    }

    @MessageMapping("/bug.chat.join.{bugId}")
    public void joinBugChat(
            @DestinationVariable String bugId,
            SimpMessageHeaderAccessor headerAccessor,
            Authentication authentication) {
        String username = authentication.getName();
        headerAccessor.getSessionAttributes().put("username", username);
        headerAccessor.getSessionAttributes().put("bugId", bugId);

        chatService.sendCurrentParticipants(bugId, username);

        chatService.sendJoinNotification(bugId, username);
    }

    @MessageMapping("/bug.chat.leave.{bugId}")
    public void leaveBugChat(
            @DestinationVariable String bugId,
            Authentication authentication) {
        String username = authentication.getName();
        chatService.sendLeaveNotification(bugId, username);
    }

    @MessageMapping("/bug.chat.read.{bugId}")
    public void markMessagesAsRead(
            @DestinationVariable String bugId,
            Authentication authentication) {
        chatService.markMessagesAsRead(bugId, authentication.getName());
    }

    @GetMapping("/api/v1/chat/messages/{bugId}")
    public List<ChatMessage> getMessagesByBugId(@PathVariable String bugId) {
        return chatService.getMessagesByBugId(bugId);
    }
}