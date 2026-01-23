// =====================================================
// FIX #1 & #2: Counselor should NOT be active until approved
// File: src/main/java/com/example/mentalhealth/controller/AuthController.java
// =====================================================

package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.User;
import com.example.mentalhealth.model.Role;
import com.example.mentalhealth.model.CounselorVerification;
import com.example.mentalhealth.repository.UserRepository;
import com.example.mentalhealth.repository.CounselorVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounselorVerificationRepository verificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String UPLOAD_DIR = "uploads/certificates/";

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
        @RequestParam String fullName,
        @RequestParam String email,
        @RequestParam String password,
        @RequestParam String role,
        @RequestParam(required = false) MultipartFile certificateFile,
        Model model) {

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "Email already registered");
            return "register";
        }

        try {
            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setCreatedAt(LocalDateTime.now());

            // For counselor, require approval
            if (role.equalsIgnoreCase("COUNSELOR")) {
                if (certificateFile == null || certificateFile.isEmpty()) {
                    model.addAttribute("error", "Certificate is required for counselor registration");
                    return "register";
                }

                user.setRole(Role.COUNSELOR);
                // ✅ KEY FIX: Set active to FALSE initially
                user.setActive(false);
                user.setAccountStatus(User.AccountStatus.INACTIVE);

                // Save user first
                User savedUser = userRepository.save(user);

                // Handle certificate upload
                String certificatePath = saveCertificateFile(certificateFile);

                // Create verification request
                CounselorVerification verification = new CounselorVerification();
                verification.setUser(savedUser);
                verification.setCertificatePath(certificatePath);
                verification.setStatus(CounselorVerification.VerificationStatus.PENDING);
                verification.setRequestedAt(LocalDateTime.now());
                verificationRepository.save(verification);

                model.addAttribute("success", "Counselor registration submitted! Your application will be reviewed in 2-3 working days.");
                return "register";
            } else {
                // Student registration - direct approval
                user.setRole(Role.STUDENT);
                // ✅ KEY FIX: Students are active immediately
                user.setActive(true);
                user.setAccountStatus(User.AccountStatus.ACTIVE);
                userRepository.save(user);
                model.addAttribute("success", "Registration successful! Please login.");
                return "register";
            }

        } catch (IOException e) {
            model.addAttribute("error", "File upload failed: " + e.getMessage());
            return "register";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    private String saveCertificateFile(MultipartFile file) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR);

        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        Path filePath = path.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        return UPLOAD_DIR + fileName;
    }
}