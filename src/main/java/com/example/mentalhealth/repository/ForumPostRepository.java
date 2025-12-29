package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    java.util.List<ForumPost> findByUserId(Long userId);
}
