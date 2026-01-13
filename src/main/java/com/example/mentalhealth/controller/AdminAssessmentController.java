package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.AssessmentQuestion;
import com.example.mentalhealth.model.AssessmentOption;
import com.example.mentalhealth.model.UserAssessment;
import com.example.mentalhealth.service.AssessmentService;
import com.example.mentalhealth.repository.AssessmentQuestionRepository;
import com.example.mentalhealth.repository.AssessmentOptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Controller
@RequestMapping("/admin/assessment")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAssessmentController {
    
    @Autowired
    private AssessmentService assessmentService;
    
    @Autowired
    private AssessmentQuestionRepository questionRepository;
    
    @Autowired
    private AssessmentOptionRepository optionRepository;
    
    @GetMapping
    public String redirectToSummary() {
        return "redirect:/admin/assessment/summary";
    }

    @GetMapping("/summary")
    public String viewAssessmentSummary(Model model) {
        AssessmentService.AssessmentStatistics stats = 
            assessmentService.getAssessmentStatistics();
        
        model.addAttribute("statistics", stats);
        return "symptoms-recognition/admin-assessment-summary";
    }
    
    @GetMapping("/all")
    public String viewAllAssessments(Model model) {
        List<UserAssessment> allAssessments = assessmentService.getAllAssessments();
        model.addAttribute("assessments", allAssessments);
        return "symptoms-recognition/admin-assessment-all";
    }
    
    @GetMapping("/questions")
    public String listQuestions(Model model) {
        List<AssessmentQuestion> questions = questionRepository.findAllByOrderByQuestionOrderAsc();
        model.addAttribute("questions", questions);
        return "symptoms-recognition/admin-questions-list";
    }
    
    @GetMapping("/questions/add")
    public String showAddForm(Model model) {
        model.addAttribute("question", new AssessmentQuestion());
        return "symptoms-recognition/admin-question-form";
    }
    
    @PostMapping("/questions/add")
    @Transactional
    public String addQuestion(@ModelAttribute AssessmentQuestion question,
                            @RequestParam List<String> optionTexts,
                            RedirectAttributes redirectAttributes) {
        try {
            long maxOrder = questionRepository.count();
            question.setQuestionOrder((int) maxOrder + 1);
            
            AssessmentQuestion savedQuestion = questionRepository.save(question);
            
            for (String optionText : optionTexts) {
                AssessmentOption option = new AssessmentOption();
                option.setQuestionId(savedQuestion.getQuestionId());
                option.setOptionText(optionText);
                optionRepository.save(option);
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Question added successfully!");
            return "redirect:/admin/assessment/questions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to add question: " + e.getMessage());
            return "redirect:/admin/assessment/questions/add";
        }
    }
    
    @GetMapping("/questions/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        AssessmentQuestion question = questionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Question not found"));
        model.addAttribute("question", question);
        return "symptoms-recognition/admin-question-form";
    }
    
    @PostMapping("/questions/edit/{id}")
    @Transactional
    public String updateQuestion(@PathVariable Integer id,
                               @ModelAttribute AssessmentQuestion question,
                               @RequestParam List<String> optionTexts,
                               RedirectAttributes redirectAttributes) {
        try {
            AssessmentQuestion existing = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
            
            existing.setQuestionText(question.getQuestionText());
            questionRepository.save(existing);
            
            List<AssessmentOption> oldOptions = optionRepository.findByQuestionId(id);
            optionRepository.deleteAll(oldOptions);
            
            for (String optionText : optionTexts) {
                AssessmentOption option = new AssessmentOption();
                option.setQuestionId(id);
                option.setOptionText(optionText);
                optionRepository.save(option);
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Question updated successfully!");
            return "redirect:/admin/assessment/questions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to update question: " + e.getMessage());
            return "redirect:/admin/assessment/questions/edit/" + id;
        }
    }
    
    @PostMapping("/questions/delete/{id}")
    @Transactional
    public String deleteQuestion(@PathVariable Integer id,
                               RedirectAttributes redirectAttributes) {
        try {
            AssessmentQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
            
            questionRepository.delete(question);
            
            List<AssessmentQuestion> remainingQuestions = 
                questionRepository.findAllByOrderByQuestionOrderAsc();
            for (int i = 0; i < remainingQuestions.size(); i++) {
                AssessmentQuestion q = remainingQuestions.get(i);
                q.setQuestionOrder(i + 1);
                questionRepository.save(q);
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Question deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to delete question: " + e.getMessage());
        }
        return "redirect:/admin/assessment/questions";
    }
}