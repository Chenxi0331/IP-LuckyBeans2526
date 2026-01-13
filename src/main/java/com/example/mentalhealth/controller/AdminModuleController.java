package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.SelfCareModule;
import com.example.mentalhealth.service.SelfCareModuleProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Admin Module Controller
 * UC014: Manage Modules (Admin)
 */
@Controller
@RequestMapping("/admin/modules")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminModuleController {
    
    @Autowired
    private SelfCareModuleProgressService moduleProgressService;
    
    private static final String UPLOAD_DIR = "uploads/";
    
    /**
     * UC014: View all modules list
     */
    @GetMapping
    public String listModules(Model model) {
        List<SelfCareModule> modules = moduleProgressService.getAllModules();
        model.addAttribute("modules", modules);
        
        return "self-care/admin-module-list";
    }
    
    /**
     * UC014: Show add module form
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("module", new SelfCareModule());
        
        return "self-care/admin-module-form";
    }
    
    /**
     * UC014: Save new module
     */
    @PostMapping("/add")
    public String addModule(@ModelAttribute SelfCareModule module,
                           @RequestParam(required = false) MultipartFile videoFile,
                           @RequestParam(required = false) MultipartFile pdfFile) throws IOException {
        
        // Handle file uploads based on content type
        if (videoFile != null && !videoFile.isEmpty()) {
            String videoPath = saveFile(videoFile, "videos");
            module.setVideoFilePath(videoPath);
            module.setContentType("video_file");
        } else if (pdfFile != null && !pdfFile.isEmpty()) {
            String pdfPath = saveFile(pdfFile, "pdfs");
            module.setPdfFilePath(pdfPath);
            module.setContentType("pdf");
        } else if (module.getVideoUrl() != null && !module.getVideoUrl().isEmpty()) {
            module.setContentType("video_url");
        } else if (module.getTextContent() != null && !module.getTextContent().isEmpty()) {
            module.setContentType("text");
        }
        
        moduleProgressService.addModule(module);
        
        return "redirect:/admin/modules";
    }
    
    /**
     * UC014: Show edit module form
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        SelfCareModule module = moduleProgressService.getModuleById(id);
        model.addAttribute("module", module);
        
        return "self-care/admin-module-form";
    }
    
    /**
     * UC014: Update module
     */
    @PostMapping("/edit/{id}")
    public String updateModule(@PathVariable Integer id, 
                              @ModelAttribute SelfCareModule module,
                              @RequestParam(required = false) MultipartFile videoFile,
                              @RequestParam(required = false) MultipartFile pdfFile) throws IOException {
        
        SelfCareModule existingModule = moduleProgressService.getModuleById(id);
        
        // Handle file uploads
        if (videoFile != null && !videoFile.isEmpty()) {
            String videoPath = saveFile(videoFile, "videos");
            module.setVideoFilePath(videoPath);
            module.setContentType("video_file");
        } else if (pdfFile != null && !pdfFile.isEmpty()) {
            String pdfPath = saveFile(pdfFile, "pdfs");
            module.setPdfFilePath(pdfPath);
            module.setContentType("pdf");
        } else if (module.getVideoUrl() != null && !module.getVideoUrl().isEmpty()) {
            module.setContentType("video_url");
        } else if (module.getTextContent() != null && !module.getTextContent().isEmpty()) {
            module.setContentType("text");
        } else {
            // Keep existing content if no new content provided
            module.setVideoFilePath(existingModule.getVideoFilePath());
            module.setPdfFilePath(existingModule.getPdfFilePath());
            module.setVideoUrl(existingModule.getVideoUrl());
            module.setTextContent(existingModule.getTextContent());
            module.setContentType(existingModule.getContentType());
        }
        
        moduleProgressService.updateModule(id, module);
        
        return "redirect:/admin/modules";
    }
    
    /**
     * UC014: Delete module
     */
    @GetMapping("/delete/{id}")
    public String deleteModule(@PathVariable Integer id) {
        moduleProgressService.deleteModule(id);
        
        return "redirect:/admin/modules";
    }
    
    /**
     * UC014: Toggle module lock status
     */
    @GetMapping("/toggle-lock/{id}")
    public String toggleLock(@PathVariable Integer id) {
        moduleProgressService.toggleModuleLock(id);
        
        return "redirect:/admin/modules";
    }
    
    /**
     * Helper method to save uploaded files
     * Returns path without leading slash (e.g., "uploads/videos/filename.mp4")
     */
    private String saveFile(MultipartFile file, String subDir) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String uploadPath = UPLOAD_DIR + subDir + "/";
        
        Path path = Paths.get(uploadPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        
        Path filePath = path.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);
        
        // Return path WITHOUT leading slash for proper URL mapping
        return uploadPath + fileName;
    }
}