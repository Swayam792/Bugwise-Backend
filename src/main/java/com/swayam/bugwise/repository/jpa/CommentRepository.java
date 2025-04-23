package com.swayam.bugwise.repository.jpa;

import com.swayam.bugwise.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {
    List<Comment> findByBugId(String bugId);
    List<Comment> findByUserId(String userId);

    @Query(value = "SELECT * FROM comments c WHERE c.bug_id = :bugId ORDER BY c.created_at DESC",
            nativeQuery = true)
    Page<Comment> findBugCommentsWithPagination(
            @Param("bugId") String bugId,
            Pageable pageable
    );
}