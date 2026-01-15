package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.service.*;
import com.example.mentalhealth.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/counselor/progress")
@PreAuthorize("hasAuthority('ROLE_COUNSELOR')")
public class CounselorProgressController {
    
    @Autowired
    private SelfCareModuleProgressService moduleProgressService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SelfCareModuleRepository moduleRepository;
    
    @Autowired
    private UserModuleProgressRepository progressRepository;
    
    @GetMapping
    public String viewAllProgress(Model model) {
        List<User> students = userService.getAllStudents();
        List<StudentProgressSummary> progressList = new ArrayList<>();
        
        System.out.println("=== DEBUG: Counselor Progress Controller ===");
        System.out.println("Total students found: " + students.size());
        
        for (User student : students) {
            Long studentId = student.getId();
            
            System.out.println("Processing student: " + student.getFullName() + " (ID: " + studentId + ")");
            
            SelfCareModuleProgressService.ProgressStatistics stats = 
                moduleProgressService.getUserProgressStatistics(studentId);
            
            System.out.println("  - Completed: " + stats.getCompletedCount());
            System.out.println("  - Average Progress: " + stats.getAverageProgress() + "%");
            System.out.println("  - In Progress: " + stats.getInProgressCount());
                
            StudentProgressSummary summary = new StudentProgressSummary();
            summary.studentId = studentId;
            summary.studentName = student.getFullName();
            summary.studentEmail = student.getEmail();
            summary.completedModules = stats.getCompletedCount();
            summary.averageProgress = stats.getAverageProgress();
            summary.totalModules = stats.getTotalModules();
            summary.inProgressCount = stats.getInProgressCount();
            progressList.add(summary);
        }
        
        System.out.println("Total progress summaries created: " + progressList.size());
        System.out.println("=== END DEBUG ===");
        
        model.addAttribute("studentProgressList", progressList);
        return "self-care/counselor-progress-overview";
    }

    @GetMapping("/student/{studentId}")
    public String viewStudentDetail(@PathVariable Long studentId, Model model) {
        User student = userService.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        
        // Student info
        model.addAttribute("student", student);
        
        // Progress statistics
        SelfCareModuleProgressService.ProgressStatistics stats = 
            moduleProgressService.getUserProgressStatistics(studentId);
        model.addAttribute("statistics", stats);
        
        // Get all modules for recommendation section
        List<SelfCareModule> allModules = moduleRepository.findAll();
        model.addAttribute("allModules", allModules);
        
        // Get student's progress details with module info
        List<UserModuleProgress> progresses = progressRepository.findByUserId(studentId);
        List<ModuleProgressDetail> progressDetails = new ArrayList<>();
        
        for (UserModuleProgress progress : progresses) {
            moduleRepository.findById(progress.getModuleId()).ifPresent(module -> {
                ModuleProgressDetail detail = new ModuleProgressDetail();
                detail.module = module;
                detail.progress = progress;
                progressDetails.add(detail);
            });
        }
        
        model.addAttribute("progressDetails", progressDetails);
        
        return "self-care/counselor-student-progress-detail";
    }

    
    public static class StudentProgressSummary {
        public Long studentId;
        public String studentName;
        public String studentEmail;
        public Integer completedModules;
        public Integer averageProgress;
        public Integer totalModules;
        public Integer inProgressCount;

        // Getters
        public Long getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getStudentEmail() { return studentEmail; }
        public Integer getCompletedModules() { return completedModules; }
        public Integer getAverageProgress() { return averageProgress; }
        public Integer getTotalModules() { return totalModules; }
        public Integer getInProgressCount() { return inProgressCount; }
        
        // Setters
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
        public void setCompletedModules(Integer completedModules) { this.completedModules = completedModules; }
        public void setAverageProgress(Integer averageProgress) { this.averageProgress = averageProgress; }
        public void setTotalModules(Integer totalModules) { this.totalModules = totalModules; }
        public void setInProgressCount(Integer inProgressCount) { this.inProgressCount = inProgressCount; }
    }
    
    // Helper class for displaying module + progress together
    public static class ModuleProgressDetail {
        public SelfCareModule module;
        public UserModuleProgress progress;
        
        public SelfCareModule getModule() { return module; }
        public UserModuleProgress getProgress() { return progress; }
    }
}