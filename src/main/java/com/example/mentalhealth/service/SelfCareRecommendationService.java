package com.example.mentalhealth.service;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.mentalhealth.model.SelfCareModule;
import java.util.ArrayList;
import java.util.List;

@Service
public class SelfCareRecommendationService {
    
    @Autowired
    private UserModuleProgressRepository progressRepository;
    
    @Autowired
    private SelfCareModuleRepository moduleRepository;
    
    @Autowired
    private CounselorRecommendationRepository counselorRecommendationRepository;
    
    /**
     * Generate personalized recommendations (max 3)
     * Priority: 1. Counselor recommendations, 2. In-progress, 3. New modules
     */
    public List<Recommendation> getPersonalizedRecommendations(Integer userId) {
        List<Recommendation> recommendations = new ArrayList<>();
        
        // Strategy 1: Add counselor recommendations first
        addCounselorRecommendations(userId.longValue(), recommendations);
        
        // Strategy 2: Add in-progress modules
        if (recommendations.size() < 3) {
            addInProgressRecommendations(userId, recommendations);
        }
        
        // Strategy 3: Add new module recommendations
        if (recommendations.size() < 3) {
            addNewModuleRecommendations(userId, recommendations);
        }
        
        return recommendations;
    }
    
    /**
     * Add counselor recommendations
     */
    private void addCounselorRecommendations(Long userId, List<Recommendation> recommendations) {
        List<CounselorRecommendation> counselorRecs = counselorRecommendationRepository
            .findByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
        
        for (CounselorRecommendation rec : counselorRecs) {
            if (recommendations.size() >= 3) break;
            
            moduleRepository.findById(rec.getModuleId()).ifPresent(module -> {
                // Check progress
                Integer progress = 0;
                progressRepository.findByUserIdAndModuleId(userId.intValue(), module.getModuleId())
                    .ifPresent(p -> {
                        // This is handled inside ifPresent, no need to assign
                    });
                
                Recommendation recommendation = new Recommendation();
                recommendation.setIcon("👨‍⚕️");
                recommendation.setTitle(module.getTitle());
                recommendation.setDescription(rec.getRecommendationReason());
                recommendation.setBasedOn("Recommended by your counselor");
                recommendation.setLevel(module.getLevel());
                recommendation.setDuration(module.getDuration());
                recommendation.setProgress(progress);
                recommendation.setButtonText("Start Module");
                recommendation.setModuleId(module.getModuleId());
                recommendation.setIsAI(false);
                recommendation.setAiBadge("Counselor");
                recommendation.setIsLocked(module.getIsLocked());  // Add this line
                recommendations.add(recommendation);
                recommendation.setContentType(module.getContentType());
            });
        }
    }
    
    /**
     * Add in-progress module recommendations
     */
    private void addInProgressRecommendations(Integer userId, List<Recommendation> recommendations) {
        List<UserModuleProgress> inProgressList = progressRepository
            .findByUserIdAndStatus(userId, "in_progress");
        
        for (UserModuleProgress progress : inProgressList) {
            if (recommendations.size() >= 3) break;
            
            moduleRepository.findById(progress.getModuleId()).ifPresent(module -> {
                Recommendation rec = new Recommendation();
                rec.setIcon(module.getIcon());
                rec.setTitle(module.getTitle());
                rec.setDescription("You're " + progress.getProgressPercentage() 
                    + "% through - great time to continue!");
                rec.setBasedOn("In-progress module");
                rec.setLevel(module.getLevel());
                rec.setDuration(module.getDuration());
                rec.setProgress(progress.getProgressPercentage());
                rec.setButtonText("Continue Module");
                rec.setModuleId(module.getModuleId());
                rec.setIsAI(true);
                rec.setAiBadge("AI");
                rec.setIsLocked(module.getIsLocked());  // Add this line
                recommendations.add(rec);
                rec.setContentType(module.getContentType());
            });
        }
    }
    
    /**
     * Add new module recommendations
     */
    private void addNewModuleRecommendations(Integer userId, List<Recommendation> recommendations) {
        List<SelfCareModule> unlockedModules = moduleRepository
            .findByIsLockedOrderByModuleIdAsc(false);
        
        for (SelfCareModule module : unlockedModules) {
            if (recommendations.size() >= 3) break;
            
            boolean isStarted = progressRepository
                .findByUserIdAndModuleId(userId, module.getModuleId())
                .isPresent();
            
            if (!isStarted) {
                Recommendation rec = new Recommendation();
                rec.setIcon(module.getIcon());
                rec.setTitle(module.getTitle());
                rec.setDescription("This practice could be beneficial for you");
                rec.setBasedOn("Recommended based on your profile");
                rec.setLevel(module.getLevel());
                rec.setDuration(module.getDuration());
                rec.setProgress(0);
                rec.setButtonText("Start Module");
                rec.setModuleId(module.getModuleId());
                rec.setIsAI(true);
                rec.setAiBadge("AI");
                rec.setIsLocked(module.getIsLocked());  // Add this line
                recommendations.add(rec);
                rec.setContentType(module.getContentType());
            }
        }
    }
    
    /**
     * Counselor recommends a module to student
     */
    @Transactional
    public CounselorRecommendation recommendModule(Long counselorId, Long studentId, 
                                                    Integer moduleId, String reason) {
        // Check if module exists and is not locked
        SelfCareModule module = moduleRepository.findById(moduleId)
            .orElseThrow(() -> new RuntimeException("Module not found"));
        
        if (module.getIsLocked()) {
            throw new RuntimeException("Cannot recommend a locked module");
        }
        
        // Check if already recommended
        if (counselorRecommendationRepository.existsByStudentIdAndModuleIdAndIsActiveTrue(studentId, moduleId)) {
            throw new RuntimeException("This module is already recommended to this student");
        }
        
        CounselorRecommendation recommendation = new CounselorRecommendation();
        recommendation.setCounselorId(counselorId);
        recommendation.setStudentId(studentId);
        recommendation.setModuleId(moduleId);
        recommendation.setRecommendationReason(reason);
        
        return counselorRecommendationRepository.save(recommendation);
    }
    
    /**
     * Remove counselor recommendation
     */
    @Transactional
    public void removeRecommendation(Long recommendationId) {
        counselorRecommendationRepository.findById(recommendationId).ifPresent(rec -> {
            rec.setIsActive(false);
            counselorRecommendationRepository.save(rec);
        });
    }
}