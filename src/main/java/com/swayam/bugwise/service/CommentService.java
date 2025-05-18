package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.CommentRequestDTO;
import com.swayam.bugwise.dto.NotificationMessageDTO;
import com.swayam.bugwise.entity.Bug;
import com.swayam.bugwise.entity.Comment;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.NotificationType;
import com.swayam.bugwise.exception.ResourceNotFoundException;
import com.swayam.bugwise.exception.ValidationException;
import com.swayam.bugwise.repository.jpa.BugRepository;
import com.swayam.bugwise.repository.jpa.CommentRepository;
import com.swayam.bugwise.repository.jpa.UserRepository;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final BugRepository bugRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public void createComment(CommentRequestDTO request, String updatedBy) {
        Bug bug = bugRepository.findById(request.getBugId())
                .orElseThrow(() -> new NoSuchElementException("Bug not found"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setBug(bug);
        comment.setUser(userRepository.findByEmail(updatedBy).get());

        List<String> userList = new ArrayList<>();
        if(bug.getAssignedDeveloper() != null){
            userList.addAll(bug.getAssignedDeveloper().stream().map(User::getEmail).toList());
        }
        userList.add(bug.getReportedBy().getEmail());

        NotificationMessageDTO message = new NotificationMessageDTO(
                NotificationType.COMMENT_ADDED,
                "Comment Added",
                "New Comment on Bug",
                Map.of(
                        "bugId", request.getBugId(),
                        "commentAuthor", updatedBy
                ),
                userList,
                new NotificationMessageDTO.InAppDetails("/bugs/" + request.getBugId(), "comment-icon.png")
        );

        notificationService.sendNotification(message);

        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public Page<Comment> getBugComments(String bugId, Pageable pageable) {
        bugRepository.findById(bugId).orElseThrow(() -> new ResourceNotFoundException("Bug not found"));
        return commentRepository.findBugCommentsWithPagination(bugId, pageable);
    }

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_READ)
    public List<Comment> getUserComments(String userId) {
        userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return commentRepository.findByUserId(userId);
    }

    public void updateComment(CommentRequestDTO request, String commentId, String currentUsername) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("Comment not found with id: " + commentId));

        if (!comment.getBug().getId().equals(request.getBugId())) {
            throw new IllegalArgumentException("Comment does not belong to the specified bug");
        }

        if (!comment.getUser().getUsername().equals(currentUsername)) {
            throw new ValidationException(Map.of("error", "You are not authorized to update this comment"));
        }

        comment.setContent(request.getContent());
        commentRepository.save(comment);
    }
}