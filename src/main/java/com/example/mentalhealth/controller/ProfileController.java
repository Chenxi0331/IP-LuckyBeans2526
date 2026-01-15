package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.Role;
import com.example.mentalhealth.model.User;
import com.example.mentalhealth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    // Student Profile
    // URL: localhost:8080/profile/student-profile
    @GetMapping("/profile/student-profile")
    public String showStudentProfile(Model model) {
        User user = getCurrentUser();
        if (user.getRole() != Role.STUDENT) {
            return "redirect:/";
        }
        model.addAttribute("user", user);
        // Look for file: templates/profile/student-profile.html
        return "profile/student-profile"; 
    }
    
    // Counselor Profile
    @GetMapping("/profile/counselor-profile")
    public String showCounselorProfile(Model model) {
        User user = getCurrentUser();
        if (user.getRole() != Role.COUNSELOR) {
            return "redirect:/";
        }
        model.addAttribute("user", user);
        // Look for file: templates/profile/counselor-profile.html
        return "profile/counselor-profile";
    }
    
    // Admin Profile
    @GetMapping("/profile/admin-profile")
    public String showAdminProfile(Model model) {
        User user = getCurrentUser();
        if (user.getRole() != Role.ADMIN) {
            return "redirect:/";
        }
        model.addAttribute("user", user);
        // Look for file: templates/profile/admin-profile.html
        return "profile/admin-profile";
    }
    
    // Edit Profile
    @PostMapping("/profile/edit")
    public String editProfile(@RequestParam String fullName,
                             @RequestParam String phone,
                             @RequestParam(required = false) String username,
                             RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        
        user.setFullName(fullName);
        user.setPhone(phone);
        if (username != null && !username.trim().isEmpty()) {
            user.setUsername(username);
        }
        
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        
        return getRedirectUrl(user.getRole());
    }
    
    // Change Password
    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
            return getRedirectUrl(user.getRole());
        }
        
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "New passwords do not match");
            return getRedirectUrl(user.getRole());
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        redirectAttributes.addFlashAttribute("success", "Password changed successfully!");
        return getRedirectUrl(user.getRole());
    }
    
    // Admin: Toggle User Active Status
    @PostMapping("/admin/users/{userId}/toggle-active")
    public String toggleUserActive(@PathVariable Long userId, 
                                  RedirectAttributes redirectAttributes) {
        User admin = getCurrentUser();
        if (admin.getRole() != Role.ADMIN) {
            return "redirect:/";
        }
        
        User targetUser = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        targetUser.setActive(!targetUser.isActive());
        userRepository.save(targetUser);
        
        redirectAttributes.addFlashAttribute("success", 
            "User " + (targetUser.isActive() ? "activated" : "deactivated") + " successfully!");
        
        // This redirects back to the AdminController's user list
        return "redirect:/admin/users";
    }
    
    // Helper to redirect to the correct Profile URL
    private String getRedirectUrl(Role role) {
        switch (role) {
            case ADMIN:
                // Redirects to: @GetMapping("/profile/admin-profile")
                return "redirect:/profile/admin-profile"; 
            case COUNSELOR:
                return "redirect:/profile/counselor-profile";
            case STUDENT:
            default:
                return "redirect:/profile/student-profile";
        }
    }
}