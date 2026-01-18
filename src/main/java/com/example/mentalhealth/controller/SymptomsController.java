package com.example.mentalhealth.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.service.AssessmentService;
import com.example.mentalhealth.service.UserService;

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
     * Assessment type selection page
     */
    @GetMapping("/select-type")
    public String selectAssessmentType(@AuthenticationPrincipal UserDetails userDetails,
                                       HttpSession session,
                                       Model model) {
        clearAssessmentSession(session);
        
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<AssessmentType> assessmentTypes = assessmentService.getActiveAssessmentTypes();
        
        model.addAttribute("user", user);
        model.addAttribute("assessmentTypes", assessmentTypes);
        
        UserProgress userProgress = new UserProgress("7-day self-care streak!");
        model.addAttribute("userProgress", userProgress);
        
        return "symptoms-recognition/select-assessment-type";
    }

    /**
     * UC007 + UC009: Display assessment page or assessment history
     */
    @GetMapping
    public String symptoms(@AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam(required = false) Integer assessmentTypeId,
                           HttpSession session,
                           Model model) {

        if (assessmentTypeId == null) {
            Integer sessionTypeId = (Integer) session.getAttribute("currentAssessmentTypeId");
            if (sessionTypeId == null) {
                return "redirect:/symptoms/select-type";
            }
            assessmentTypeId = sessionTypeId;
        }

        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Integer userId = user.getUserId();
        
        session.setAttribute("currentAssessmentTypeId", assessmentTypeId);

        model.addAttribute("user", user);

        UserProgress userProgress = new UserProgress("7-day self-care streak!");
        model.addAttribute("userProgress", userProgress);

        Integer currentQuestion = (Integer) session.getAttribute("currentQuestion");
        if (currentQuestion == null) {
            currentQuestion = 1;
            session.setAttribute("currentQuestion", currentQuestion);
        }

        return showQuestion(session, model, userId, assessmentTypeId);
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
     * Display the current assessment question
     */
    private String showQuestion(HttpSession session, Model model, Integer userId, Integer assessmentTypeId) {
        Integer currentQuestion = Optional.ofNullable((Integer) session.getAttribute("currentQuestion"))
                .orElse(1);

        List<AssessmentQuestion> allQuestions =
                assessmentService.getApprovedQuestionsByType(assessmentTypeId);
        int totalQuestions = allQuestions.size();

        AssessmentType assessmentType = assessmentService.getAssessmentTypeById(assessmentTypeId);

        if (totalQuestions == 0) {
            model.addAttribute("currentQuestion", null);
            model.addAttribute("totalQuestions", 0);
            model.addAttribute("maxScore", 0);
            model.addAttribute("assessmentHistory", 
                    assessmentService.getUserAssessmentHistoryWithTypeNames(userId));
            model.addAttribute("counselorComments",
                    assessmentService.getVisibleStudentComments(userId.longValue()));
            return "symptoms-recognition/symptoms-recognition";
        }

        if (currentQuestion > totalQuestions || currentQuestion < 1) {
            return "redirect:/symptoms/dashboard";
        }

        AssessmentQuestion assessmentQuestion = allQuestions.get(currentQuestion - 1);

        List<Option> options = new ArrayList<>();
        int index = 0;
        List<AssessmentOption> dbOptions =
                assessmentService.getQuestionOptions(assessmentQuestion.getQuestionId());
        for (AssessmentOption ao : dbOptions) {
            options.add(new Option(index, ao.getOptionText()));
            index++;
        }

        Integer selectedAnswer = (Integer) session.getAttribute("answer_" + currentQuestion);
        int progressPercentage = (currentQuestion * 100) / totalQuestions;

        Question questionDto = new Question();
        questionDto.setId(assessmentQuestion.getQuestionId());
        questionDto.setText(assessmentQuestion.getQuestionText());
        questionDto.setOptions(options);

        QuestionForm form = new QuestionForm();
        form.setQuestionId(assessmentQuestion.getQuestionId());

        model.addAttribute("currentQuestion", currentQuestion);
        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("progressPercentage", progressPercentage);
        model.addAttribute("question", questionDto);
        model.addAttribute("selectedAnswer", selectedAnswer);
        model.addAttribute("questionForm", form);

        model.addAttribute("assessmentTypeName", assessmentType.getTypeName());
        model.addAttribute("assessmentTypeInstructions", assessmentType.getInstructions());
        model.addAttribute("maxScore", assessmentType.getMaxScore());

        UserAssessment latestAssessment = 
                assessmentService.getLatestAssessmentWithTypeName(userId);
        if (latestAssessment != null) {
            model.addAttribute("latestScore", latestAssessment.getTotalScore());
            model.addAttribute("latestAssessment", latestAssessment.getAssessmentResult());
            model.addAttribute("latestSeverity", latestAssessment.getSeverityLevel());
            model.addAttribute("latestAssessmentType", latestAssessment.getAssessmentTypeName());
        }

        model.addAttribute("assessmentHistory", 
                assessmentService.getUserAssessmentHistoryWithTypeNames(userId));
        model.addAttribute("counselorComments",
                assessmentService.getVisibleStudentComments(userId.longValue()));

        return "symptoms-recognition/symptoms-recognition";
    }

    /**
     * Display dashboard containing assessment history
     */
    private String showDashboard(Integer userId, Model model) {
        List<UserAssessment> assessments =
                assessmentService.getUserAssessmentHistoryWithTypeNames(userId);

        if (!assessments.isEmpty()) {
            UserAssessment latest = assessments.get(0);
            model.addAttribute("totalScore", latest.getTotalScore());
            model.addAttribute("assessment", latest.getAssessmentResult());
            model.addAttribute("severityLevel", latest.getSeverityLevel());
            model.addAttribute("latestAssessmentType", latest.getAssessmentTypeName());
        
           if (latest.getAssessmentTypeId() != null) {
            AssessmentType type = assessmentService.getAssessmentTypeById(latest.getAssessmentTypeId());
            model.addAttribute("maxScore", type.getMaxScore());
            } else {
            model.addAttribute("maxScore", 27); // Default for legacy data
            } 
        }
        
        model.addAttribute("assessmentHistory", assessments);
        model.addAttribute("counselorComments",
                assessmentService.getVisibleStudentComments(userId.longValue()));

        return "symptoms-recognition/symptoms-recognition";
    }

    /**
     * UC007: Submit answer for the current question
     */
    @PostMapping("/submit")
    public String submitAnswer(@ModelAttribute QuestionForm questionForm,
                               @AuthenticationPrincipal UserDetails userDetails,
                               HttpSession session) {

        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Integer userId = user.getUserId();

        Integer currentQuestion = (Integer) session.getAttribute("currentQuestion");
        if (currentQuestion == null) currentQuestion = 1;

        session.setAttribute("answer_" + currentQuestion, questionForm.getAnswer());
        session.setAttribute("questionId_" + currentQuestion, questionForm.getQuestionId());

        Integer assessmentTypeId = (Integer) session.getAttribute("currentAssessmentTypeId");
        if (assessmentTypeId == null) {
            return "redirect:/symptoms/select-type";
        }

        List<AssessmentQuestion> questions =
                assessmentService.getApprovedQuestionsByType(assessmentTypeId);
        int totalQuestions = questions.size();

        if (totalQuestions == 0) {
            session.removeAttribute("currentQuestion");
            return "redirect:/symptoms/dashboard";
        }

        if (currentQuestion < totalQuestions) {
            session.setAttribute("currentQuestion", currentQuestion + 1);
            return "redirect:/symptoms?assessmentTypeId=" + assessmentTypeId;
        } else {
            saveAssessmentToDatabase(userId, session, totalQuestions, assessmentTypeId);
            clearAssessmentSession(session, totalQuestions);
            return "redirect:/symptoms/dashboard";
        }
    }

    /**
     * Navigate to the previous question
     */
    @GetMapping("/previous")
    public String previousQuestion(@RequestParam(required = false) Integer assessmentTypeId,
                                   HttpSession session) {
        Integer currentQuestion = (Integer) session.getAttribute("currentQuestion");
        if (currentQuestion != null && currentQuestion > 1) {
            session.setAttribute("currentQuestion", currentQuestion - 1);
        }
        
        if (assessmentTypeId == null) {
            assessmentTypeId = (Integer) session.getAttribute("currentAssessmentTypeId");
        }
        
        return "redirect:/symptoms?assessmentTypeId=" + assessmentTypeId;
    }

    /**
     * Start a new assessment
     */
    @GetMapping("/start")
    public String startAssessment(@RequestParam Integer assessmentTypeId,
                                  HttpSession session) {
        session.setAttribute("currentQuestion", 1);
        session.setAttribute("currentAssessmentTypeId", assessmentTypeId);
        return "redirect:/symptoms?assessmentTypeId=" + assessmentTypeId;
    }

    /**
     * Restart the current assessment
     */
    @GetMapping("/restart")
    public String restartAssessment(HttpSession session) {
        Integer assessmentTypeId = (Integer) session.getAttribute("currentAssessmentTypeId");
        
        if (assessmentTypeId != null) {
            List<AssessmentQuestion> questions =
                    assessmentService.getApprovedQuestionsByType(assessmentTypeId);
            int totalQuestions = questions.size();
            
            for (int i = 1; i <= totalQuestions; i++) {
                session.removeAttribute("answer_" + i);
                session.removeAttribute("questionId_" + i);
            }
        }
        
        session.setAttribute("currentQuestion", 1);
        
        if (assessmentTypeId == null) {
            return "redirect:/symptoms/select-type";
        }
        
        return "redirect:/symptoms?assessmentTypeId=" + assessmentTypeId;
    }

    /**
     * Save completed assessment results to the database
     */
    private void saveAssessmentToDatabase(Integer userId, HttpSession session,
                                          int totalQuestions, Integer assessmentTypeId) {
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

        assessmentService.saveAssessment(userId, answers, questionIds, assessmentTypeId);
    }
    
    /**
     * Clear assessment-related session data
     */
    private void clearAssessmentSession(HttpSession session) {
        clearAssessmentSession(session, 50);
    }
    
    /**
     * Clear assessment-related session data with specific question count
     */
    private void clearAssessmentSession(HttpSession session, int totalQuestions) {
        for (int i = 1; i <= totalQuestions; i++) {
            session.removeAttribute("answer_" + i);
            session.removeAttribute("questionId_" + i);
        }
        session.removeAttribute("currentQuestion");
        session.removeAttribute("currentAssessmentTypeId");
    }
}