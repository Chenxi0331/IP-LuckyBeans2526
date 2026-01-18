package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.example.mentalhealth.repository.ModuleQuizRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Arrays; 

@Controller
@RequestMapping("/self-care")
public class SelfCareController {
    
    @Autowired
    private SelfCareModuleProgressService moduleProgressService;
    
    @Autowired
    private SelfCareRecommendationService recommendationService;
    
    @Autowired
    private UserService userService;

    @Autowired
    private ModuleQuizRepository quizRepository;
    @GetMapping
public String selfCare(@RequestParam(required = false) String category,
                      @AuthenticationPrincipal UserDetails userDetails,
                      Model model) {
    
    User user = userService.findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found"));
    
    Long userId = user.getId();
    
    // Fix 1: Use correct attribute names matching HTML
    model.addAttribute("allModules", moduleProgressService.getModulesWithProgress(userId, category));
    model.addAttribute("recommendations", recommendationService.getPersonalizedRecommendations(userId));
    
    // Fix 2: Add overallProgress object
    SelfCareModuleProgressService.ProgressStatistics stats = 
        moduleProgressService.getUserProgressStatistics(userId);
    model.addAttribute("overallProgress", stats);
    
    // Fix 3: Add category
    model.addAttribute("category", category != null ? category : "all");
    
    return "self-care/self-care";
}
    
    @GetMapping("/module/{id}")
    public String viewModule(@PathVariable Integer id,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        SelfCareModule module = moduleProgressService.getModuleById(id);
        if (module.getIsLocked()) {
            redirectAttributes.addFlashAttribute("error", "This module is locked.");
            return "redirect:/self-care";
        }
        Long quizCount = quizRepository.countByModuleIdAndStatus(id, "APPROVED");
        boolean hasQuiz = quizCount > 0;

        model.addAttribute("module", module);
        model.addAttribute("progress", moduleProgressService.getOrCreateProgress(user.getId(), id));
        model.addAttribute("hasQuiz", hasQuiz);
        return "self-care/module-detail";
    }
    
    @PostMapping("/module/{id}/update-progress")
    @ResponseBody
    public String updateProgress(@PathVariable Integer id, @RequestParam Integer progress,
                                @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.findByEmail(userDetails.getUsername()).get();
            moduleProgressService.updateProgress(user.getId(), id, progress);
            return "{\"success\": true, \"message\": \"Progress updated\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}";
        }
    }
    
    @PostMapping("/module/{id}/reset")
    @ResponseBody
    public String resetProgress(@PathVariable Integer id, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.findByEmail(userDetails.getUsername()).get();
            moduleProgressService.resetProgress(user.getId(), id);
            return "{\"success\": true, \"message\": \"Progress reset\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}";
        }
    }


}