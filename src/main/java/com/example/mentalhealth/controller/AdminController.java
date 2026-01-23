package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.model.User.AccountStatus;
import com.example.mentalhealth.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.example.mentalhealth.service.CounsellingService;
import com.example.mentalhealth.service.UserService;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounselorVerificationRepository verificationRepository;

    @Autowired
    private UserActivityLogRepository activityLogRepository;

    @Autowired
    private UserLoginHistoryRepository loginHistoryRepository;

    @Autowired
    private CounsellingService counsellingService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    // ============ USER MANAGEMENT ============

    @GetMapping("/users")
    public String manageUsers(@RequestParam(defaultValue = "all") String filter, Model model) {
        List<User> users;

        switch (filter.toLowerCase()) {
            case "students":
                users = userRepository.findByRole(Role.STUDENT);
                model.addAttribute("filterType", "Students");
                break;
            case "counselors":
                users = userRepository.findByRole(Role.COUNSELOR);
                model.addAttribute("filterType", "Counselors");
                break;
            default:
                users = userRepository.findAll();
                model.addAttribute("filterType", "All Users");
        }

        model.addAttribute("users", users);
        model.addAttribute("currentFilter", filter);
        // YOUR ACTUAL TEMPLATE LOCATION
        return "admin/users-list";
    }

    @GetMapping("/counselor-verification/{id}/view")
    public String viewVerificationRequest(@PathVariable Long id, Model model) {
        CounselorVerification verification = verificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Verification request not found"));

        model.addAttribute("verification", verification);
        model.addAttribute("user", verification.getUser());
        return "admin/verification-details";
    }

    @GetMapping("/users/view/{id}")
    public String viewUserDetails(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);
        
        // Get activity log
        List<UserActivityLog> activities = activityLogRepository.findLatestActivityByUserId(id);
        model.addAttribute("activities", activities);

        // Get login history
        List<UserLoginHistory> loginHistory = loginHistoryRepository.findLatestLoginsByUserId(id);
        model.addAttribute("loginHistory", loginHistory);

        // If counselor, get verification status
        if (user.getRole() == Role.COUNSELOR) {
            Optional<CounselorVerification> verification = verificationRepository.findByUserId(id);
            model.addAttribute("verification", verification.orElse(null));
        }

        // YOUR ACTUAL TEMPLATE LOCATION
        return "admin/user-details";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        // YOUR ACTUAL TEMPLATE LOCATION
        return "admin/user-edit";
    }

    @PostMapping("/users/edit/{id}")
    public String updateUser(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam(required = false) String username,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFullName(fullName);
        user.setPhone(phone);
        if (username != null && !username.trim().isEmpty()) {
            user.setUsername(username);
        }

        userRepository.save(user);
        logActivity(user, "PROFILE_UPDATED", "Profile updated by admin");

        redirectAttributes.addFlashAttribute("success", "User information updated successfully!");
        return "redirect:/admin/users/view/" + id;
    }

    // ============ ACCOUNT STATUS MANAGEMENT ============

    @PostMapping("/users/{id}/deactivate")
    public String deactivateUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(false);
        user.setAccountStatus(User.AccountStatus.INACTIVE);
        userRepository.save(user);
        logActivity(user, "ACCOUNT_DEACTIVATED", "Account deactivated by admin");

        redirectAttributes.addFlashAttribute("success", "User account has been deactivated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/activate")
    public String activateUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(true);
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        userRepository.save(user);
        logActivity(user, "ACCOUNT_ACTIVATED", "Account activated by admin");

        redirectAttributes.addFlashAttribute("success", "User account has been activated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/suspend")
    public String suspendUser(
            @PathVariable Long id,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setAccountStatus(User.AccountStatus.SUSPENDED);
        userRepository.save(user);
        logActivity(user, "ACCOUNT_SUSPENDED", "Account suspended by admin. Reason: " + reason);

        redirectAttributes.addFlashAttribute("success", "User account has been suspended.");
        return "redirect:/admin/users";
    }

@PostMapping("/users/{id}/unsuspend")
public String unsuspendUser(@PathVariable Long id) {
    Optional<User> userOptional = userService.findById(id);
    if (userOptional.isPresent()) {
        User user = userOptional.get();
        user.setAccountStatus(AccountStatus.ACTIVE);
        userService.save(user);
    }
    return "redirect:/admin/users";
}

    @PostMapping("/users/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User admin = getCurrentUser();
        logActivity(admin, "USER_DELETED", "Deleted user: " + user.getEmail());

        userRepository.deleteById(id);
        
        redirectAttributes.addFlashAttribute("success", "User has been deleted.");
        return "redirect:/admin/users";
    }

    // ============ COUNSELOR VERIFICATION ============

    @GetMapping("/counselor-verification")
    public String counselorVerificationList(Model model) {
        List<CounselorVerification> pendingVerifications = 
            verificationRepository.findByStatus(CounselorVerification.VerificationStatus.PENDING);
        
        model.addAttribute("pendingVerifications", pendingVerifications);
        // Create admin/counselor-verification.html template
        return "admin/counselor-verification";
    }

    @PostMapping("/counselor-verification/{id}/approve")
    public String approveCounselorVerification(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        CounselorVerification verification = verificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Verification request not found"));

        User admin = getCurrentUser();
        User counselor = verification.getUser();

        // Update verification
        verification.setStatus(CounselorVerification.VerificationStatus.APPROVED);
        verification.setReviewedBy(admin);
        verification.setReviewedAt(LocalDateTime.now());
        verificationRepository.save(verification);

        // Activate counselor
        counselor.setActive(true);
        counselor.setAccountStatus(User.AccountStatus.ACTIVE);
        userRepository.save(counselor);

        logActivity(admin, "COUNSELOR_APPROVED", "Approved counselor: " + counselor.getEmail());

        redirectAttributes.addFlashAttribute("success", 
            "Counselor verification approved! User can now login.");
        return "redirect:/admin/counselor-verification";
    }

    @PostMapping("/counselor-verification/{id}/reject")
    public String rejectCounselorVerification(
            @PathVariable Long id,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {

        CounselorVerification verification = verificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Verification request not found"));

        User admin = getCurrentUser();
        User counselor = verification.getUser();

        // Update verification
        verification.setStatus(CounselorVerification.VerificationStatus.REJECTED);
        verification.setRejectionReason(reason);
        verification.setReviewedBy(admin);
        verification.setReviewedAt(LocalDateTime.now());
        verificationRepository.save(verification);

        logActivity(admin, "COUNSELOR_REJECTED", "Rejected counselor: " + counselor.getEmail() + ". Reason: " + reason);

        redirectAttributes.addFlashAttribute("success", "Counselor verification rejected.");
        return "redirect:/admin/counselor-verification";
    }

    @GetMapping("/sessions")
    public String viewAllSessions(Model model) {
        model.addAttribute("sessions", counsellingService.getAllSessions());
        return "admin/sessions";
    }

    @GetMapping("/reports")
    public String reports() {
        return "admin/reports";
    }

    @GetMapping("/settings")
    public String settings() {
        return "admin/settings";
    }

    // ============ HELPER METHODS ============

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Admin user not found"));
    }

    private void logActivity(User user, String activityType, String description) {
        UserActivityLog log = new UserActivityLog();
        log.setUser(user);
        log.setActivityType(activityType);
        log.setDescription(description);
        log.setTimestamp(LocalDateTime.now());
        activityLogRepository.save(log);
    }
}