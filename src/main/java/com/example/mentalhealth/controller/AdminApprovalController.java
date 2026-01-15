package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.repository.*;
import com.example.mentalhealth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/approvals")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminApprovalController {
    
    @Autowired
    private ModuleEditRequestRepository editRequestRepository;
    
    @Autowired
    private ModuleQuizRepository quizRepository;
    
    @Autowired
    private SelfCareModuleRepository moduleRepository;
    
    @Autowired
    private ModuleEditRequestQuizRepository requestQuizRepository;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/module-requests")
    public String listModuleRequests(Model model) {
        List<ModuleEditRequest> pendingRequests = editRequestRepository
            .findByStatusOrderByRequestedAtDesc("PENDING");
        
        List<ModuleEditRequest> allRequests = editRequestRepository.findAll();
        
        Map<Long, String> userNames = new HashMap<>();
    for (ModuleEditRequest req : allRequests) {
        if (!userNames.containsKey(req.getRequestedBy())) {
            userService.findById(req.getRequestedBy()).ifPresent(user -> 
                userNames.put(req.getRequestedBy(), user.getFullName())
            );
        }
    }
        
        long pendingCount = pendingRequests.size();
        long approvedCount = editRequestRepository.findByStatusOrderByRequestedAtDesc("APPROVED").size();
        long rejectedCount = editRequestRepository.findByStatusOrderByRequestedAtDesc("REJECTED").size();
        
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("allRequests", allRequests);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        
        return "self-care/admin-module-requests";
    }
    
    @GetMapping("/module-requests/{id}")
    public String viewModuleRequest(@PathVariable Integer id, Model model) {
        ModuleEditRequest request = editRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Request not found"));
        
        List<ModuleEditRequestQuiz> quizzes = requestQuizRepository
            .findByRequestIdOrderByQuestionOrderAsc(id);
        
        model.addAttribute("request", request);
        model.addAttribute("quizzes", quizzes);
        
        return "self-care/admin-module-request-detail";
    }
    
    @PostMapping("/module-requests/{id}/approve")
    @Transactional
    public String approveModuleRequest(@PathVariable Integer id,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        
        User admin = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        ModuleEditRequest request = editRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Request not found"));
        
        SelfCareModule module = null;
        int quizCount = 0;
        
        if ("CREATE".equals(request.getRequestType())) {
            module = new SelfCareModule();
            copyRequestToModule(request, module);
            module.setIsLocked(false);
            module = moduleRepository.save(module);
            
            List<ModuleEditRequestQuiz> requestQuizzes = requestQuizRepository
                .findByRequestIdOrderByQuestionOrderAsc(request.getRequestId());
            
            for (ModuleEditRequestQuiz tempQuiz : requestQuizzes) {
                ModuleQuiz quiz = new ModuleQuiz();
                quiz.setModuleId(module.getModuleId());
                quiz.setQuestionText(tempQuiz.getQuestionText());
                quiz.setOptionA(tempQuiz.getOptionA());
                quiz.setOptionB(tempQuiz.getOptionB());
                quiz.setOptionC(tempQuiz.getOptionC());
                quiz.setOptionD(tempQuiz.getOptionD());
                quiz.setCorrectAnswer(tempQuiz.getCorrectAnswer());
                quiz.setQuestionOrder(tempQuiz.getQuestionOrder());
                quiz.setStatus("APPROVED");
                quiz.setCreatedBy(request.getRequestedBy());
                quiz.setCreatedAt(LocalDateTime.now());
                quiz.setApprovedBy(admin.getUserId().longValue());
                quiz.setApprovedAt(LocalDateTime.now());
                
                quizRepository.save(quiz);
                quizCount++;
            }
            
            requestQuizRepository.deleteByRequestId(request.getRequestId());
            
        } else if ("UPDATE".equals(request.getRequestType())) {
            module = moduleRepository.findById(request.getModuleId())
                .orElseThrow(() -> new RuntimeException("Module not found"));
            copyRequestToModule(request, module);
            moduleRepository.save(module);
        }
        
        request.setStatus("APPROVED");
        request.setReviewedBy(admin.getUserId().longValue());
        request.setReviewedAt(LocalDateTime.now());
        editRequestRepository.save(request);
        
        String message = "CREATE".equals(request.getRequestType()) 
            ? "Module request approved successfully with " + quizCount + " quiz questions!"
            : "Module request approved successfully!";
        
        redirectAttributes.addFlashAttribute("message", message);
        redirectAttributes.addFlashAttribute("messageType", "success");
        
        return "redirect:/admin/approvals/module-requests";
    }
    
    @PostMapping("/module-requests/{id}/reject")
    @Transactional
    public String rejectModuleRequest(@PathVariable Integer id,
                                     @RequestParam String reviewNotes,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        
        User admin = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        ModuleEditRequest request = editRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Request not found"));
        
        request.setStatus("REJECTED");
        request.setReviewedBy(admin.getUserId().longValue());
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNotes(reviewNotes);
        editRequestRepository.save(request);
        
        requestQuizRepository.deleteByRequestId(id);
        
        redirectAttributes.addFlashAttribute("message", "Module request rejected.");
        redirectAttributes.addFlashAttribute("messageType", "info");
        
        return "redirect:/admin/approvals/module-requests";
    }
    
    @GetMapping("/quiz-questions")
    public String listQuizQuestions(Model model) {
        List<ModuleQuiz> pendingQuizzes = quizRepository
            .findByStatusOrderByCreatedAtDesc("PENDING");
        
        List<ModuleQuiz> allQuizzes = quizRepository.findAll();
        
        long pendingCount = pendingQuizzes.size();
        long approvedCount = quizRepository.findByStatusOrderByCreatedAtDesc("APPROVED").size();
        long rejectedCount = quizRepository.findByStatusOrderByCreatedAtDesc("REJECTED").size();
        
        model.addAttribute("pendingQuizzes", pendingQuizzes);
        model.addAttribute("allQuizzes", allQuizzes);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        
        return "self-care/admin-quiz-approvals";
    }
    
    @PostMapping("/quiz-questions/{id}/approve")
    public String approveQuizQuestion(@PathVariable Integer id,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        
        User admin = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        ModuleQuiz quiz = quizRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        quiz.setStatus("APPROVED");
        quiz.setApprovedBy(admin.getUserId().longValue());
        quiz.setApprovedAt(LocalDateTime.now());
        quizRepository.save(quiz);
        
        redirectAttributes.addFlashAttribute("message", "Quiz question approved!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        
        return "redirect:/admin/approvals/quiz-questions";
    }
    
    @PostMapping("/quiz-questions/{id}/reject")
    public String rejectQuizQuestion(@PathVariable Integer id,
                                    RedirectAttributes redirectAttributes) {
        
        ModuleQuiz quiz = quizRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        quiz.setStatus("REJECTED");
        quizRepository.save(quiz);
        
        redirectAttributes.addFlashAttribute("message", "Quiz question rejected.");
        redirectAttributes.addFlashAttribute("messageType", "info");
        
        return "redirect:/admin/approvals/quiz-questions";
    }
    
    private void copyRequestToModule(ModuleEditRequest request, SelfCareModule module) {
        module.setTitle(request.getTitle());
        module.setDescription(request.getDescription());
        module.setCategory(request.getCategory());
        module.setLevel(request.getLevel());
        module.setDuration(request.getDuration());
        module.setIcon(request.getIcon());
        module.setContentType(request.getContentType());
        module.setVideoUrl(request.getVideoUrl());
        module.setVideoFilePath(request.getVideoFilePath());
        module.setPdfFilePath(request.getPdfFilePath());
        module.setTextContent(request.getTextContent());
    }
}