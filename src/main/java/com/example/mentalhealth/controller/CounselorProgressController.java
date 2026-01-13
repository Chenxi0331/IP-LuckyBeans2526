package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.mentalhealth.repository.SelfCareModuleRepository;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/counselor/progress")
@PreAuthorize("hasAuthority('ROLE_COUNSELOR')")
public class CounselorProgressController {
    
    @Autowired
    private SelfCareModuleProgressService moduleProgressService;
    
    @Autowired
    private SelfCareModuleRepository moduleRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SelfCareRecommendationService recommendationService;
    
    /**
     * View all students' self-care progress overview
     */
    @GetMapping
    public String viewAllProgress(Model model) {
        List<User> students = userService.getAllStudents();
        
        List<StudentProgressSummary> progressList = new ArrayList<>();
        
        for (User student : students) {
            SelfCareModuleProgressService.ProgressStatistics stats = 
                moduleProgressService.getProgressStatistics(student.getUserId());
            
            StudentProgressSummary summary = new StudentProgressSummary();
            summary.setStudentId(student.getUserId());
            summary.setStudentName(student.getFullName());
            summary.setStudentEmail(student.getEmail());
            summary.setCompletedModules(stats.getCompletedCount());
            summary.setAverageProgress(stats.getAverageProgress());
            summary.setTotalModules(stats.getTotalModules());
            summary.setInProgressCount(stats.getInProgressCount());
            
            progressList.add(summary);
        }
        
        model.addAttribute("studentProgressList", progressList);
        
        return "self-care/counselor-progress-overview";
    }
    
    /**
     * View specific student's detailed progress
     */
    @GetMapping("/student/{userId}")
    public String viewStudentProgress(@PathVariable Integer userId, Model model) {
        User student = userService.findById(userId.longValue())
            .orElseThrow(() -> new RuntimeException("Student not found"));
        
        // Get all progress records for the student
        List<UserModuleProgress> progressList = moduleProgressService.getUserProgress(userId);
        
        // Attach module info to each progress record
        List<ModuleProgressDetail> detailList = new ArrayList<>();
        for (UserModuleProgress progress : progressList) {
            SelfCareModule module = moduleProgressService.getModuleById(progress.getModuleId());
            
            ModuleProgressDetail detail = new ModuleProgressDetail();
            detail.setProgress(progress);
            detail.setModule(module);
            
            detailList.add(detail);
        }
        
        // Get statistics
        SelfCareModuleProgressService.ProgressStatistics stats = 
            moduleProgressService.getProgressStatistics(userId);
        
        // Get all available modules for recommendation (only unlocked ones)
        List<SelfCareModule> allModules = moduleProgressService.getAllModules();
        List<SelfCareModule> unlockedModules = allModules.stream()
            .filter(m -> !m.getIsLocked())
            .collect(java.util.stream.Collectors.toList());
        
        model.addAttribute("student", student);
        model.addAttribute("progressDetails", detailList);
        model.addAttribute("statistics", stats);
        model.addAttribute("allModules", unlockedModules);
        
        return "self-care/counselor-student-progress-detail";
    }

    
    
    /**
     * Recommend a module to student
     */
    @PostMapping("/student/{userId}/recommend")
    public String recommendModule(@PathVariable Integer userId,
                                   @RequestParam Integer moduleId,
                                   @RequestParam String reason,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            User counselor = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Counselor not found"));
            
            recommendationService.recommendModule(
                counselor.getId(),
                userId.longValue(),
                moduleId,
                reason
            );
            
            redirectAttributes.addFlashAttribute("message", "Module recommended successfully!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        
        return "redirect:/counselor/progress/student/" + userId;
    }
    
    // ==================== Inner DTO Classes ====================
    
    public static class StudentProgressSummary {
        private Integer studentId;
        private String studentName;
        private String studentEmail;
        private Integer completedModules;
        private Integer averageProgress;
        private Integer totalModules;
        private Integer inProgressCount;
        
        // Getters and Setters
        public Integer getStudentId() { return studentId; }
        public void setStudentId(Integer studentId) { this.studentId = studentId; }
        
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        
        public String getStudentEmail() { return studentEmail; }
        public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
        
        public Integer getCompletedModules() { return completedModules; }
        public void setCompletedModules(Integer completedModules) { this.completedModules = completedModules; }
        
        public Integer getAverageProgress() { return averageProgress; }
        public void setAverageProgress(Integer averageProgress) { this.averageProgress = averageProgress; }
        
        public Integer getTotalModules() { return totalModules; }
        public void setTotalModules(Integer totalModules) { this.totalModules = totalModules; }
        
        public Integer getInProgressCount() { return inProgressCount; }
        public void setInProgressCount(Integer inProgressCount) { this.inProgressCount = inProgressCount; }
    }
    
    public static class ModuleProgressDetail {
        private UserModuleProgress progress;
        private SelfCareModule module;
        
        // Getters and Setters
        public UserModuleProgress getProgress() { return progress; }
        public void setProgress(UserModuleProgress progress) { this.progress = progress; }
        
        public SelfCareModule getModule() { return module; }
        public void setModule(SelfCareModule module) { this.module = module; }
    }
}