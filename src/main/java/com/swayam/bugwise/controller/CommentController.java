package com.swayam.bugwise.controller;

import com.swayam.bugwise.dto.CommentRequestDTO;
import com.swayam.bugwise.entity.Comment;
import com.swayam.bugwise.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<Void> createComment(@Valid @RequestBody CommentRequestDTO request) {
        commentService.createComment(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            Authentication authentication,
            @PathVariable String commentId,
            @Valid @RequestBody CommentRequestDTO request) {
        commentService.updateComment(request, commentId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/bug/{bugId}")
    public Page<Comment> getBugComments(@PathVariable String bugId, Pageable pageable) {
        return commentService.getBugComments(bugId, pageable);
    }

    @GetMapping("/user/{userId}")
    public List<Comment> getUserComments(@PathVariable String userId) {
        return commentService.getUserComments(userId);
    }
}