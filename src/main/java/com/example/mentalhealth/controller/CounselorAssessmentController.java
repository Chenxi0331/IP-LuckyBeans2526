package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.AssessmentQuestion;
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

import java.util.List;
import java.util.Map;

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
                assessmentService.getAllUsersLatestAssessmentsWithUserInfo();

        model.addAttribute("assessmentList", summaries);
        return "symptoms-recognition/counselor-assessment-overview";
    }

    @GetMapping("/student/{userId}")
    public String viewStudentAssessments(@PathVariable Integer userId, Model model) {
        AssessmentService.CounselorStudentPageData pageData =
                assessmentService.getCounselorStudentPageData(userId);

        model.addAttribute("student", pageData.getStudent());
        model.addAttribute("assessments", pageData.getAssessments());
        model.addAttribute("comments", pageData.getComments());
        model.addAttribute("trendDates", pageData.getTrendDates());
        model.addAttribute("trendScores", pageData.getTrendScores());

        return "symptoms-recognition/counselor-student-assessment-detail";
    }

    @GetMapping("/assessment/{assessmentId}/details")
    public String viewAssessmentDetails(@PathVariable Integer assessmentId, Model model) {
        AssessmentService.CounselorAssessmentDetailsPageData pageData =
                assessmentService.getCounselorAssessmentDetailsPageData(assessmentId);

        model.addAttribute("assessment", pageData.getAssessment());
        model.addAttribute("student", pageData.getStudent());
        model.addAttribute("answers", pageData.getAnswers());
        model.addAttribute("questionTexts", pageData.getQuestionTexts());
        model.addAttribute("answerTexts", pageData.getAnswerTexts());
        model.addAttribute("comments", pageData.getComments());

        return "symptoms-recognition/counselor-assessment-details";
    }

    @GetMapping("/student/{userId}/trend-data")
    @ResponseBody
    public Map<String, Object> getStudentTrendData(@PathVariable Integer userId) {
        return assessmentService.getStudentTrendData(userId);
    }

    @PostMapping("/student/{userId}/comment")
    public String addComment(@PathVariable Integer userId,
                             @RequestParam String commentText,
                             @RequestParam(required = false) Integer assessmentId,
                             @RequestParam(defaultValue = "true") Boolean visibleToStudent,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {

        assessmentService.addCounselorCommentByEmail(
                userDetails.getUsername(),
                userId.longValue(),
                assessmentId,
                commentText,
                visibleToStudent
        );

        redirectAttributes.addFlashAttribute("message", "Comment added successfully");
        return "redirect:/counselor/assessment/student/" + userId;
    }

    // ==================== Question Management ====================

    @GetMapping("/questions")
    public String listQuestions(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        AssessmentService.CounselorQuestionListPageData pageData =
                assessmentService.getCounselorQuestionsListPageData(userDetails.getUsername());

        model.addAttribute("questions", pageData.getApprovedQuestions());
        model.addAttribute("pendingCount", pageData.getMyPendingCount());
        return "symptoms-recognition/counselor-questions-list";
    }

    @GetMapping("/questions/add")
    public String showAddQuestionForm(Model model) {
        model.addAttribute("question", new AssessmentQuestion());
        model.addAttribute("isNew", true);
        return "symptoms-recognition/counselor-question-form";
    }

    @PostMapping("/questions/add")
    public String addQuestion(@ModelAttribute AssessmentQuestion question,
                              @RequestParam List<String> optionTexts,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            assessmentService.counselorSubmitNewQuestion(userDetails.getUsername(), question, optionTexts);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Question submitted for admin approval!");
            return "redirect:/counselor/assessment/questions/pending";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to submit question: " + e.getMessage());
            return "redirect:/counselor/assessment/questions/add";
        }
    }

    @GetMapping("/questions/edit/{id}")
    public String showEditQuestionForm(@PathVariable Integer id, Model model) {
        AssessmentQuestion question = assessmentService.getQuestionById(id);
        model.addAttribute("question", question);
        model.addAttribute("isNew", false);
        return "symptoms-recognition/counselor-question-form";
    }

    @PostMapping("/questions/edit/{id}")
    public String editQuestion(@PathVariable Integer id,
                               @ModelAttribute AssessmentQuestion question,
                               @RequestParam List<String> optionTexts,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            assessmentService.counselorSubmitEditQuestion(userDetails.getUsername(), id, question, optionTexts);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Changes submitted for admin approval!");
            return "redirect:/counselor/assessment/questions/pending";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to submit changes: " + e.getMessage());
            return "redirect:/counselor/assessment/questions/edit/" + id;
        }
    }

    @PostMapping("/questions/delete/{id}")
    public String deleteQuestion(@PathVariable Integer id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            assessmentService.counselorRequestDeleteQuestion(userDetails.getUsername(), id);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Deletion request submitted for admin approval!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to submit deletion request: " + e.getMessage());
        }
        return "redirect:/counselor/assessment/questions";
    }

    @GetMapping("/questions/pending")
    public String viewPendingQuestions(@AuthenticationPrincipal UserDetails userDetails,
                                       Model model) {
        List<AssessmentQuestion> questions =
                assessmentService.getCounselorMySubmissions(userDetails.getUsername());

        model.addAttribute("questions", questions);
        return "symptoms-recognition/counselor-pending-questions";
    }
}
