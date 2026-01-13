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
    
    // ==================== UC011: Access Self-Care Modules ====================
    
    public List<SelfCareModule> getModulesWithProgress(Integer userId, String category) {
    
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
    
    /**
     * 获取单个模块详情
     */
    public SelfCareModule getModuleById(Integer moduleId) {
        return moduleRepository.findById(moduleId)
            .orElseThrow(() -> new RuntimeException("Module not found: " + moduleId));
    }
    
    /**
     * 开始一个模块（创建或更新进度记录）
     */
    @Transactional
    public UserModuleProgress startModule(Integer userId, Integer moduleId) {
        // 检查是否已存在进度记录
        Optional<UserModuleProgress> existingProgress = progressRepository
            .findByUserIdAndModuleId(userId, moduleId);
        
        if (existingProgress.isPresent()) {
            // 如果已存在，只更新最后访问时间
            UserModuleProgress progress = existingProgress.get();
            progress.setLastAccessed(LocalDateTime.now());
            return progressRepository.save(progress);
        }
        
        // 创建新的进度记录
        UserModuleProgress newProgress = new UserModuleProgress();
        newProgress.setUserId(userId);
        newProgress.setModuleId(moduleId);
        newProgress.setProgressPercentage(0);
        newProgress.setStatus("in_progress");
        newProgress.setStartDate(LocalDateTime.now());
        newProgress.setLastAccessed(LocalDateTime.now());
        
        return progressRepository.save(newProgress);
    }
    
    /**
     * 更新模块进度
     */
    @Transactional
    public UserModuleProgress updateProgress(Integer userId, Integer moduleId, Integer percentage) {
        // 验证进度百分比
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Progress percentage must be between 0 and 100");
        }
        
        // 查找进度记录
        UserModuleProgress progress = progressRepository
            .findByUserIdAndModuleId(userId, moduleId)
            .orElseThrow(() -> new RuntimeException("Progress not found"));
        
        // 更新进度百分比
        progress.setProgressPercentage(percentage);
        
        // 如果达到 100%，标记为完成
        if (percentage >= 100) {
            progress.setStatus("completed");
            if (progress.getCompletionDate() == null) {
                progress.setCompletionDate(LocalDateTime.now());
            }
        } else {
            progress.setStatus("in_progress");
        }
        
        progress.setLastAccessed(LocalDateTime.now());
        
        return progressRepository.save(progress);
    }
    
    /**
     * 重置模块进度
     */
    @Transactional
    public void resetProgress(Integer userId, Integer moduleId) {
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
    public List<UserModuleProgress> getUserProgress(Integer userId) {
        return progressRepository.findByUserId(userId);
    }
    
    public ProgressStatistics getProgressStatistics(Integer userId) {
    
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