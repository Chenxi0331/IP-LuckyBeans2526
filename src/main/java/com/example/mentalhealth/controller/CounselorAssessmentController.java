package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.CounselorComment;
import com.example.mentalhealth.model.User;
import com.example.mentalhealth.model.UserAssessment;
import com.example.mentalhealth.model.UserAssessmentAnswer;
import com.example.mentalhealth.service.AssessmentService;
import com.example.mentalhealth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/counselor/assessment")
@PreAuthorize("hasAuthority('ROLE_COUNSELOR')")
public class CounselorAssessmentController {
    
    @Autowired
    private AssessmentService assessmentService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String viewAllAssessments(Model model) {
        List<AssessmentService.UserAssessmentSummary> summaries = 
            assessmentService.getAllUsersLatestAssessments();
        
        for (AssessmentService.UserAssessmentSummary summary : summaries) {
            userService.findById(summary.getUserId().longValue()).ifPresent(user -> {
                summary.setUserName(user.getFullName());
                summary.setUserEmail(user.getEmail());
            });
        }
        
        model.addAttribute("assessmentList", summaries);
        return "symptoms-recognition/counselor-assessment-overview";
    }
    
    @GetMapping("/student/{userId}")
    public String viewStudentAssessments(@PathVariable Integer userId, Model model) {
        User student = userService.findById(userId.longValue())
            .orElseThrow(() -> new RuntimeException("Student not found"));
        
        List<UserAssessment> assessments = assessmentService
            .getUserAssessmentHistory(userId);
        
        List<CounselorComment> comments = assessmentService
            .getStudentComments(userId.longValue());
        
        model.addAttribute("student", student);
        model.addAttribute("assessments", assessments);
        model.addAttribute("comments", comments);
        
        List<String> dates = assessments.stream()
            .map(a -> a.getAssessmentDate().toLocalDate().toString())
            .collect(Collectors.toList());
        List<Integer> scores = assessments.stream()
            .map(UserAssessment::getTotalScore)
            .collect(Collectors.toList());
        
        model.addAttribute("trendDates", dates);
        model.addAttribute("trendScores", scores);
        
        return "symptoms-recognition/counselor-student-assessment-detail";
    }
    
    @GetMapping("/assessment/{assessmentId}/details")
    public String viewAssessmentDetails(@PathVariable Integer assessmentId, Model model) {
        UserAssessment assessment = assessmentService.getAssessmentById(assessmentId);
        List<UserAssessmentAnswer> answers = assessmentService.getAssessmentAnswers(assessmentId);
        
        User student = userService.findById(assessment.getUserId().longValue())
            .orElseThrow(() -> new RuntimeException("Student not found"));
        
        Map<Integer, String> questionTexts = new HashMap<>();
        Map<Integer, String> answerTexts = new HashMap<>();
        
        for (UserAssessmentAnswer answer : answers) {
            var question = assessmentService.getQuestionById(answer.getQuestionId());
            questionTexts.put(answer.getQuestionId(), question.getQuestionText());
            
            var option = assessmentService.getOptionById(answer.getSelectedOptionId());
            answerTexts.put(answer.getQuestionId(), option.getOptionText());
        }
        
        List<CounselorComment> comments = assessmentService.getAssessmentComments(assessmentId);
        
        model.addAttribute("assessment", assessment);
        model.addAttribute("student", student);
        model.addAttribute("answers", answers);
        model.addAttribute("questionTexts", questionTexts);
        model.addAttribute("answerTexts", answerTexts);
        model.addAttribute("comments", comments);
        
        return "symptoms-recognition/counselor-assessment-details";
    }
    
    @GetMapping("/student/{userId}/trend-data")
    @ResponseBody
    public Map<String, Object> getStudentTrendData(@PathVariable Integer userId) {
        List<UserAssessment> assessments = assessmentService
            .getUserAssessmentHistory(userId);
        
        Collections.reverse(assessments);
        
        Map<String, Object> data = new HashMap<>();
        data.put("dates", assessments.stream()
            .map(a -> a.getAssessmentDate().toLocalDate().toString())
            .collect(Collectors.toList()));
        data.put("scores", assessments.stream()
            .map(UserAssessment::getTotalScore)
            .collect(Collectors.toList()));
        data.put("severities", assessments.stream()
            .map(UserAssessment::getSeverityLevel)
            .collect(Collectors.toList()));
        
        return data;
    }

    @PostMapping("/student/{userId}/comment")
    public String addComment(@PathVariable Integer userId,
                            @RequestParam String commentText,
                            @RequestParam(required = false) Integer assessmentId,
                            @RequestParam(defaultValue = "true") Boolean visibleToStudent,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        
        User counselor = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Counselor not found"));
        
        assessmentService.addComment(
            counselor.getId(),
            userId.longValue(),
            assessmentId,
            commentText,
            visibleToStudent
        );
        
        redirectAttributes.addFlashAttribute("message", "Comment added successfully");
        return "redirect:/counselor/assessment/student/" + userId;
    }
}