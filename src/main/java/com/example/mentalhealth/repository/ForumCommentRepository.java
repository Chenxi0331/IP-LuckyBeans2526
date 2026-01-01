package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.ForumComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {

    List<ForumComment> findByPostId(Long postId);

    long countByCreatedAtAfter(java.time.LocalDateTime date);
}
