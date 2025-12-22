package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.ForumPost;
import com.example.mentalhealth.model.Role;
import com.example.mentalhealth.model.User;
import com.example.mentalhealth.repository.UserRepository;
import com.example.mentalhealth.model.ForumComment;
import com.example.mentalhealth.service.ForumService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@Controller
@RequestMapping("/forum")
public class ForumController {

    @Autowired
    private ForumService service;
    @Autowired
    private UserRepository userRepository;

    public ForumController(ForumService service) {
        this.service = service;
    }

    @GetMapping
    public String viewForum(Model model) {
        model.addAttribute("posts", service.getAllPosts());
        return "forum/list";
    }

    @GetMapping("/new")
    public String newPost(Model model) {
        model.addAttribute("post", new ForumPost());
        return "forum/new";
    }

    @PostMapping("/new")
    public String createPost(@ModelAttribute ForumPost post, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        post.setUserId(user.getId());
        service.savePost(post);
        return "redirect:/forum";
    }

    @GetMapping("/post/{id}")
    public String viewPost(@PathVariable Long id, Model model) {
        // Use mock data for demo/testing if id == 999
        if (id == 999) {
            ForumPost mockPost = new ForumPost();
            mockPost.setId(999L);
            mockPost.setTitle("Mock Title");
            mockPost.setContent("This is mock content for testing.");
            mockPost.setCreatedAt(java.time.LocalDateTime.now());

            ForumComment c1 = new ForumComment();
            c1.setUserId(1L);
            c1.setContent("Nice post!");
            c1.setCreatedAt(java.time.LocalDateTime.now());

            ForumComment c2 = new ForumComment();
            c2.setUserId(2L);
            c2.setContent("Thanks for sharing.");
            c2.setCreatedAt(java.time.LocalDateTime.now());

            java.util.List<ForumComment> mockComments = java.util.List.of(c1, c2);

            model.addAttribute("post", mockPost);
            model.addAttribute("comments", mockComments);
            model.addAttribute("comment", new ForumComment());
            return "forum/post";
        }
        // ...existing code...
        model.addAttribute("post", service.getPostById(id));
        model.addAttribute("comments", service.getComments(id));
        model.addAttribute("comment", new ForumComment());
        return "forum/post";
    }

    @PostMapping("/comment")
    public String addComment(@ModelAttribute ForumComment comment, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        comment.setUserId(user.getId());
        service.addComment(comment);
        return "redirect:/forum/post/" + comment.getPostId();
    }

    @GetMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        ForumPost post = service.getPostById(id);
        
        if (post.getUserId().equals(user.getId()) || user.getRole() == Role.ADMIN) {
            service.deletePost(id);
        }
        return "redirect:/forum";
    }

    @GetMapping("/my-posts")
    public String viewMyPosts(Model model, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        model.addAttribute("posts", service.getPostsByUserId(user.getId()));
        return "forum/list"; 
    }
}
