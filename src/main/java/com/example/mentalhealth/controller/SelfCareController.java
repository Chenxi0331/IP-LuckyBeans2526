package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/self-care")
public class SelfCareController {
    
    @Autowired
    private SelfCareModuleProgressService moduleProgressService;
    
    @Autowired
    private SelfCareRecommendationService recommendationService;
    
    @Autowired
    private UserService userService;
    
    /**
     * UC011: Access Self-Care Modules
     */
    @GetMapping
    public String selfCare(@RequestParam(required = false) String category,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        
        User user = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Integer userId = user.getUserId();
        
        List<Recommendation> recommendations = recommendationService
            .getPersonalizedRecommendations(userId);

        List<SelfCareModule> modules = moduleProgressService
            .getModulesWithProgress(userId, category);
 
        SelfCareModuleProgressService.ProgressStatistics stats = moduleProgressService
            .getProgressStatistics(userId);
                           
        System.out.println("Recommendations size: " + (recommendations != null ? recommendations.size() : "null"));
        System.out.println("Modules size: " + (modules != null ? modules.size() : "null"));
        System.out.println("Stats: " + stats);
        
        model.addAttribute("user", user);
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("allModules", modules);
        model.addAttribute("overallProgress", stats);
        model.addAttribute("category", category != null ? category : "all");
        
        return "self-care/self-care";
    }
    
    /**
     * UC011: View Module Detail
     */
    @GetMapping("/module/{id}")
    public String viewModule(@PathVariable Integer id,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        
        User user = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Integer userId = user.getUserId();
        
        SelfCareModule module = moduleProgressService.getModuleById(id);
        
        // Check if module is locked
        if (module.getIsLocked()) {
            redirectAttributes.addFlashAttribute("error", 
                "This module is currently locked. Please contact your counselor for access.");
            return "redirect:/self-care";
        }
        
        UserModuleProgress progress = moduleProgressService.startModule(userId, id);
        
        model.addAttribute("module", module);
        model.addAttribute("progress", progress);
        
        return "self-care/module-detail";
    }
    
    /**
     * UC011: Update Module Progress
     */
    @PostMapping("/module/{id}/update-progress")
    @ResponseBody
    public String updateProgress(@PathVariable Integer id,
                                 @RequestParam Integer progress,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if module is locked
            SelfCareModule module = moduleProgressService.getModuleById(id);
            if (module.getIsLocked()) {
                return "{\"success\": false, \"message\": \"This module is locked\"}";
            }
            
            moduleProgressService.updateProgress(user.getUserId(), id, progress);
            
            return "{\"success\": true, \"message\": \"Progress updated successfully\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * UC011: Reset Module Progress
     */
    @PostMapping("/module/{id}/reset")
    @ResponseBody
    public String resetProgress(@PathVariable Integer id,
                               @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if module is locked
            SelfCareModule module = moduleProgressService.getModuleById(id);
            if (module.getIsLocked()) {
                return "{\"success\": false, \"message\": \"This module is locked\"}";
            }
            
            moduleProgressService.resetProgress(user.getUserId(), id);
            
            return "{\"success\": true, \"message\": \"Progress reset successfully\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}";
        }
    }
}