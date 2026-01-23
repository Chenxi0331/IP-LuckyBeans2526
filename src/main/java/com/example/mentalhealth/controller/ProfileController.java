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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Controller
public class ProfileController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String UPLOAD_DIR = "uploads/profile-pictures/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif"};
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    // Student Profile
    @GetMapping("/profile/student-profile")
    public String showStudentProfile(Model model) {
        User user = getCurrentUser();
        if (user.getRole() != Role.STUDENT) {
            return "redirect:/";
        }
        model.addAttribute("user", user);
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

    // Upload Profile Picture
    @PostMapping("/profile/upload-picture")
    public String uploadProfilePicture(@RequestParam("profilePicture") MultipartFile file,
                                       RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();

            // Validate file
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file");
                return getRedirectUrl(user.getRole());
            }

            // Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                redirectAttributes.addFlashAttribute("error", "File size must be less than 5MB");
                return getRedirectUrl(user.getRole());
            }

            // Check file extension
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !isAllowedExtension(originalFilename)) {
                redirectAttributes.addFlashAttribute("error", "Only JPG, PNG, GIF files are allowed");
                return getRedirectUrl(user.getRole());
            }

            // Save file
            String pictureFilePath = saveProfilePicture(file, user.getId());

            // Delete old picture if exists
            if (user.getProfilePicturePath() != null && !user.getProfilePicturePath().isEmpty()) {
                deleteOldPicture(user.getProfilePicturePath());
            }

            // Update user
            user.setProfilePicturePath(pictureFilePath);
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("success", "Profile picture updated successfully!");

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload picture: " + e.getMessage());
        }

        return getRedirectUrl(getCurrentUser().getRole());
    }

    // Delete Profile Picture
    @PostMapping("/profile/delete-picture")
    public String deleteProfilePicture(RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();

            if (user.getProfilePicturePath() == null || user.getProfilePicturePath().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No profile picture to delete");
                return getRedirectUrl(user.getRole());
            }

            // Delete file
            deleteOldPicture(user.getProfilePicturePath());

            // Update user
            user.setProfilePicturePath(null);
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("success", "Profile picture deleted successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete picture: " + e.getMessage());
        }

        return getRedirectUrl(getCurrentUser().getRole());
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

        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters");
            return getRedirectUrl(user.getRole());
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        redirectAttributes.addFlashAttribute("success", "Password changed successfully!");
        return getRedirectUrl(user.getRole());
    }

    // ============ HELPER METHODS ============

    private String saveProfilePicture(MultipartFile file, Long userId) throws IOException {
        String fileName = userId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR);

        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        Path filePath = path.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        return UPLOAD_DIR + fileName;
    }

    private void deleteOldPicture(String picturePath) {
        try {
            Path path = Paths.get(picturePath);
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
            // Log error but don't fail the operation
            System.err.println("Failed to delete old picture: " + e.getMessage());
        }
    }

    private boolean isAllowedExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerFilename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private String getRedirectUrl(Role role) {
        switch (role) {
            case ADMIN:
                return "redirect:/profile/admin-profile"; 
            case COUNSELOR:
                return "redirect:/profile/counselor-profile";
            case STUDENT:
            default:
                return "redirect:/profile/student-profile";
        }
    }
}