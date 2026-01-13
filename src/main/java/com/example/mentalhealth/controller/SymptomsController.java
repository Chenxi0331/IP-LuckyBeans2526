package com.example.mentalhealth.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.service.AssessmentService;
import com.example.mentalhealth.service.UserService;
import java.util.Optional;

import jakarta.servlet.http.HttpSession;

/**
 * Symptoms Recognition Controller
 * UC007: Access Mental Health Assessment (Student)
 * UC009: View Assessment History (Student)
 */
@Controller
@RequestMapping("/symptoms")
public class SymptomsController {

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private UserService userService;
    
    /**
     * UC007 + UC009: Display assessment page or history
     */
    @GetMapping
    public String symptoms(@AuthenticationPrincipal UserDetails userDetails,
                          HttpSession session, 
                          Model model) {
        User user = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
    
        Integer userId = user.getUserId();
        
        model.addAttribute("user", user);
        
        UserProgress userProgress = new UserProgress("7-day self-care streak!");
        model.addAttribute("userProgress", userProgress);

        Integer currentQuestion = (Integer) session.getAttribute("currentQuestion");
        if (currentQuestion == null) {
            currentQuestion = 1;
            session.setAttribute("currentQuestion", currentQuestion);
        }

        return showQuestion(session, model, userId);
    }

    /**
     * UC009: Display assessment history dashboard
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails,
                           HttpSession session, 
                           Model model) {
        User user = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Integer userId = user.getUserId();

        model.addAttribute("user", user);

        UserProgress userProgress = new UserProgress("7-day self-care streak!");
        model.addAttribute("userProgress", userProgress);

        return showDashboard(userId, model);
    }

    /**
     * Display current question
     */
    private String showQuestion(HttpSession session, Model model, Integer userId) {
        Integer currentQuestion = Optional.ofNullable(
            (Integer) session.getAttribute("currentQuestion")
            ).orElse(1);

        List<AssessmentQuestion> allQuestions = assessmentService.getAllQuestions();
        int totalQuestions = allQuestions.size();

        if (currentQuestion > totalQuestions || currentQuestion < 1) {
            return "redirect:/symptoms/dashboard";
        }

        AssessmentQuestion assessmentQuestion = allQuestions.get(currentQuestion - 1);

        List<Option> options = new ArrayList<>();
        int index = 0;
        for (AssessmentOption ao : assessmentQuestion.getOptions()) {
            options.add(new Option(index, ao.getOptionText()));
            index++;
        }

        Integer selectedAnswer = (Integer) session.getAttribute("answer_" + currentQuestion);
        int progressPercentage = (currentQuestion * 100) / totalQuestions;

        Question questionDto = new Question();
        questionDto.setId(assessmentQuestion.getQuestionId());
        questionDto.setText(assessmentQuestion.getQuestionText());
        questionDto.setOptions(options);

        model.addAttribute("currentQuestion", currentQuestion);
        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("progressPercentage", progressPercentage);
        model.addAttribute("question", questionDto);
        model.addAttribute("selectedAnswer", selectedAnswer);
        model.addAttribute("questionForm", new QuestionForm());

        // Get latest assessment result (if exists)
        UserAssessment latestAssessment = assessmentService.getLatestAssessment(userId);
        if (latestAssessment != null) {
            model.addAttribute("latestScore", latestAssessment.getTotalScore());
            model.addAttribute("latestAssessment", latestAssessment.getAssessmentResult());
            model.addAttribute("latestSeverity", latestAssessment.getSeverityLevel());
        }

        // Get assessment history
        List<UserAssessment> assessments = assessmentService.getUserAssessmentHistory(userId);
        model.addAttribute("assessmentHistory", assessments);
        
        // Get counselor comments visible to student
        List<CounselorComment> counselorComments = 
            assessmentService.getVisibleStudentComments(userId.longValue());
        model.addAttribute("counselorComments", counselorComments);

        return "symptoms-recognition/symptoms-recognition";
    }

    /**
     * Display dashboard (assessment history)
     */
    private String showDashboard(Integer userId, Model model) {
        List<UserAssessment> assessments = assessmentService.getUserAssessmentHistory(userId);

        if (!assessments.isEmpty()) {
            UserAssessment latest = assessments.get(0);
            model.addAttribute("totalScore", latest.getTotalScore());
            model.addAttribute("assessment", latest.getAssessmentResult());
            model.addAttribute("severityLevel", latest.getSeverityLevel());
        }

        model.addAttribute("assessmentHistory", assessments);
        
        // Get counselor comments visible to student
        List<CounselorComment> counselorComments = 
            assessmentService.getVisibleStudentComments(userId.longValue());
        model.addAttribute("counselorComments", counselorComments);

        return "symptoms-recognition/symptoms-recognition";
    }

    /**
     * UC007: Submit answer
     */
    @PostMapping("/submit")
    public String submitAnswer(@ModelAttribute QuestionForm questionForm,
                               @AuthenticationPrincipal UserDetails userDetails,
                               HttpSession session) {
        User user = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        Integer userId = user.getUserId();
        
        Integer currentQuestion = (Integer) session.getAttribute("currentQuestion");

        if (currentQuestion == null) {
            currentQuestion = 1;
        }

        session.setAttribute("answer_" + currentQuestion, questionForm.getAnswer());
        session.setAttribute("questionId_" + currentQuestion, questionForm.getQuestionId());

        int totalQuestions = assessmentService.getTotalQuestions();

        if (currentQuestion < totalQuestions) {
            session.setAttribute("currentQuestion", currentQuestion + 1);
            return "redirect:/symptoms";
        } else {
            saveAssessmentToDatabase(userId, session, totalQuestions);
            
            // Clear session data
            for (int i = 1; i <= totalQuestions; i++) {
                session.removeAttribute("answer_" + i);
                session.removeAttribute("questionId_" + i);
            }
            session.removeAttribute("currentQuestion");
            
            return "redirect:/symptoms/dashboard";
        }
    }

    /**
     * Go to previous question
     */
    @GetMapping("/previous")
    public String previousQuestion(HttpSession session) {
        Integer currentQuestion = (Integer) session.getAttribute("currentQuestion");
        if (currentQuestion != null && currentQuestion > 1) {
            session.setAttribute("currentQuestion", currentQuestion - 1);
        }
        return "redirect:/symptoms";
    }

    /**
     * Start new assessment
     */
    @GetMapping("/start")
    public String startAssessment(HttpSession session) {
        session.setAttribute("currentQuestion", 1);
        return "redirect:/symptoms";
    }

    /**
     * Restart assessment
     */
    @GetMapping("/restart")
    public String restartAssessment(HttpSession session) {
        int totalQuestions = assessmentService.getTotalQuestions();
        for (int i = 1; i <= totalQuestions; i++) {
            session.removeAttribute("answer_" + i);
            session.removeAttribute("questionId_" + i);
        }
        session.setAttribute("currentQuestion", 1);
        return "redirect:/symptoms";
    }

    /**
     * Save assessment to database
     */
    private void saveAssessmentToDatabase(Integer userId, HttpSession session, int totalQuestions) {
        List<Integer> answers = new ArrayList<>();
        List<Integer> questionIds = new ArrayList<>();

        for (int i = 1; i <= totalQuestions; i++) {
            Integer answerIndex = (Integer) session.getAttribute("answer_" + i);
            Integer questionId = (Integer) session.getAttribute("questionId_" + i);

            if (answerIndex != null && questionId != null) {
                answers.add(answerIndex);
                questionIds.add(questionId);
            }
        }

        // Use Service to save assessment
        assessmentService.saveAssessment(userId, answers, questionIds);
    }
}