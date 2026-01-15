package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.CounselorChat;
import com.example.mentalhealth.model.CounselorMessage;
import com.example.mentalhealth.model.User;
import com.example.mentalhealth.repository.UserRepository;
import com.example.mentalhealth.service.CounselorChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/chatbot") // <--- 1. Set base URL to /chatbot
public class CounselorChatController {
    
    @Autowired
    private CounselorChatService counselorChatService;
    
    @Autowired
    private UserRepository userRepository;
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    // Show counselor selection page for students
    // URL: localhost:8080/chatbot/counselor-list
    @GetMapping("/counselor-list") 
    public String showCounselors(Model model) {
        User user = getCurrentUser();
        List<User> counselors = counselorChatService.getActiveCounselors();
        
        model.addAttribute("user", user);
        model.addAttribute("counselors", counselors);
        // Look for file: templates/chatbot/counselor-list.html
        return "chatbot/counselor-list"; 
    }
    
    // Create or open chat with counselor
    @GetMapping("/with/{counselorId}")
    public String openChat(@PathVariable Long counselorId) {
        User student = getCurrentUser();
        CounselorChat chat = counselorChatService.createOrGetChat(student.getId(), counselorId);
        // Redirect to the chat room URL
        return "redirect:/chatbot/room/" + chat.getId();
    }
    
    // Show chat room
    // URL: localhost:8080/chatbot/room/{id}
    @GetMapping("/room/{chatId}")
    public String showChatRoom(@PathVariable Long chatId, Model model) {
        User user = getCurrentUser();
        CounselorChat chat = counselorChatService.getChatById(chatId)
            .orElseThrow(() -> new RuntimeException("Chat not found"));
        List<CounselorMessage> messages = counselorChatService.getChatMessages(chatId);
        
        model.addAttribute("user", user);
        model.addAttribute("chat", chat);
        model.addAttribute("chatId", chatId);
        model.addAttribute("messages", messages);
        model.addAttribute("currentUserId", user.getId());
        
        // Look for file: templates/chatbot/counselor-chat-room.html
        return "chatbot/counselor-chat-room";
    }
    
    // Send message (AJAX) - URL: /chatbot/send
    @PostMapping("/send")
    @ResponseBody
    public Map<String, Object> sendMessage(@RequestParam Long chatId,
                                           @RequestParam String content) {
        User user = getCurrentUser();
        CounselorMessage message = counselorChatService.sendMessage(chatId, user.getId(), content);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("id", message.getId());
        response.put("content", message.getContent());
        response.put("senderId", message.getSender().getId());
        response.put("senderName", message.getSender().getFullName());
        response.put("sentAt", message.getSentAt().toString());
        
        return response;
    }
    
    // Get messages (AJAX) - URL: /chatbot/messages/{id}
    @GetMapping("/messages/{chatId}")
    @ResponseBody
    public List<CounselorMessage> getMessages(@PathVariable Long chatId) {
        return counselorChatService.getChatMessages(chatId);
    }
    
    // Get user's chats
    @GetMapping("/my-chats")
    public String showMyChats(Model model) {
        User user = getCurrentUser();
        List<CounselorChat> chats = counselorChatService.getUserChats(user.getId());
        
        model.addAttribute("user", user);
        model.addAttribute("chats", chats);
        
        return "chatbot/my-counselor-chats"; // Assuming you have this file too
    }
}