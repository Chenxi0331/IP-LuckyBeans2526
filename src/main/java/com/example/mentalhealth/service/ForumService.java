package com.example.mentalhealth.service;

import com.example.mentalhealth.model.ForumPost;
import com.example.mentalhealth.model.ForumComment;
import com.example.mentalhealth.repository.ForumPostRepository;
import com.example.mentalhealth.repository.ForumCommentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForumService {

    private final ForumPostRepository postRepo;
    private final ForumCommentRepository commentRepo;

    public ForumService(ForumPostRepository postRepo, ForumCommentRepository commentRepo) {
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
    }

    // --- CRUD ---
    public void savePost(ForumPost post) { postRepo.save(post); }

    // public ForumPost createPost(ForumPost post) {
    //     return postRepo.save(post);
    // }

    public List<ForumPost> getAllPosts() {
        return postRepo.findAll();
    }

    public ForumPost getPostById(Long id) {
        return postRepo.findById(id).orElse(null);
    }

    public void deletePost(Long id) { postRepo.deleteById(id); }

    // --- Comments ---
    public List<ForumComment> getComments(Long postId) {
        return commentRepo.findByPostId(postId);
    }

    public ForumComment addComment(ForumComment comment) {
        return commentRepo.save(comment);
    }

    public List<ForumPost> getPostsByUserId(Long userId) {
        return postRepo.findByUserId(userId);
    }

    public long countTodayPosts() {
        return postRepo.findAll().stream()
                .filter(p -> p.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now()))
                .count();
    }
}
