package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.CounselorChat;
import com.example.mentalhealth.model.CounselorMessage;
import com.example.mentalhealth.model.User;
import com.example.mentalhealth.model.Role;
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
@RequestMapping("/chatbot")
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

    /**
     * Show appropriate page based on user role.
     * - Student: shows counselor-list
     * - Counselor: shows their chat list with students
     */
    @GetMapping("/counselor-list")
    public String showCounselors(Model model) {
        User user = getCurrentUser();
        
        // If user is a counselor, show their student chats instead
        if (user.getRole() == Role.COUNSELOR) {
            List<CounselorChat> chats = counselorChatService.getUserChats(user.getId());
            model.addAttribute("user", user);
            model.addAttribute("chats", chats);
            return "chatbot/counselor-chat-list"; // New page for counselor's chats
        }
        
        // If user is a student, show available counselors
        List<User> counselors = counselorChatService.getActiveCounselors();
        model.addAttribute("user", user);
        model.addAttribute("counselors", counselors);
        return "chatbot/counselor-list";
    }

    /**
     * Create or open chat with counselor (for students only).
     */
    @GetMapping("/with/{counselorId}")
    public String openChat(@PathVariable Long counselorId) {
        User student = getCurrentUser();
        
        // Only students can initiate chats
        if (student.getRole() != Role.STUDENT) {
            return "redirect:/chatbot/counselor-list?error=Only students can initiate chats";
        }
        
        try {
            CounselorChat chat = counselorChatService.createOrGetChat(student.getId(), counselorId);
            return "redirect:/chatbot/room/" + chat.getId();
        } catch (RuntimeException e) {
            return "redirect:/chatbot/counselor-list?error=" + e.getMessage();
        }
    }

    /**
     * Show chat room - works for both students and counselors.
     */
    @GetMapping("/room/{chatId}")
    public String showChatRoom(@PathVariable Long chatId, Model model) {
        User user = getCurrentUser();
        
        CounselorChat chat = counselorChatService.getChatById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        
        // Security check: only student or counselor in this chat can access it
        boolean isStudent = chat.getStudent().getId().equals(user.getId());
        boolean isCounselor = chat.getCounselor().getId().equals(user.getId());
        
        if (!isStudent && !isCounselor) {
            throw new RuntimeException("Unauthorized access to this chat");
        }

        List<CounselorMessage> messages = counselorChatService.getChatMessages(chatId);

        model.addAttribute("user", user);
        model.addAttribute("chat", chat);
        model.addAttribute("messages", messages);
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("chatId", chatId);

        return "chatbot/counselor-chat-room"; // NO .html extension!
    }

    /**
     * Send message (AJAX).
     */
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

    /**
     * Mark message as read (AJAX).
     */
    @PostMapping("/messages/{messageId}/read")
    @ResponseBody
    public Map<String, Object> markAsRead(@PathVariable Long messageId) {
        counselorChatService.markMessageAsRead(messageId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return response;
    }

    /**
     * Get all chats for the current user.
     * Used by both students and counselors.
     */
    @GetMapping("/my-chats")
    public String showMyChats(Model model) {
        User user = getCurrentUser();
        List<CounselorChat> chats = counselorChatService.getUserChats(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("chats", chats);
        model.addAttribute("userRole", user.getRole());
        return "chatbot/counselor-chat-list";
    }

    /**
     * Close a chat session.
     */
    @PostMapping("/room/{chatId}/close")
    public String closeChat(@PathVariable Long chatId) {
        counselorChatService.closeChat(chatId);
        return "redirect:/chatbot/my-chats";
    }
}