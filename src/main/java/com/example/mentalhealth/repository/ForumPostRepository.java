package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    java.util.List<ForumPost> findByAuthorId(Long userId);
    java.util.List<ForumPost> findByCategory(String category);
    java.util.List<ForumPost> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String title, String content);
}
