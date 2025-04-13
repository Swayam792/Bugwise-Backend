package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.CommentRequestDTO;
import com.swayam.bugwise.entity.Bug;
import com.swayam.bugwise.entity.Comment;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final BugRepository bugRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public Comment createComment(CommentRequestDTO request) {
        Bug bug = bugRepository.findById(request.getBugId())
                .orElseThrow(() -> new NoSuchElementException("Bug not found"));

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setBug(bug);
        comment.setUser(userService.getCurrentUser());

        return commentRepository.save(comment);
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
}