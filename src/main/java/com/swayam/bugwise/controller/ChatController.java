package com.swayam.bugwise.controller;

import com.swayam.bugwise.dto.ChatMessageRequestDTO;
import com.swayam.bugwise.entity.ChatMessage;
import com.swayam.bugwise.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @MessageMapping("/bug.chat.send")
    @SendTo("/topic/bug.{bugId}")
    public ChatMessage sendMessage(
            @DestinationVariable String bugId,
            @Payload ChatMessageRequestDTO chatMessage,
            @AuthenticationPrincipal UserDetails userDetails) {
        chatMessage.setBugId(bugId);
        return chatService.sendMessage(chatMessage);
    }

    @MessageMapping("/bug.chat.join.{bugId}")
    public void joinBugChat(
            @DestinationVariable String bugId,
            SimpMessageHeaderAccessor headerAccessor,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        headerAccessor.getSessionAttributes().put("username", username);
        headerAccessor.getSessionAttributes().put("bugId", bugId);
        chatService.handleUserJoin(bugId, username);
    }

    @MessageMapping("/bug.chat.typing.{bugId}")
    public void sendTypingNotification(
            @DestinationVariable String bugId,
            @AuthenticationPrincipal UserDetails userDetails) {
        chatService.sendTypingNotification(bugId, userDetails.getUsername());
    }

    @MessageMapping("/bug.chat.read.{bugId}")
    public void markMessagesAsRead(
            @DestinationVariable String bugId,
            @AuthenticationPrincipal UserDetails userDetails) {
        chatService.markMessagesAsRead(bugId, userDetails.getUsername());
    }

    @MessageMapping("/bug.chat.leave.{bugId}")
    public void leaveBugChat(
            @DestinationVariable String bugId,
            SimpMessageHeaderAccessor headerAccessor,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        chatService.handleUserLeave(bugId, username);
    }
}