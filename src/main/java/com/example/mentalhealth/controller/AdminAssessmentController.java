package com.example.mentalhealth.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.mentalhealth.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.mentalhealth.model.AssessmentOption;
import com.example.mentalhealth.model.AssessmentQuestion;
import com.example.mentalhealth.model.AssessmentScoring;
import com.example.mentalhealth.model.AssessmentType;
import com.example.mentalhealth.model.AssessmentTypeRequest;
import com.example.mentalhealth.model.QuestionRequest;
import com.example.mentalhealth.model.UserAssessment;
import com.example.mentalhealth.model.UserAssessmentAnswer;
import com.example.mentalhealth.service.AssessmentService;
import com.example.mentalhealth.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/admin/assessment")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAssessmentController {

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public String redirectToSummary() {
        return "redirect:/admin/assessment/summary";
    }

    @GetMapping("/summary")
    public String viewAssessmentSummary(Model model) {
        AssessmentService.AssessmentStatistics stats =
                assessmentService.getAssessmentStatistics();
        long pendingRequestCount =
                assessmentService.getPendingAssessmentTypeRequestCount();

        model.addAttribute("statistics", stats);
        model.addAttribute("pendingRequestCount", pendingRequestCount);
        
        return "symptoms-recognition/admin-assessment-summary";
    }

@GetMapping("/all")
public String viewAllAssessments(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String severity,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @RequestParam(defaultValue = "date_desc") String sortBy,
        Model model) {
    
    // Get filtered and sorted assessments from service
    List<UserAssessment> filteredAssessments = assessmentService.getFilteredAssessments(
        search, type, severity, dateFrom, dateTo, sortBy);
    
    // Get total count
    int totalRecords = assessmentService.getTotalAssessmentCount();
    
    // Add to model
    model.addAttribute("assessments", filteredAssessments);
    model.addAttribute("totalRecords", totalRecords);
    model.addAttribute("filteredCount", filteredAssessments.size());
    
    // Keep filter values for form (so inputs stay filled)
    model.addAttribute("search", search != null ? search : "");
    model.addAttribute("type", type != null ? type : "");
    model.addAttribute("severity", severity != null ? severity : "");
    model.addAttribute("dateFrom", dateFrom);
    model.addAttribute("dateTo", dateTo);
    model.addAttribute("sortBy", sortBy);
    
    return "symptoms-recognition/admin-assessment-all";
}

    // ==================== Assessment Type Management ====================

    @GetMapping("/types")
    public String viewAssessmentTypes(Model model) {
        List<AssessmentType> allTypes =
                assessmentService.getAllAssessmentTypes();
        model.addAttribute("assessmentTypes", allTypes);
        return "symptoms-recognition/admin-assessment-type-list";
    }

    @GetMapping("/types/add")
    public String showAddAssessmentTypeForm(Model model) {
        model.addAttribute("assessmentType", new AssessmentType());
        return "symptoms-recognition/admin-add-assessment-type";
    }

    @PostMapping("/types/add")
    public String addAssessmentType(@RequestParam String questionsJson,
                                    @RequestParam String scoringJson,
                                    @RequestParam String typeCode,
                                    @RequestParam String typeName,
                                    @RequestParam String description,
                                    @RequestParam String instructions,
                                    @RequestParam Integer totalQuestions,
                                    @RequestParam Integer maxScore,
                                    RedirectAttributes redirectAttributes) {
        try {
            assessmentService.adminAddAssessmentTypeFromJson(
                typeCode, typeName, description, instructions,
                totalQuestions, maxScore, questionsJson, scoringJson);
            
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Assessment type added successfully!");
            return "redirect:/admin/assessment/types";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to add assessment type: " + e.getMessage());
            return "redirect:/admin/assessment/types/add";
        }
    }
    
    @PostMapping("/types/{typeId}/toggle-active")
    public String toggleAssessmentTypeActive(@PathVariable Integer typeId,
                                             RedirectAttributes redirectAttributes) {
        try {
            assessmentService.toggleAssessmentTypeActive(typeId);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Assessment type status updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to update status: " + e.getMessage());
        }
        return "redirect:/admin/assessment/types";
    }

    @PostMapping("/types/{typeId}/delete")
    public String deleteAssessmentType(@PathVariable Integer typeId,
                                       RedirectAttributes redirectAttributes) {
        try {
            assessmentService.adminDeleteAssessmentType(typeId);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Assessment type deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to delete: " + e.getMessage());
        }
        return "redirect:/admin/assessment/types";
    }

    // ==================== Assessment Type Request Approval ====================

    @GetMapping("/type-requests/pending")
    public String viewPendingTypeRequests(Model model) {
        List<AssessmentTypeRequest> pendingRequests =
                assessmentService.getPendingAssessmentTypeRequests();
        
        Map<Integer, List<QuestionRequest>> questionsByRequest = new HashMap<>();
        Map<Long, String> counselorNames = new HashMap<>();
        
        for (AssessmentTypeRequest request : pendingRequests) {
            List<QuestionRequest> questions = 
                assessmentService.getQuestionRequestsByTypeRequestId(request.getRequestId());
            questionsByRequest.put(request.getRequestId(), questions);
            
            if (counselorNames.get(request.getRequestedBy()) == null) {
                userService.findById(request.getRequestedBy())
                    .ifPresent(user -> counselorNames.put(user.getId(), user.getFullName()));
            }
        }
        
        model.addAttribute("requests", pendingRequests);
        model.addAttribute("questionsByRequest", questionsByRequest);
        model.addAttribute("counselorNames", counselorNames);
        
        return "symptoms-recognition/admin-assessment-type-requests";
    }

    @GetMapping("/type-requests/{requestId}")
    public String viewTypeRequestDetails(@PathVariable Integer requestId,
                                         Model model) {
        Map<String, Object> details =
                assessmentService.getAssessmentTypeRequestDetails(requestId);
        model.addAllAttributes(details);
        return "symptoms-recognition/admin-assessment-type-request-detail";
    }

    @PostMapping("/type-requests/{requestId}/approve")
    public String approveTypeRequest(@PathVariable Integer requestId,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        try {
            AssessmentTypeRequest request = assessmentService
                .getAssessmentTypeRequestById(requestId);
            
            if ("UPDATE".equals(request.getRequestType())) {
                assessmentService.adminApproveAssessmentTypeUpdateRequest(
                    requestId, userDetails.getUsername());
            } else {
                assessmentService.adminApproveAssessmentTypeRequest(
                    requestId, userDetails.getUsername());
            }
            
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Assessment type request approved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to approve: " + e.getMessage());
        }
        return "redirect:/admin/assessment/type-requests/pending";
    }

    @PostMapping("/type-requests/{requestId}/reject")
    public String rejectTypeRequest(@PathVariable Integer requestId,
                                    @RequestParam String notes,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        try {
            assessmentService.adminRejectAssessmentTypeRequest(
                    requestId, userDetails.getUsername(), notes);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Assessment type request rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to reject: " + e.getMessage());
        }
        return "redirect:/admin/assessment/type-requests/pending";
    }

    // @GetMapping("/types/{typeId}/edit")
    // public String showEditAssessmentTypeForm(@PathVariable Integer typeId, Model model) {
    //     try {
    //         AssessmentType type = assessmentService.getAssessmentTypeById(typeId);
            
    //         if (type.getIsSystemDefault() != null && type.getIsSystemDefault()) {
    //             model.addAttribute("errorMessage", "Cannot edit system default assessment types");
    //             return "redirect:/admin/assessment/types";
    //         }
            
    //         List<AssessmentQuestion> questions = 
    //             assessmentService.getApprovedQuestionsByType(typeId);
            
    //         model.addAttribute("assessmentType", type);
    //         model.addAttribute("questions", questions);
            
    //         return "symptoms-recognition/admin-edit-assessment-type";
    //     } catch (Exception e) {
    //         model.addAttribute("errorMessage", "Error loading assessment type: " + e.getMessage());
    //         return "redirect:/admin/assessment/types";
    //     }
    // }

    // @PostMapping("/types/{typeId}/edit")
    // public String editAssessmentType(@PathVariable Integer typeId,
    //                                 @RequestParam String questionsJson,
    //                                 @RequestParam String scoringJson,
    //                                 @RequestParam String typeName,
    //                                 @RequestParam String description,
    //                                 @RequestParam String instructions,
    //                                 @RequestParam Integer totalQuestions,
    //                                 @RequestParam Integer maxScore,
    //                                 RedirectAttributes redirectAttributes) {
    //     try {
    //         List<AssessmentQuestion> questions = parseQuestionsFromJson(questionsJson);
    //         List<AssessmentScoring> scorings = parseScoringsFromJson(scoringJson);
            
    //         AssessmentType updatedType = new AssessmentType();
    //         updatedType.setTypeName(typeName);
    //         updatedType.setDescription(description);
    //         updatedType.setInstructions(instructions);
    //         updatedType.setTotalQuestions(totalQuestions);
    //         updatedType.setMaxScore(maxScore);
            
    //         assessmentService.adminDirectUpdateAssessmentType(
    //             typeId, updatedType, questions, scorings);
            
    //         redirectAttributes.addFlashAttribute("successMessage",
    //             "Assessment type updated successfully!");
    //     } catch (Exception e) {
    //         redirectAttributes.addFlashAttribute("errorMessage",
    //             "Failed to update: " + e.getMessage());
    //     }
    //     return "redirect:/admin/assessment/types";
    // }

    // ==================== Helper Methods (JSON Parsing) ====================

    private List<AssessmentQuestion> parseQuestionsFromJson(String json) {
        try {
            List<Map<String, Object>> questionMaps = objectMapper.readValue(
                json, new TypeReference<List<Map<String, Object>>>(){});
            
            List<AssessmentQuestion> questions = new ArrayList<>();
            
            for (Map<String, Object> qMap : questionMaps) {
                AssessmentQuestion question = new AssessmentQuestion();
                question.setQuestionText((String) qMap.get("questionText"));
                question.setQuestionOrder((Integer) qMap.get("questionOrder"));
                
                String optionsJson = (String) qMap.get("options");
                List<String> optionTexts = objectMapper.readValue(
                    optionsJson, new TypeReference<List<String>>(){});
               
                List<AssessmentOption> options = new ArrayList<>();
                for (String text : optionTexts) {
                    AssessmentOption opt = new AssessmentOption();
                    opt.setOptionText(text);
                    options.add(opt);
                }
                question.setOptions(options);
                
                questions.add(question);
            }
            
            return questions;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse questions JSON: " + e.getMessage(), e);
        }
    }

    private List<AssessmentScoring> parseScoringsFromJson(String json) {
        try {
            List<Map<String, Object>> scoringMaps = objectMapper.readValue(
                json, new TypeReference<List<Map<String, Object>>>(){});
            
            List<AssessmentScoring> scorings = new ArrayList<>();
            
            for (Map<String, Object> sMap : scoringMaps) {
                AssessmentScoring scoring = new AssessmentScoring();
                scoring.setSeverityLevel((String) sMap.get("level"));
                scoring.setMinScore((Integer) sMap.get("min"));
                scoring.setMaxScore((Integer) sMap.get("max"));
                scoring.setInterpretation((String) sMap.get("interpretation"));
                scoring.setRecommendations((String) sMap.getOrDefault("recommendations", ""));
                scorings.add(scoring);
            }
            
            return scorings;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse scoring JSON: " + e.getMessage(), e);
        }
    }

/**
 * Toggle System Protected status for an assessment type
 */
@PostMapping("/types/{typeId}/toggle-system-protected")
public String toggleSystemProtected(@PathVariable Integer typeId,
                                    RedirectAttributes redirectAttributes) {
    try {
        AssessmentType type = assessmentService.getAssessmentTypeById(typeId);
        
        // Toggle the system default status
        boolean newStatus = !Boolean.TRUE.equals(type.getIsSystemDefault());
        type.setIsSystemDefault(newStatus);
        
        // If setting to system protected, also set is_editable to false
        if (newStatus) {
            type.setIsEditable(false);
        } else {
            type.setIsEditable(true);
        }
        
        // Save the changes
        assessmentService.updateAssessmentType(type);
        
        String message = newStatus 
            ? "Assessment type is now system protected. Counselors cannot edit it."
            : "Assessment type protection removed. Counselors can now submit edit requests.";
            
        redirectAttributes.addFlashAttribute("successMessage", message);
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage",
            "Failed to update system protection status: " + e.getMessage());
    }
    
    return "redirect:/admin/assessment/types";
}

/**
 * Show edit assessment type form with pre-populated data
 */
@GetMapping("/types/{typeId}/edit")
public String showEditAssessmentTypeForm(@PathVariable Integer typeId, Model model) {
    try {
        AssessmentType type = assessmentService.getAssessmentTypeById(typeId);
        
        // Get questions for this type
        List<AssessmentQuestion> questions = 
            assessmentService.getApprovedQuestionsByType(typeId);
        
        // Get scoring criteria
        List<AssessmentScoring> scorings = 
            assessmentService.getScoringByType(typeId);
        
        model.addAttribute("assessmentType", type);
        model.addAttribute("questions", questions);
        model.addAttribute("scorings", scorings);
        
        return "symptoms-recognition/admin-edit-assessment-type";
    } catch (Exception e) {
        model.addAttribute("errorMessage", "Error loading assessment type: " + e.getMessage());
        return "redirect:/admin/assessment/types";
    }
}

/**
 * Process edit form submission
 */
@PostMapping("/types/{typeId}/edit")
public String editAssessmentType(@PathVariable Integer typeId,
                                @RequestParam String questionsJson,
                                @RequestParam String scoringJson,
                                @RequestParam String typeName,
                                @RequestParam String description,
                                @RequestParam String instructions,
                                @RequestParam Integer totalQuestions,
                                @RequestParam Integer maxScore,
                                RedirectAttributes redirectAttributes) {
    try {
        // Parse questions from JSON
        List<AssessmentQuestion> questions = parseQuestionsFromJson(questionsJson);
        
        // Parse scoring from JSON
        List<AssessmentScoring> scorings = parseScoringsFromJson(scoringJson);
        
        // Create updated type object
        AssessmentType updatedType = new AssessmentType();
        updatedType.setTypeName(typeName);
        updatedType.setDescription(description);
        updatedType.setInstructions(instructions);
        updatedType.setTotalQuestions(totalQuestions);
        updatedType.setMaxScore(maxScore);
        
        // Call service method to update
        assessmentService.adminDirectUpdateAssessmentType(
            typeId, updatedType, questions, scorings);
        
        redirectAttributes.addFlashAttribute("successMessage",
            "Assessment type updated successfully!");
            
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage",
            "Failed to update: " + e.getMessage());
    }
    
    return "redirect:/admin/assessment/types";
}

    // Add this method to AdminAssessmentController.java for the modal details view

@GetMapping("/{assessmentId}/details")
@ResponseBody
public Map<String, Object> getAssessmentDetails(@PathVariable Integer assessmentId) {
    Map<String, Object> details = new HashMap<>();
    
    try {
        UserAssessment assessment = assessmentService.getAssessmentById(assessmentId);
        
        // Basic assessment info
        details.put("assessmentId", assessment.getAssessmentId());
        details.put("score", assessment.getTotalScore());
        details.put("severity", assessment.getSeverityLevel());
        details.put("result", assessment.getAssessmentResult());
        details.put("date", assessment.getAssessmentDate());
        
        // Get user info
        userService.findById(assessment.getUserId().longValue()).ifPresent(user -> {
            details.put("studentName", user.getFullName());
            details.put("studentEmail", user.getEmail());
            details.put("studentId", user.getId());
        });
        
        // Get assessment type info
        if (assessment.getAssessmentTypeId() != null) {
            AssessmentType type = assessmentService.getAssessmentTypeById(
                assessment.getAssessmentTypeId());
            details.put("typeName", type.getTypeName());
            details.put("typeCode", type.getTypeCode());
            details.put("maxScore", type.getMaxScore());
        }
        
        // Get answers
        List<UserAssessmentAnswer> answers = 
            assessmentService.getAnswersByAssessmentId(assessmentId);
        details.put("totalAnswers", answers.size());
        
        // Get recommendations
        if (assessment.getRecommendations() != null) {
            details.put("recommendations", assessment.getRecommendations());
        }
        
    } catch (Exception e) {
        details.put("error", "Failed to load assessment details: " + e.getMessage());
    }
    
    return details;
}

@GetMapping("/{assessmentId}/answers")
public String viewAssessmentAnswers(@PathVariable Integer assessmentId, Model model) {
    try {
        // Get assessment
        UserAssessment assessment = assessmentService.getAssessmentById(assessmentId);
        
        // Get answers
        List<UserAssessmentAnswer> answers = assessmentService.getAssessmentAnswers(assessmentId);
        
        // Get student info
        User student = userService.findById(assessment.getUserId().longValue())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        // Get assessment type info
        if (assessment.getAssessmentTypeId() != null) {
            AssessmentType type = assessmentService.getAssessmentTypeById(
                assessment.getAssessmentTypeId());
            assessment.setAssessmentTypeName(type.getTypeName());
            assessment.setMaxScore(type.getMaxScore());
        }
        
        // Get question texts and answer texts
        Map<Integer, String> questionTexts = new HashMap<>();
        Map<Integer, String> answerTexts = new HashMap<>();
        
        for (UserAssessmentAnswer answer : answers) {
            // Get question text
            AssessmentQuestion question = assessmentService.getQuestionById(answer.getQuestionId());
            questionTexts.put(answer.getQuestionId(), question.getQuestionText());
            
            // Get answer text
            AssessmentOption option = assessmentService.getOptionById(answer.getSelectedOptionId());
            answerTexts.put(answer.getQuestionId(), option.getOptionText());
        }
        
        model.addAttribute("assessment", assessment);
        model.addAttribute("answers", answers);
        model.addAttribute("studentName", student.getFullName());
        model.addAttribute("studentEmail", student.getEmail());
        model.addAttribute("questionTexts", questionTexts);
        model.addAttribute("answerTexts", answerTexts);
        
        return "symptoms-recognition/admin-assessment-answers";
        
    } catch (Exception e) {
        model.addAttribute("errorMessage", "Failed to load assessment answers: " + e.getMessage());
        return "redirect:/admin/assessment/all";
    }
}

}