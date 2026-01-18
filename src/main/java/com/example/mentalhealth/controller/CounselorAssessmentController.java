package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.repository.AssessmentOptionRepository;
import com.example.mentalhealth.repository.AssessmentQuestionRepository;
import com.example.mentalhealth.repository.AssessmentScoringRepository;
import com.example.mentalhealth.service.AssessmentService;
import com.example.mentalhealth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private AssessmentScoringRepository assessmentScoringRepository;

    @Autowired
    private UserService userService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AssessmentQuestionRepository assessmentQuestionRepository;
    
    @Autowired
    private AssessmentOptionRepository assessmentOptionRepository;

    /**
     * View overview of all students' latest assessments
     */
    @GetMapping
    public String viewAllAssessments(Model model) {
        List<AssessmentService.UserAssessmentSummary> summaries =
                assessmentService.getAllUsersLatestAssessmentsWithUserInfo();

        model.addAttribute("assessmentList", summaries);
        return "symptoms-recognition/counselor-assessment-overview";
    }

    /**
     * View all assessments and details for a specific student
     */
 @GetMapping("/student/{userId}")
public String viewStudentAssessments(@PathVariable Integer userId, Model model) {
    AssessmentService.CounselorStudentPageData pageData =
            assessmentService.getCounselorStudentPageData(userId);

    // Get assessments and enrich with type names AND recommendations
    List<UserAssessment> assessmentsWithDetails = pageData.getAssessments();
    
    for (UserAssessment assessment : assessmentsWithDetails) {
        if (assessment.getAssessmentTypeId() != null) {
            try {
                // Set assessment type name and max score
                AssessmentType type = assessmentService.getAssessmentTypeById(
                    assessment.getAssessmentTypeId()
                );
                assessment.setAssessmentTypeName(type.getTypeName());
                assessment.setMaxScore(type.getMaxScore());
                
                // Get and set recommendations from scoring
                AssessmentScoring scoring = assessmentService.getScoringResult(
                    assessment.getAssessmentTypeId(), 
                    assessment.getTotalScore()
                );
                
                if (scoring != null && scoring.getRecommendations() != null) {
                    assessment.setRecommendations(scoring.getRecommendations());
                }
            } catch (Exception e) {
                assessment.setAssessmentTypeName("Unknown");
            }
        }
    }

    model.addAttribute("student", pageData.getStudent());
    model.addAttribute("assessments", assessmentsWithDetails);
    model.addAttribute("comments", pageData.getComments());
    model.addAttribute("trendDates", pageData.getTrendDates());
    model.addAttribute("trendScores", pageData.getTrendScores());

    return "symptoms-recognition/counselor-student-assessment-detail";
}


    /**
     * View details of a specific assessment
     */
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

    /**
     * Return JSON trend data for a student's assessments
     */
    @GetMapping("/student/{userId}/trend-data")
    @ResponseBody
    public Map<String, Object> getStudentTrendData(@PathVariable Integer userId) {
        return assessmentService.getStudentTrendData(userId);
    }

    /**
     * Add a comment for a student's assessment
     */
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

    // ==================== Assessment Type Request Management ====================

    /**
     * View list of active assessment types
     */
    @GetMapping("/types")
    public String viewAssessmentTypes(Model model) {
        List<AssessmentType> types = assessmentService.getActiveAssessmentTypes();
        model.addAttribute("assessmentTypes", types);
        return "symptoms-recognition/counselor-assessment-types";
    }
    
    /**
     * Display form to submit a new assessment type request
     */
    @GetMapping("/types/submit")
    public String showSubmitTypeForm(Model model) {
        model.addAttribute("typeRequest", new AssessmentTypeRequest());
        return "symptoms-recognition/counselor-submit-assessment-type";
    }
    
    /**
     * Submit a new assessment type request (for admin approval)
     */
    @PostMapping("/types/submit")
    public String submitAssessmentType(@RequestParam String questionsJson,
                                       @RequestParam String scoringJson,
                                       @RequestParam String typeCode,
                                       @RequestParam String typeName,
                                       @RequestParam String description,
                                       @RequestParam String instructions,
                                       @RequestParam Integer totalQuestions,
                                       @RequestParam Integer maxScore,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        try {
            AssessmentTypeRequest typeRequest = new AssessmentTypeRequest();
            typeRequest.setTypeCode(typeCode);
            typeRequest.setTypeName(typeName);
            typeRequest.setDescription(description);
            typeRequest.setInstructions(instructions);
            typeRequest.setTotalQuestions(totalQuestions);
            typeRequest.setMaxScore(maxScore);
            typeRequest.setScoringCriteria(scoringJson);
            
            // Parse questions JSON
            List<QuestionRequest> questions = parseQuestionsJson(questionsJson);
            
            assessmentService.counselorSubmitAssessmentType(
                userDetails.getUsername(), typeRequest, questions);
            
            redirectAttributes.addFlashAttribute("successMessage",
                "Assessment type submitted for admin approval!");
            return "redirect:/counselor/assessment/type-requests";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Failed to submit: " + e.getMessage());
            return "redirect:/counselor/assessment/types/submit";
        }
    }
    
    /**
     * View counselor's own assessment type requests
     */
    @GetMapping("/type-requests")
    public String viewMyTypeRequests(@AuthenticationPrincipal UserDetails userDetails,
                                     Model model) {
        List<AssessmentTypeRequest> requests =
                assessmentService.getCounselorAssessmentTypeRequests(userDetails.getUsername());
        model.addAttribute("requests", requests);
        return "symptoms-recognition/counselor-my-assessment-requests";
    }
    
    /**
     * View details of a specific assessment type request
     */
@GetMapping("/type-requests/{requestId}")
public String viewTypeRequestDetails(@PathVariable Integer requestId, 
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
    try {
        Map<String, Object> details = assessmentService.getAssessmentTypeRequestDetails(requestId);
        
        AssessmentTypeRequest request = (AssessmentTypeRequest) details.get("request");
        
        User currentUser = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!request.getRequestedBy().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "You don't have permission to view this request");
            return "redirect:/counselor/assessment/type-requests";
        }
        
        model.addAttribute("request", request);
        model.addAttribute("questions", details.get("questions"));
        model.addAttribute("scoringList", details.get("scoringList"));
        model.addAttribute("counselorName", details.get("counselorName"));
        
        return "symptoms-recognition/counselor-assessment-type-request-detail"; 
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", 
            "Failed to load request details: " + e.getMessage());
        return "redirect:/counselor/assessment/type-requests";
    }
}
    
    // ==================== Helper Methods ====================
    
    private List<QuestionRequest> parseQuestionsJson(String questionsJson) {
        try {
            return objectMapper.readValue(questionsJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, QuestionRequest.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse questions JSON: " + e.getMessage());
        }
    }

    @GetMapping("/types/{typeId}/edit")
    public String showEditTypeForm(@PathVariable Integer typeId, Model model) {
        AssessmentType type = assessmentService.getAssessmentTypeById(typeId);
        
        if (type.getIsSystemDefault() != null && type.getIsSystemDefault()) {
            model.addAttribute("errorMessage", "Cannot edit system default assessment types");
            return "redirect:/counselor/assessment/types";
        }
       
        List<AssessmentQuestion> questions = 
            assessmentService.getApprovedQuestionsByType(typeId);

        List<AssessmentScoring> scorings = 
        assessmentScoringRepository.findByAssessmentTypeIdOrderByMinScoreAsc(typeId);
        
        model.addAttribute("assessmentType", type);
        model.addAttribute("questions", questions);
        model.addAttribute("scorings", scorings); 
        model.addAttribute("isEdit", true);
        
        return "symptoms-recognition/counselor-edit-assessment-type";
    }

    @PostMapping("/types/{typeId}/edit")
    public String submitEditTypeRequest(@PathVariable Integer typeId,
                                       @RequestParam String questionsJson,
                                       @RequestParam String scoringJson,
                                       @RequestParam String typeName,
                                       @RequestParam String description,
                                       @RequestParam String instructions,
                                       @RequestParam Integer totalQuestions,
                                       @RequestParam Integer maxScore,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        try {
            AssessmentType existingType = assessmentService.getAssessmentTypeById(typeId);
            
            AssessmentTypeRequest typeRequest = new AssessmentTypeRequest();
            typeRequest.setTypeCode(existingType.getTypeCode());
            typeRequest.setTypeName(typeName);
            typeRequest.setDescription(description);
            typeRequest.setInstructions(instructions);
            typeRequest.setTotalQuestions(totalQuestions);
            typeRequest.setMaxScore(maxScore);
            typeRequest.setScoringCriteria(scoringJson);
            
            List<QuestionRequest> questions = parseQuestionsJson(questionsJson);
            
            assessmentService.counselorSubmitAssessmentTypeUpdate(
                userDetails.getUsername(), typeId, typeRequest, questions);
            
            redirectAttributes.addFlashAttribute("successMessage",
                "Edit request submitted for admin approval!");
            return "redirect:/counselor/assessment/type-requests";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Failed to submit edit request: " + e.getMessage());
            return "redirect:/counselor/assessment/types/" + typeId + "/edit";
        }
    }

  @GetMapping("/types/{id}/details")
public String viewAssessmentTypeDetails(@PathVariable Integer id,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
    try {
        // Get assessment type using existing service method
        AssessmentType type = assessmentService.getAssessmentTypeById(id);
        
        // Get questions using repository
        List<AssessmentQuestion> questions = assessmentQuestionRepository
            .findByAssessmentTypeIdOrderByQuestionOrderAsc(id);
        
        // Get options for each question using repository
        for (AssessmentQuestion question : questions) {
            List<AssessmentOption> options = assessmentOptionRepository
                .findByQuestionId(question.getQuestionId());
            question.setOptions(options);
        }
        
        // Get scoring criteria using existing service method
        List<AssessmentScoring> scorings = assessmentService.getScoringByType(id);
        
        model.addAttribute("assessmentType", type);
        model.addAttribute("questions", questions);
        model.addAttribute("scorings", scorings);
        
        return "symptoms-recognition/counselor-assessment-type-details";
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", 
            "Failed to load assessment type details: " + e.getMessage());
        return "redirect:/counselor/assessment/types";
    }
}
}