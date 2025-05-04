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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @MessageMapping("/bug.chat.send.{bugId}")
    @SendTo("/topic/bug.{bugId}")
    public ChatMessage sendMessage(
            @DestinationVariable String bugId,
            @Valid @Payload ChatMessageRequestDTO chatMessage,
            Authentication authentication) {
        chatMessage.setBugId(bugId);
        chatMessage.setSender(authentication.getName());
        return chatService.sendMessage(chatMessage);
    }

    @MessageMapping("/bug.chat.join.{bugId}")
    public void joinBugChat(
            @DestinationVariable String bugId,
            SimpMessageHeaderAccessor headerAccessor,
            Authentication authentication) {
        String username = authentication.getName();
        headerAccessor.getSessionAttributes().put("username", username);
        headerAccessor.getSessionAttributes().put("bugId", bugId);
        chatService.handleUserJoin(bugId, username);
    }

    @MessageMapping("/bug.chat.typing.{bugId}")
    public void sendTypingNotification(
            @DestinationVariable String bugId,
            Authentication authentication) {
        chatService.sendTypingNotification(bugId, authentication.getName());
    }

    @MessageMapping("/bug.chat.read.{bugId}")
    public void markMessagesAsRead(
            @DestinationVariable String bugId,
            Authentication authentication) {
        chatService.markMessagesAsRead(bugId, authentication.getName());
    }

    @MessageMapping("/bug.chat.leave.{bugId}")
    public void leaveBugChat(
            @DestinationVariable String bugId,
            SimpMessageHeaderAccessor headerAccessor,
            Authentication authentication) {
        String username = authentication.getName();
        chatService.handleUserLeave(bugId, username);
    }

    @GetMapping("/api/v1/chat/messages/{bugId}")
    @ResponseBody
    public List<ChatMessage> getMessages(@PathVariable String bugId) {
        return chatService.getMessagesForBug(bugId);
    }
}