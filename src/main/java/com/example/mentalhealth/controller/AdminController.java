package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.User;
import com.example.mentalhealth.model.Role;
import com.example.mentalhealth.repository.UserRepository;
import com.example.mentalhealth.service.CounsellingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounsellingService counsellingService;

    @Autowired
    private com.example.mentalhealth.repository.UserAssessmentRepository userAssessmentRepository;
    
    @Autowired
    private com.example.mentalhealth.repository.CounselorChatRepository counselorChatRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Summary Stats
        long totalUsers = userRepository.count();
        long totalSessions = counsellingService.getAllSessions().size(); // Or use a count query if available
        long totalAssessments = userAssessmentRepository.count();
        long totalChats = counselorChatRepository.count();
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalSessions", totalSessions);
        model.addAttribute("totalAssessments", totalAssessments);
        model.addAttribute("totalChats", totalChats);
        
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String manageUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/sessions")
    public String viewAllSessions(Model model) {
        // Assuming service has a method to get all sessions or we fetch from repo
        model.addAttribute("sessions", counsellingService.getAllSessions());
        return "counselling/all-sessions";
    }



    @GetMapping("/settings")
    public String settings() {
        return "admin/settings";
    }
}
