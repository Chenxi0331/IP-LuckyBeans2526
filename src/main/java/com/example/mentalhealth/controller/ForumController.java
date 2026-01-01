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
    public String viewForum(@RequestParam(required = false) String category,
                            @RequestParam(required = false) String search,
                            Model model, Authentication auth) {
        model.addAttribute("posts", service.filterPosts(category, search));
        // Pass current category/search to view for highlighting
        model.addAttribute("currentCategory", category);
        model.addAttribute("currentSearch", search);
        if (auth != null) {
            User user = userRepository.findByEmail(auth.getName()).orElse(null);
            if (user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.COUNSELOR)) {
                 model.addAttribute("statsCategories", service.getPostStats()); // List<Object[]>
                 model.addAttribute("statsCommentsWeek", service.countCommentsLastWeek());
                 model.addAttribute("statsToday", service.countTodayPosts());
                 model.addAttribute("isAdminOrCounselor", true);
            }
        }
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
        post.setAuthor(user);
        service.savePost(post);
        return "redirect:/forum";
    }

    @GetMapping("/post/{id}")
    public String viewPost(@PathVariable Long id, Model model) {
        model.addAttribute("post", service.getPostById(id));
        model.addAttribute("comments", service.getComments(id));
        model.addAttribute("comment", new ForumComment());
        return "forum/post";
    }

    @PostMapping("/comment")
    public String addComment(@ModelAttribute ForumComment comment, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        comment.setAuthor(user);
        service.addComment(comment);
        return "redirect:/forum/post/" + comment.getPostId();
    }

    @GetMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        ForumPost post = service.getPostById(id);
        
        if (post != null && (post.getAuthor().getId().equals(user.getId()) || user.getRole() == Role.ADMIN || user.getRole() == Role.COUNSELOR)) {
            service.deletePost(id);
        }
        return "redirect:/forum";
    }

    @GetMapping("/edit/{id}")
    public String editPost(@PathVariable Long id, Model model, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        ForumPost post = service.getPostById(id);

        if (post == null) return "redirect:/forum";

        // Authorization: Author OR Admin OR Counselor
        boolean isAuthor = post.getAuthor().getId().equals(user.getId());
        boolean isAdminOrCounselor = user.getRole() == Role.ADMIN || user.getRole() == Role.COUNSELOR;

        if (isAuthor || isAdminOrCounselor) {
            model.addAttribute("post", post);
            return "forum/edit";
        }

        return "redirect:/forum";
    }

    @PostMapping("/update")
    public String updatePost(@ModelAttribute ForumPost post, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        ForumPost existingPost = service.getPostById(post.getId());

        if (existingPost == null) return "redirect:/forum";

        boolean isAuthor = existingPost.getAuthor().getId().equals(user.getId());
        boolean isAdminOrCounselor = user.getRole() == Role.ADMIN || user.getRole() == Role.COUNSELOR;

        if (isAuthor || isAdminOrCounselor) {
            existingPost.setTitle(post.getTitle());
            existingPost.setContent(post.getContent());
            existingPost.setCategory(post.getCategory());
            existingPost.setStatus(post.getStatus());
            service.updatePost(existingPost);
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
