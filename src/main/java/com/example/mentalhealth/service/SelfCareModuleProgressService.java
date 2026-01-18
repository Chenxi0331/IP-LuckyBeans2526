package com.example.mentalhealth.service;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SelfCareModuleProgressService {
    
    @Autowired
    private UserModuleProgressRepository progressRepository;
    
    @Autowired
    private SelfCareModuleRepository moduleRepository;

    @Autowired
    private ModuleQuizRepository quizRepository;
    
    // ==================== UC011: Access Self-Care Modules ====================
    
    public List<SelfCareModule> getModulesWithProgress(Long userId, String category) {
    
        List<SelfCareModule> modules;
        if (category != null && !category.equals("all")) {
            modules = moduleRepository.findByCategory(category);
        } else {
            modules = moduleRepository.findAll();
        }
        
        return modules.stream().map(module -> {
            
            SelfCareModule moduleWithProgress = new SelfCareModule();
            moduleWithProgress.setModuleId(module.getModuleId());
            moduleWithProgress.setTitle(module.getTitle());
            moduleWithProgress.setDescription(module.getDescription());
            moduleWithProgress.setCategory(module.getCategory());
            moduleWithProgress.setLevel(module.getLevel());
            moduleWithProgress.setDuration(module.getDuration());
            moduleWithProgress.setIcon(module.getIcon());
            moduleWithProgress.setIsLocked(module.getIsLocked());
            moduleWithProgress.setContentType(module.getContentType());
            moduleWithProgress.setVideoUrl(module.getVideoUrl());
            moduleWithProgress.setVideoFilePath(module.getVideoFilePath());
            moduleWithProgress.setPdfFilePath(module.getPdfFilePath());
            moduleWithProgress.setTextContent(module.getTextContent());

            
            Optional<UserModuleProgress> progressOpt = progressRepository
                .findByUserIdAndModuleId(userId, module.getModuleId());
            
            if (progressOpt.isPresent()) {
                UserModuleProgress progress = progressOpt.get();
                moduleWithProgress.setProgress(progress.getProgressPercentage());
                moduleWithProgress.setCompleted("completed".equals(progress.getStatus()));
            } else {
                moduleWithProgress.setProgress(0);
                moduleWithProgress.setCompleted(false);
            }
            
            return moduleWithProgress;
        }).collect(Collectors.toList());
    }
    
   
    public SelfCareModule getModuleById(Integer moduleId) {
        return moduleRepository.findById(moduleId)
            .orElseThrow(() -> new RuntimeException("Module not found: " + moduleId));
    }
    
    @Transactional
    public UserModuleProgress startModule(Long userId, Integer moduleId) {
    
        Optional<UserModuleProgress> existingProgress = progressRepository
            .findByUserIdAndModuleId(userId, moduleId);
        
        if (existingProgress.isPresent()) {
    
            UserModuleProgress progress = existingProgress.get();
            progress.setLastAccessed(LocalDateTime.now());
            return progressRepository.save(progress);
        }
        
        UserModuleProgress newProgress = new UserModuleProgress();
        newProgress.setUserId(userId);
        newProgress.setModuleId(moduleId);
        newProgress.setProgressPercentage(0);
        newProgress.setStatus("in_progress");
        newProgress.setStartDate(LocalDateTime.now());
        newProgress.setLastAccessed(LocalDateTime.now());
        
        return progressRepository.save(newProgress);
    }
    
    public List<UserModuleProgress> getUserProgressList(Long userId) {
    return progressRepository.findByUserId(userId);
}

public UserModuleProgress getOrCreateProgress(Long userId, Integer moduleId) {
    return progressRepository.findByUserIdAndModuleId(userId, moduleId)
            .orElseGet(() -> {
                UserModuleProgress newProgress = new UserModuleProgress();
                newProgress.setUserId(userId);
                newProgress.setModuleId(moduleId);
                newProgress.setProgressPercentage(0);
                newProgress.setStatus("not-started");
                newProgress.setStartDate(LocalDateTime.now());
                newProgress.setLastAccessed(LocalDateTime.now());
                return progressRepository.save(newProgress);
            });
}

@Transactional
public void updateProgress(Long userId, Integer moduleId, Integer progress) {
    UserModuleProgress userProgress = getOrCreateProgress(userId, moduleId);
    userProgress.setProgressPercentage(progress);
    userProgress.setLastAccessed(LocalDateTime.now());
    
    if (progress >= 100) {
        userProgress.setStatus("completed");
        userProgress.setCompletionDate(LocalDateTime.now());
    } else if (progress > 0) {
        userProgress.setStatus("in-progress");
    }
    
    progressRepository.save(userProgress);
}

public ProgressStatistics getUserProgressStatistics(Long userId) {
    List<UserModuleProgress> progresses = progressRepository.findByUserId(userId);
    List<SelfCareModule> allModules = moduleRepository.findAll();
    
    ProgressStatistics stats = new ProgressStatistics();
    stats.setTotalModules(allModules.size());
    
    int completed = (int) progresses.stream()
            .filter(p -> "completed".equalsIgnoreCase(p.getStatus()))
            .count();
    int inProgress = (int) progresses.stream()
            .filter(p -> "in-progress".equalsIgnoreCase(p.getStatus()) || 
                         "in_progress".equalsIgnoreCase(p.getStatus()))
            .count();
            
    stats.setCompletedCount(completed);
    stats.setInProgressCount(inProgress);
    
    if (progresses.isEmpty()) {
        stats.setAverageProgress(0);
        stats.setPercentage(0);
    } else {
        double avg = progresses.stream()
                .mapToInt(UserModuleProgress::getProgressPercentage)
                .average()
                .orElse(0.0);
        stats.setAverageProgress((int) avg);

        // IMPORTANT: Calculate percentage correctly
        int percentage = allModules.isEmpty() ? 0 : (completed * 100 / allModules.size());
        stats.setPercentage(percentage);
    }
    
    return stats;
}
    // Modify updateProgress to work with content + quiz completion
//     public UserModuleProgress updateProgress(Long userId, Integer moduleId, Integer percentage) {
//     UserModuleProgress progress = progressRepository
//         .findByUserIdAndModuleId(userId, moduleId)
//         .orElseThrow();
    
//     progress.setProgressPercentage(percentage);
    
//     if (percentage >= 100) {
//         progress.setStatus("completed");
//         progress.setCompletionDate(LocalDateTime.now());
//     }
    
//     return progressRepository.save(progress);
// }
    
@Transactional
public void markContentCompleted(Long userId, Integer moduleId) {
    UserModuleProgress progress = getOrCreateProgress(userId, moduleId);
    progress.setContentCompleted(true);

    Long quizCount = quizRepository.countByModuleIdAndStatus(moduleId, "APPROVED");
    boolean hasQuiz = quizCount > 0;

    int percentage;
    if (hasQuiz) {
        // If has quiz: content = 50%, need quiz to reach 100%
        percentage = progress.getQuizCompleted() ? 100 : 50;
    } else {
        // If no quiz: content completion = 100%
        percentage = 100;
    }
    
    progress.setProgressPercentage(percentage);
    
    if (percentage == 100) {
        progress.setStatus("completed");
        progress.setCompletionDate(LocalDateTime.now());
    } else {
        progress.setStatus("in-progress");
    }
    progress.setLastAccessed(LocalDateTime.now());
    progressRepository.save(progress);
}


@Transactional
public void markQuizCompleted(Long userId, Integer moduleId) {
    UserModuleProgress progress = getOrCreateProgress(userId, moduleId);
    progress.setQuizCompleted(true);

    int percentage = progress.getContentCompleted() ? 100 : 50;
    progress.setProgressPercentage(percentage);
    
    if (percentage == 100) {
        progress.setStatus("completed");
        progress.setCompletionDate(LocalDateTime.now());
    } else {
        progress.setStatus("in-progress");
    }
    progress.setLastAccessed(LocalDateTime.now());
    progressRepository.save(progress);
}

    @Transactional
    public void resetProgress(Long userId, Integer moduleId) {
        Optional<UserModuleProgress> progressOpt = progressRepository
            .findByUserIdAndModuleId(userId, moduleId);
        
        if (progressOpt.isPresent()) {
            UserModuleProgress progress = progressOpt.get();
            progress.setProgressPercentage(0);
            progress.setStatus("in_progress");
            progress.setCompletionDate(null);
            progress.setLastAccessed(LocalDateTime.now());
            progressRepository.save(progress);
        }
    }
    
    // ==================== UC012: Track Self-Care Progress ====================
    public List<UserModuleProgress> getUserProgress(Long userId) {
        return progressRepository.findByUserId(userId);
    }

    public ProgressStatistics getProgressStatistics(Long userId) {
        Long completed = progressRepository.countCompletedModules(userId);
       
        Double avgProgress = progressRepository.getAverageProgress(userId);
        
        List<UserModuleProgress> allProgress = progressRepository.findByUserId(userId);
        
        long inProgressCount = allProgress.stream()
            .filter(p -> "in_progress".equals(p.getStatus()))
            .count();
        
        int totalModules = (int) moduleRepository.count();
        
        ProgressStatistics stats = new ProgressStatistics();
        stats.setCompletedCount(completed != null ? completed.intValue() : 0);
        stats.setAverageProgress(avgProgress != null ? avgProgress.intValue() : 0);
        stats.setTotalModules(totalModules);
        stats.setInProgressCount((int) inProgressCount);
         
        int percentage = 0;
        if (totalModules > 0) {
        percentage = (int) ((completed != null ? completed : 0) * 100 / totalModules);
        }
        stats.setPercentage(percentage);
        return stats;
    }
    
    // ==================== UC014: Manage Modules (Admin) ====================
    
    public List<SelfCareModule> getAllModules() {
        return moduleRepository.findAll();
    }
    
    @Transactional
    public SelfCareModule addModule(SelfCareModule module) {
        return moduleRepository.save(module);
    }

    @Transactional
    public SelfCareModule updateModule(Integer moduleId, SelfCareModule moduleData) {
        SelfCareModule module = moduleRepository.findById(moduleId)
            .orElseThrow(() -> new RuntimeException("Module not found: " + moduleId));
        
        module.setTitle(moduleData.getTitle());
        module.setDescription(moduleData.getDescription());
        module.setCategory(moduleData.getCategory());
        module.setLevel(moduleData.getLevel());
        module.setDuration(moduleData.getDuration());
        module.setIcon(moduleData.getIcon());
        module.setIsLocked(moduleData.getIsLocked());
        
        return moduleRepository.save(module);
    }
    
    @Transactional
    public void deleteModule(Integer moduleId) {
        moduleRepository.deleteById(moduleId);
    }

    @Transactional
    public SelfCareModule toggleModuleLock(Integer moduleId) {
        SelfCareModule module = moduleRepository.findById(moduleId)
            .orElseThrow(() -> new RuntimeException("Module not found: " + moduleId));
        
        module.setIsLocked(!module.getIsLocked());
        return moduleRepository.save(module);
    }
    
    // ==================== DTO Classes ====================
   
    public static class ProgressStatistics {
        private Integer completedCount;
        private Integer averageProgress;
        private Integer totalModules;
        private Integer inProgressCount;
        private Integer percentage;

        public Integer getPercentage() {
            return percentage;
        }

        public void setPercentage(Integer percentage) {
            this.percentage = percentage;
        }

        // Getters and Setters
        public Integer getCompletedCount() { return completedCount; }
        public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }
        
        public Integer getAverageProgress() { return averageProgress; }
        public void setAverageProgress(Integer averageProgress) { this.averageProgress = averageProgress; }
        
        public Integer getTotalModules() { return totalModules; }
        public void setTotalModules(Integer totalModules) { this.totalModules = totalModules; }
        
        public Integer getInProgressCount() { return inProgressCount; }
        public void setInProgressCount(Integer inProgressCount) { this.inProgressCount = inProgressCount; }
    }
}