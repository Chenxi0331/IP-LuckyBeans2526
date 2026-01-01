package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    java.util.List<ForumPost> findByAuthorId(Long userId);
    java.util.List<ForumPost> findByCategory(String category);
    java.util.List<ForumPost> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String title, String content);
    
    @org.springframework.data.jpa.repository.Query("SELECT p.category, COUNT(p) FROM ForumPost p GROUP BY p.category ORDER BY COUNT(p) DESC")
    java.util.List<Object[]> countPostsByCategory();
}
