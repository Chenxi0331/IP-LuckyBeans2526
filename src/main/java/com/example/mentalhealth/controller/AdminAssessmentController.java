package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.AssessmentQuestion;
import com.example.mentalhealth.model.UserAssessment;
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

@Controller
@RequestMapping("/admin/assessment")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAssessmentController {

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String redirectToSummary() {
        return "redirect:/admin/assessment/summary";
    }

    @GetMapping("/summary")
    public String viewAssessmentSummary(Model model) {
        AssessmentService.AssessmentStatistics stats = assessmentService.getAssessmentStatistics();
        long pendingCount = assessmentService.getPendingQuestionCount();

        model.addAttribute("statistics", stats);
        model.addAttribute("pendingCount", pendingCount);
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
        AssessmentService.AdminQuestionListPageData pageData =
                assessmentService.getAdminApprovedQuestionsPageData();

        model.addAttribute("questions", pageData.getApprovedQuestions());
        model.addAttribute("pendingCount", pageData.getPendingCount());
        return "symptoms-recognition/admin-questions-list";
    }

    @GetMapping("/questions/pending")
    public String viewPendingQuestions(Model model) {
        AssessmentService.AdminPendingQuestionsPageData pageData =
                assessmentService.getAdminPendingQuestionsPageData();

        model.addAttribute("pendingQuestions", pageData.getPendingQuestions());
        model.addAttribute("counselorNames", pageData.getCounselorNames());
        model.addAttribute("counselorEmails", pageData.getCounselorEmails());

        return "symptoms-recognition/admin-pending-questions";
    }

    @GetMapping("/questions/history")
    public String viewApprovalHistory(Model model) {
        AssessmentService.AdminApprovalHistoryPageData pageData =
                assessmentService.getAdminApprovalHistoryPageData();

        model.addAttribute("historyQuestions", pageData.getHistoryQuestions());
        model.addAttribute("counselorNames", pageData.getCounselorNames());
        model.addAttribute("counselorEmails", pageData.getCounselorEmails());
        model.addAttribute("approverNames", pageData.getApproverNames());

        return "symptoms-recognition/admin-approval-history";
    }

    @GetMapping("/questions/add")
    public String showAddForm(Model model) {
        model.addAttribute("question", new AssessmentQuestion());
        return "symptoms-recognition/admin-question-form";
    }

    @GetMapping("/questions/edit/{id}")
public String showEditForm(@PathVariable Integer id, Model model) {
    AssessmentQuestion question = assessmentService.getQuestionById(id);
    model.addAttribute("question", question);
    return "symptoms-recognition/admin-question-form"; // 或者你单独做 admin edit form
}

@PostMapping("/questions/edit/{id}")
public String updateQuestion(@PathVariable Integer id,
                             @ModelAttribute AssessmentQuestion question,
                             @RequestParam List<String> optionTexts,
                             RedirectAttributes redirectAttributes) {
    try {
        assessmentService.adminEditQuestion(id, question, optionTexts);
        redirectAttributes.addFlashAttribute("successMessage", "Question updated successfully!");
        return "redirect:/admin/assessment/questions";
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Failed to update question: " + e.getMessage());
        return "redirect:/admin/assessment/questions/edit/" + id;
    }
}

    @PostMapping("/questions/add")
    public String addQuestion(@ModelAttribute AssessmentQuestion question,
                              @RequestParam List<String> optionTexts,
                              RedirectAttributes redirectAttributes) {
        try {
            assessmentService.adminAddQuestion(question, optionTexts);

            redirectAttributes.addFlashAttribute("successMessage", "Question added successfully!");
            return "redirect:/admin/assessment/questions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to add question: " + e.getMessage());
            return "redirect:/admin/assessment/questions/add";
        }
    }

    @PostMapping("/questions/{id}/approve")
    public String approveQuestion(@PathVariable Integer id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            assessmentService.adminApproveQuestion(id, userDetails.getUsername());

            redirectAttributes.addFlashAttribute("successMessage", "Question approved successfully!");
            return "redirect:/admin/assessment/questions/pending";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to approve question: " + e.getMessage());
            return "redirect:/admin/assessment/questions/pending";
        }
    }

    @PostMapping("/questions/{id}/reject")
    public String rejectQuestion(@PathVariable Integer id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            assessmentService.adminRejectQuestion(id, userDetails.getUsername());

            redirectAttributes.addFlashAttribute("successMessage", "Question rejected.");
            return "redirect:/admin/assessment/questions/pending";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to reject question: " + e.getMessage());
            return "redirect:/admin/assessment/questions/pending";
        }
    }

    @PostMapping("/questions/{id}/approve-deletion")
    public String approveDeletion(@PathVariable Integer id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            assessmentService.adminApproveDeletion(id, userDetails.getUsername());

            redirectAttributes.addFlashAttribute("successMessage", "Question deleted successfully!");
            return "redirect:/admin/assessment/questions/pending";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to delete question: " + e.getMessage());
            return "redirect:/admin/assessment/questions/pending";
        }
    }

    @PostMapping("/questions/{id}/delete")
    public String deleteQuestion(@PathVariable Integer id,
                                 RedirectAttributes redirectAttributes) {
        try {
            assessmentService.adminDeleteSubmissionQuestion(id);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Question submission deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to delete question: " + e.getMessage());
        }
        return "redirect:/admin/assessment/questions/pending";
    }
}
