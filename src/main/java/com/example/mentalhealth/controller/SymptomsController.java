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
        Integer currentQuestion = Optional.ofNullable((Integer) session.getAttribute("currentQuestion"))
                .orElse(1);

        // ✅ IMPORTANT: only show APPROVED questions to students
        List<AssessmentQuestion> allQuestions = assessmentService.getApprovedQuestions();
        int totalQuestions = allQuestions.size();

        // Edge: no approved questions
        if (totalQuestions == 0) {
            // show dashboard-like page but without questions
            model.addAttribute("currentQuestion", null);
            model.addAttribute("totalQuestions", 0);
            model.addAttribute("maxScore", 0);
            model.addAttribute("assessmentHistory", assessmentService.getUserAssessmentHistory(userId));
            model.addAttribute("counselorComments",
                    assessmentService.getVisibleStudentComments(userId.longValue()));
            return "symptoms-recognition/symptoms-recognition";
        }

        if (currentQuestion > totalQuestions || currentQuestion < 1) {
            return "redirect:/symptoms/dashboard";
        }

        AssessmentQuestion assessmentQuestion = allQuestions.get(currentQuestion - 1);

        // Build options (radio values are 0..n-1)
        List<Option> options = new ArrayList<>();
        int index = 0;
        // Ensure options is loaded (safe even if mapping is lazy)
        List<AssessmentOption> dbOptions = assessmentService.getQuestionOptions(assessmentQuestion.getQuestionId());
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

        // ✅ Fix: bind questionId into form so it gets submitted
        QuestionForm form = new QuestionForm();
        form.setQuestionId(assessmentQuestion.getQuestionId());

        model.addAttribute("currentQuestion", currentQuestion);
        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("progressPercentage", progressPercentage);
        model.addAttribute("question", questionDto);
        model.addAttribute("selectedAnswer", selectedAnswer);
        model.addAttribute("questionForm", form);

        // ✅ Dynamic maxScore (supports different option counts per question)
        int maxScore = assessmentService.calculateMaxScoreForApprovedQuestions();
        model.addAttribute("maxScore", maxScore);

        // Latest assessment
        UserAssessment latestAssessment = assessmentService.getLatestAssessment(userId);
        if (latestAssessment != null) {
            model.addAttribute("latestScore", latestAssessment.getTotalScore());
            model.addAttribute("latestAssessment", latestAssessment.getAssessmentResult());
            model.addAttribute("latestSeverity", latestAssessment.getSeverityLevel());
        }

        // Assessment history
        List<UserAssessment> assessments = assessmentService.getUserAssessmentHistory(userId);
        model.addAttribute("assessmentHistory", assessments);

        // Counselor comments visible to student
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

        // Counselor comments visible to student
        List<CounselorComment> counselorComments =
                assessmentService.getVisibleStudentComments(userId.longValue());
        model.addAttribute("counselorComments", counselorComments);

        // ✅ Dynamic maxScore for display "/max"
        int maxScore = assessmentService.calculateMaxScoreForApprovedQuestions();
        model.addAttribute("maxScore", maxScore);

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
        if (currentQuestion == null) currentQuestion = 1;

        // Save current answer + questionId into session
        session.setAttribute("answer_" + currentQuestion, questionForm.getAnswer());
        session.setAttribute("questionId_" + currentQuestion, questionForm.getQuestionId());

        // ✅ totalQuestions must match what we actually show (approved only)
        int totalQuestions = assessmentService.getApprovedQuestions().size();

        if (totalQuestions == 0) {
            // no questions -> go dashboard
            session.removeAttribute("currentQuestion");
            return "redirect:/symptoms/dashboard";
        }

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
        int totalQuestions = assessmentService.getApprovedQuestions().size();
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

            // ✅ now questionId will not be null because we bind it into form
            if (answerIndex != null && questionId != null) {
                answers.add(answerIndex);
                questionIds.add(questionId);
            }
        }

        assessmentService.saveAssessment(userId, answers, questionIds);
    }
}
