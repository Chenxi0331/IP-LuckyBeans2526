package com.example.mentalhealth.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AssessmentService {

    @Autowired
    private AssessmentOptionRepository optionRepository;

    @Autowired
    private UserAssessmentRepository assessmentRepository;

    @Autowired
    private UserAssessmentAnswerRepository answerRepository;

    @Autowired
    private AssessmentQuestionRepository questionRepository;

    @Autowired
    private CounselorCommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;
    

    @Autowired
    private UserService userService;
    
    @Autowired
    private AssessmentTypeRepository assessmentTypeRepository;
    
    @Autowired
    private AssessmentScoringRepository assessmentScoringRepository;
    
    @Autowired
    private AssessmentTypeRequestRepository typeRequestRepository;
    
    @Autowired
    private QuestionRequestRepository questionRequestRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<AssessmentType> getActiveAssessmentTypes() {
        return assessmentTypeRepository.findByIsActiveTrueAndStatusOrderByTypeNameAsc("APPROVED");
    }
    
    public List<AssessmentQuestion> getApprovedQuestionsByType(Integer assessmentTypeId) {
        return questionRepository.findByAssessmentTypeIdAndStatusOrderByQuestionOrderAsc(
            assessmentTypeId, "APPROVED");
    }
    
    public AssessmentType getAssessmentTypeById(Integer typeId) {
        return assessmentTypeRepository.findById(typeId)
            .orElseThrow(() -> new RuntimeException("Assessment type not found"));
    }
   
    @Transactional
    public void counselorSubmitAssessmentType(String counselorEmail,
                                             AssessmentTypeRequest typeRequest,
                                             List<QuestionRequest> questions) {
        User counselor = userService.findByEmail(counselorEmail)
                .orElseThrow(() -> new RuntimeException("Counselor not found"));
        
        if (assessmentTypeRepository.findByTypeCode(typeRequest.getTypeCode()) != null) {
            throw new RuntimeException("Assessment type code already exists");
        }
        
        typeRequest.setStatus("PENDING");
        typeRequest.setRequestedBy(counselor.getId());
        typeRequest.setRequestedAt(LocalDateTime.now());
        
        AssessmentTypeRequest savedRequest = typeRequestRepository.save(typeRequest);
        
        for (QuestionRequest question : questions) {
            question.setTypeRequestId(savedRequest.getRequestId());
            question.setCreatedAt(LocalDateTime.now());
            questionRequestRepository.save(question);
        }
    }
    
   
    @Transactional
    public void adminAddAssessmentType(AssessmentType assessmentType,
                                      List<AssessmentQuestion> questions,
                                      List<AssessmentScoring> scorings) {
        if (assessmentTypeRepository.findByTypeCode(assessmentType.getTypeCode()) != null) {
            throw new RuntimeException("Assessment type code already exists");
        }
        
        assessmentType.setStatus("APPROVED");
        assessmentType.setIsSystemDefault(false);
        assessmentType.setIsEditable(true);
        assessmentType.setIsActive(true);
        assessmentType.setCreatedAt(LocalDateTime.now());
        
        AssessmentType savedType = assessmentTypeRepository.save(assessmentType);
        
        for (AssessmentQuestion question : questions) {
            question.setAssessmentTypeId(savedType.getTypeId());
            question.setStatus("APPROVED");
            question.setIsEditable(false); // 标准化题目不可单独编辑
            question.setCreatedAt(LocalDateTime.now());
            
            AssessmentQuestion savedQuestion = questionRepository.save(question);
            
            if (question.getOptions() != null) {
                for (AssessmentOption option : question.getOptions()) {
                    option.setQuestionId(savedQuestion.getQuestionId());
                    optionRepository.save(option);
                }
            }
        }
        for (AssessmentScoring scoring : scorings) {
            scoring.setAssessmentTypeId(savedType.getTypeId());
            assessmentScoringRepository.save(scoring);
        }
    }
    
    
    @Transactional
    public void adminApproveAssessmentTypeRequest(Integer requestId, String adminEmail) {
        User admin = userService.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        AssessmentTypeRequest request = typeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
      
        if (assessmentTypeRepository.findByTypeCode(request.getTypeCode()) != null) {
            throw new RuntimeException("Assessment type code already exists");
        }
        
        AssessmentType assessmentType = new AssessmentType();
        assessmentType.setTypeCode(request.getTypeCode());
        assessmentType.setTypeName(request.getTypeName());
        assessmentType.setDescription(request.getDescription());
        assessmentType.setTotalQuestions(request.getTotalQuestions());
        assessmentType.setMaxScore(request.getMaxScore());
        assessmentType.setInstructions(request.getInstructions());
        assessmentType.setStatus("APPROVED");
        assessmentType.setIsSystemDefault(false);
        assessmentType.setIsEditable(true);
        assessmentType.setIsActive(true);
        assessmentType.setCreatedBy(request.getRequestedBy());
        assessmentType.setApprovedBy(admin.getId());
        assessmentType.setApprovedAt(LocalDateTime.now());
        assessmentType.setCreatedAt(LocalDateTime.now());
        
        AssessmentType savedType = assessmentTypeRepository.save(assessmentType);
        
        List<QuestionRequest> questionRequests = 
            questionRequestRepository.findByTypeRequestIdOrderByQuestionOrder(requestId);
        
        for (QuestionRequest qr : questionRequests) {
            AssessmentQuestion question = new AssessmentQuestion();
            question.setQuestionText(qr.getQuestionText());
            question.setQuestionOrder(qr.getQuestionOrder());
            question.setAssessmentTypeId(savedType.getTypeId());
            question.setStatus("APPROVED");
            question.setIsEditable(false);
            question.setCreatedBy(request.getRequestedBy());
            question.setCreatedAt(LocalDateTime.now());
            
            AssessmentQuestion savedQuestion = questionRepository.save(question);
            
            List<String> options = parseJsonOptions(qr.getOptions());
            for (String optionText : options) {
                AssessmentOption option = new AssessmentOption();
                option.setQuestionId(savedQuestion.getQuestionId());
                option.setOptionText(optionText);
                optionRepository.save(option);
            }
        }
        
        if (request.getScoringCriteria() != null && !request.getScoringCriteria().isEmpty()) {
            List<AssessmentScoring> scorings = parseScoringCriteria(
                request.getScoringCriteria(), savedType.getTypeId());
            assessmentScoringRepository.saveAll(scorings);
        }
        
        
        request.setStatus("APPROVED");
        request.setReviewedBy(admin.getId());
        request.setReviewedAt(LocalDateTime.now());
        typeRequestRepository.save(request);
    }
    
    /**
 * Update an assessment type (for toggling status, system protected, etc.)
 */
@Transactional
public void updateAssessmentType(AssessmentType type) {
    assessmentTypeRepository.save(type);
}

/**
 * Get scoring criteria by assessment type
 */
public List<AssessmentScoring> getScoringByType(Integer typeId) {
    return assessmentScoringRepository.findByAssessmentTypeIdOrderByMinScoreAsc(typeId);
}

// /**
//  * Admin directly updates an assessment type (bypassing approval)
//  * This method completely replaces questions and scoring
//  */
// @Transactional
// public void adminDirectUpdateAssessmentType(
//         Integer typeId,
//         AssessmentType updatedTypeInfo,
//         List<AssessmentQuestion> newQuestions,
//         List<AssessmentScoring> newScorings) {
    
//     // Get existing type
//     AssessmentType existingType = assessmentTypeRepository.findById(typeId)
//         .orElseThrow(() -> new RuntimeException("Assessment type not found"));
    
//     // Update basic info
//     existingType.setTypeName(updatedTypeInfo.getTypeName());
//     existingType.setDescription(updatedTypeInfo.getDescription());
//     existingType.setInstructions(updatedTypeInfo.getInstructions());
//     existingType.setTotalQuestions(updatedTypeInfo.getTotalQuestions());
//     existingType.setMaxScore(updatedTypeInfo.getMaxScore());
    
//     // Save updated type
//     assessmentTypeRepository.save(existingType);
    
//     // Delete existing questions and options (cascade will handle options)
//     List<AssessmentQuestion> existingQuestions = 
//         assessmentQuestionRepository.findByAssessmentTypeIdOrderByQuestionOrderAsc(typeId);
//     assessmentQuestionRepository.deleteAll(existingQuestions);
    
//     // Add new questions
//     for (AssessmentQuestion question : newQuestions) {
//         question.setAssessmentTypeId(typeId);
//         question.setStatus("APPROVED");
//         question.setIsEditable(existingType.getIsEditable());
//         question.setCreatedAt(LocalDateTime.now());
        
//         // Save question first
//         AssessmentQuestion savedQuestion = assessmentQuestionRepository.save(question);
        
//         // Save options
//         if (question.getOptions() != null) {
//             for (AssessmentOption option : question.getOptions()) {
//                 option.setQuestionId(savedQuestion.getQuestionId());
//                 assessmentOptionRepository.save(option);
//             }
//         }
//     }
    
//     // Delete existing scoring
//     assessmentScoringRepository.deleteByAssessmentTypeId(typeId);
    
//     // Add new scoring
//     for (AssessmentScoring scoring : newScorings) {
//         scoring.setAssessmentTypeId(typeId);
//         assessmentScoringRepository.save(scoring);
//     }
// }

    @Transactional
    public void adminRejectAssessmentTypeRequest(Integer requestId, String adminEmail, String notes) {
        User admin = userService.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        AssessmentTypeRequest request = typeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        request.setStatus("REJECTED");
        request.setReviewedBy(admin.getId());
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNotes(notes);
        typeRequestRepository.save(request);
    }
    
    public List<AssessmentTypeRequest> getPendingAssessmentTypeRequests() {
        return typeRequestRepository.findByStatusOrderByRequestedAtDesc("PENDING");
    }
    
    
    public List<QuestionRequest> getQuestionRequestsByTypeRequestId(Integer typeRequestId) {
        return questionRequestRepository.findByTypeRequestIdOrderByQuestionOrder(typeRequestId);
    }
    

    public List<AssessmentTypeRequest> getCounselorAssessmentTypeRequests(String counselorEmail) {
        User counselor = userService.findByEmail(counselorEmail)
                .orElseThrow(() -> new RuntimeException("Counselor not found"));
        return typeRequestRepository.findByRequestedByOrderByRequestedAtDesc(counselor.getId());
    }
    
 
@Transactional
public void counselorSubmitAssessmentTypeUpdate(String counselorEmail,
                                               Integer assessmentTypeId,
                                               AssessmentTypeRequest typeRequest,
                                               List<QuestionRequest> questions) {
    User counselor = userService.findByEmail(counselorEmail)
            .orElseThrow(() -> new RuntimeException("Counselor not found"));
    
    AssessmentType existingType = assessmentTypeRepository.findById(assessmentTypeId)
            .orElseThrow(() -> new RuntimeException("Assessment type not found"));
    
    if (existingType.getIsSystemDefault() != null && existingType.getIsSystemDefault()) {
        throw new RuntimeException("Cannot modify system default assessment types");
    }
    
    typeRequest.setRequestType("UPDATE");
    typeRequest.setAssessmentTypeId(assessmentTypeId);
    typeRequest.setStatus("PENDING");
    typeRequest.setRequestedBy(counselor.getId());
    typeRequest.setRequestedAt(LocalDateTime.now());
    
    AssessmentTypeRequest savedRequest = typeRequestRepository.save(typeRequest);
    
    for (QuestionRequest question : questions) {
        question.setTypeRequestId(savedRequest.getRequestId());
        question.setCreatedAt(LocalDateTime.now());
        questionRequestRepository.save(question);
    }
}


@Transactional
public void adminApproveAssessmentTypeUpdateRequest(Integer requestId, String adminEmail) {
    User admin = userService.findByEmail(adminEmail)
            .orElseThrow(() -> new RuntimeException("Admin not found"));
    
    AssessmentTypeRequest request = typeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found"));
    
    if (!"UPDATE".equals(request.getRequestType())) {
        throw new RuntimeException("This is not an update request");
    }
    
    AssessmentType assessmentType = assessmentTypeRepository.findById(request.getAssessmentTypeId())
            .orElseThrow(() -> new RuntimeException("Assessment type not found"));
    
    assessmentType.setTypeName(request.getTypeName());
    assessmentType.setDescription(request.getDescription());
    assessmentType.setTotalQuestions(request.getTotalQuestions());
    assessmentType.setMaxScore(request.getMaxScore());
    assessmentType.setInstructions(request.getInstructions());
    
    assessmentTypeRepository.save(assessmentType);
    
    List<AssessmentQuestion> oldQuestions = 
        questionRepository.findByAssessmentTypeIdOrderByQuestionOrderAsc(request.getAssessmentTypeId());
    
    for (AssessmentQuestion q : oldQuestions) {
        List<AssessmentOption> options = optionRepository.findByQuestionId(q.getQuestionId());
        optionRepository.deleteAll(options);
    }
    questionRepository.deleteAll(oldQuestions);
    
    List<QuestionRequest> questionRequests = 
        questionRequestRepository.findByTypeRequestIdOrderByQuestionOrder(requestId);
    
    for (QuestionRequest qr : questionRequests) {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setQuestionText(qr.getQuestionText());
        question.setQuestionOrder(qr.getQuestionOrder());
        question.setAssessmentTypeId(assessmentType.getTypeId());
        question.setStatus("APPROVED");
        question.setIsEditable(false);
        question.setCreatedBy(request.getRequestedBy());
        question.setCreatedAt(LocalDateTime.now());
        
        AssessmentQuestion savedQuestion = questionRepository.save(question);
        
        List<String> options = parseJsonOptions(qr.getOptions());
        for (String optionText : options) {
            AssessmentOption option = new AssessmentOption();
            option.setQuestionId(savedQuestion.getQuestionId());
            option.setOptionText(optionText);
            optionRepository.save(option);
        }
    }
    
    if (request.getScoringCriteria() != null && !request.getScoringCriteria().isEmpty()) {
        assessmentScoringRepository.deleteByAssessmentTypeId(assessmentType.getTypeId());
        List<AssessmentScoring> scorings = parseScoringCriteria(
            request.getScoringCriteria(), assessmentType.getTypeId());
        assessmentScoringRepository.saveAll(scorings);
    }
    
    request.setStatus("APPROVED");
    request.setReviewedBy(admin.getId());
    request.setReviewedAt(LocalDateTime.now());
    typeRequestRepository.save(request);
}


@Transactional
public void adminDirectUpdateAssessmentType(Integer assessmentTypeId,
                                           AssessmentType updatedType,
                                           List<AssessmentQuestion> questions,
                                           List<AssessmentScoring> scorings) {
    AssessmentType existingType = assessmentTypeRepository.findById(assessmentTypeId)
            .orElseThrow(() -> new RuntimeException("Assessment type not found"));
    
    if (existingType.getIsSystemDefault() != null && existingType.getIsSystemDefault()) {
        throw new RuntimeException("Cannot modify system default assessment types (PHQ-9, GAD-7, DASS-21)");
    }
    
    existingType.setTypeName(updatedType.getTypeName());
    existingType.setDescription(updatedType.getDescription());
    existingType.setTotalQuestions(updatedType.getTotalQuestions());
    existingType.setMaxScore(updatedType.getMaxScore());
    existingType.setInstructions(updatedType.getInstructions());
    
    assessmentTypeRepository.save(existingType);
    
    List<AssessmentQuestion> oldQuestions = 
        questionRepository.findByAssessmentTypeIdOrderByQuestionOrderAsc(assessmentTypeId);
    for (AssessmentQuestion q : oldQuestions) {
        List<AssessmentOption> options = optionRepository.findByQuestionId(q.getQuestionId());
        optionRepository.deleteAll(options);
    }
    questionRepository.deleteAll(oldQuestions);
    
    for (AssessmentQuestion question : questions) {
        question.setAssessmentTypeId(assessmentTypeId);
        question.setStatus("APPROVED");
        question.setIsEditable(false);
        question.setCreatedAt(LocalDateTime.now());
        
        AssessmentQuestion savedQuestion = questionRepository.save(question);
        
        if (question.getOptions() != null) {
            for (AssessmentOption option : question.getOptions()) {
                option.setQuestionId(savedQuestion.getQuestionId());
                optionRepository.save(option);
            }
        }
    }
    
    assessmentScoringRepository.deleteByAssessmentTypeId(assessmentTypeId);
    for (AssessmentScoring scoring : scorings) {
        scoring.setAssessmentTypeId(assessmentTypeId);
        assessmentScoringRepository.save(scoring);
    }
}
    @Transactional
    public void adminDeleteAssessmentType(Integer typeId) {
        AssessmentType type = assessmentTypeRepository.findById(typeId)
                .orElseThrow(() -> new RuntimeException("Assessment type not found"));
        
        if (type.getIsSystemDefault() != null && type.getIsSystemDefault()) {
            throw new RuntimeException("Cannot delete system default assessment types (PHQ-9, GAD-7, DASS-21)");
        }
        
    
        List<AssessmentQuestion> questions = 
            questionRepository.findByAssessmentTypeIdOrderByQuestionOrderAsc(typeId);
        
        for (AssessmentQuestion q : questions) {
            List<AssessmentOption> options = optionRepository.findByQuestionId(q.getQuestionId());
            optionRepository.deleteAll(options);
        }
        questionRepository.deleteAll(questions);
        
    
        assessmentScoringRepository.deleteByAssessmentTypeId(typeId);
        
       
        assessmentTypeRepository.delete(type);
    }
    
    
    public long getPendingAssessmentTypeRequestCount() {
        return typeRequestRepository.countByStatus("PENDING");
    }
    
public Map<String, Object> getAssessmentTypeRequestDetails(Integer requestId) {
    AssessmentTypeRequest request = typeRequestRepository.findById(requestId)
        .orElseThrow(() -> new RuntimeException("Request not found"));
    
    List<QuestionRequest> questions = questionRequestRepository
        .findByTypeRequestIdOrderByQuestionOrder(requestId);
    
    List<Map<String, Object>> enrichedQuestions = new ArrayList<>();
    for (QuestionRequest qr : questions) {
        Map<String, Object> questionMap = new HashMap<>();
        questionMap.put("questionText", qr.getQuestionText());
        questionMap.put("questionOrder", qr.getQuestionOrder());
        
        List<String> optionsList = parseJsonOptions(qr.getOptions());
        questionMap.put("optionsList", optionsList);
        questionMap.put("options", qr.getOptions());
        
        enrichedQuestions.add(questionMap);
    }
    
    List<Map<String, Object>> scoringList = null;
    if (request.getScoringCriteria() != null && !request.getScoringCriteria().isEmpty()) {
        try {
            scoringList = objectMapper.readValue(
                request.getScoringCriteria(), 
                new TypeReference<List<Map<String, Object>>>(){}
            );
        } catch (Exception e) {
            System.err.println("Failed to parse scoring criteria: " + e.getMessage());
        }
    }
    
    User requester = userRepository.findById(request.getRequestedBy()).orElse(null);
    
    Map<String, Object> details = new HashMap<>();
    details.put("request", request);
    details.put("questions", enrichedQuestions);
    details.put("scoringList", scoringList);  
    details.put("counselorName", requester != null ? requester.getFullName() : "Unknown");
    details.put("requesterEmail", requester != null ? requester.getEmail() : "Unknown");
    
    if (request.getReviewedBy() != null) {
        User reviewer = userRepository.findById(request.getReviewedBy()).orElse(null);
        details.put("reviewerName", reviewer != null ? reviewer.getFullName() : "Unknown");
    }
    
    return details;
}

    public AssessmentScoring getScoringResult(Integer assessmentTypeId, Integer score) {
        List<AssessmentScoring> scorings = 
            assessmentScoringRepository.findByAssessmentTypeIdOrderByMinScoreAsc(assessmentTypeId);
        
        for (AssessmentScoring scoring : scorings) {
            if (score >= scoring.getMinScore() && score <= scoring.getMaxScore()) {
                return scoring;
            }
        }
        
        return null;
    }
    
   public AssessmentTypeRequest getAssessmentTypeRequestById(Integer requestId) {
    return typeRequestRepository.findById(requestId)
        .orElseThrow(() -> new RuntimeException("Assessment type request not found"));
    }

    public List<AssessmentType> getAllAssessmentTypes() {
    return assessmentTypeRepository.findAll();
    }

    @Transactional
    public void toggleAssessmentTypeActive(Integer typeId) {
    AssessmentType type = assessmentTypeRepository.findById(typeId)
        .orElseThrow(() -> new RuntimeException("Assessment type not found"));
    
    type.setIsActive(!type.getIsActive());
    assessmentTypeRepository.save(type);
    }

    @Transactional
public void adminAddAssessmentTypeFromJson(String typeCode, String typeName, 
                                          String description, String instructions,
                                          Integer totalQuestions, Integer maxScore,
                                          String questionsJson, String scoringJson) {
    
    if (assessmentTypeRepository.findByTypeCode(typeCode) != null) {
        throw new RuntimeException("Assessment type code already exists: " + typeCode);
    }
    
    AssessmentType assessmentType = new AssessmentType();
    assessmentType.setTypeCode(typeCode);
    assessmentType.setTypeName(typeName);
    assessmentType.setDescription(description);
    assessmentType.setInstructions(instructions);
    assessmentType.setTotalQuestions(totalQuestions);
    assessmentType.setMaxScore(maxScore);
    assessmentType.setStatus("APPROVED");
    assessmentType.setIsSystemDefault(false);
    assessmentType.setIsEditable(true);
    assessmentType.setIsActive(true);
    
    AssessmentType savedType = assessmentTypeRepository.save(assessmentType);
    
    List<QuestionData> questions = parseQuestionsJsonData(questionsJson);
    for (QuestionData qData : questions) {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setAssessmentTypeId(savedType.getTypeId());
        question.setQuestionText(qData.questionText);
        question.setQuestionOrder(qData.questionOrder);
        question.setStatus("APPROVED");
        question.setIsEditable(false);
        question.setCreatedAt(LocalDateTime.now());
        
        AssessmentQuestion savedQuestion = questionRepository.save(question);
        
        List<String> optionTexts = parseJsonOptions(qData.options);
        for (String text : optionTexts) {
            AssessmentOption option = new AssessmentOption();
            option.setQuestionId(savedQuestion.getQuestionId());
            option.setOptionText(text);
            optionRepository.save(option);
        }
    }
    
    List<ScoringData> scorings = parseScoringJsonData(scoringJson);
    for (ScoringData sData : scorings) {
        AssessmentScoring scoring = new AssessmentScoring();
        scoring.setAssessmentTypeId(savedType.getTypeId());
        scoring.setSeverityLevel(sData.level);
        scoring.setMinScore(sData.min);
        scoring.setMaxScore(sData.max);
        scoring.setInterpretation(sData.interpretation);
        scoring.setRecommendations(sData.recommendations);
        assessmentScoringRepository.save(scoring);
    }
}


private List<QuestionData> parseQuestionsJsonData(String json) {
    try {
        List<Map<String, Object>> maps = objectMapper.readValue(
            json, new TypeReference<List<Map<String, Object>>>(){});
        
        List<QuestionData> result = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            QuestionData data = new QuestionData();
            data.questionText = (String) map.get("questionText");
            data.questionOrder = (Integer) map.get("questionOrder");
            data.options = (String) map.get("options");
            result.add(data);
        }
        return result;
    } catch (Exception e) {
        throw new RuntimeException("Failed to parse questions JSON: " + e.getMessage());
    }
}


private List<ScoringData> parseScoringJsonData(String json) {
    try {
        List<Map<String, Object>> maps = objectMapper.readValue(
            json, new TypeReference<List<Map<String, Object>>>(){});
        
        List<ScoringData> result = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            ScoringData data = new ScoringData();
            data.level = (String) map.get("level");
            data.min = (Integer) map.get("min");
            data.max = (Integer) map.get("max");
            data.interpretation = (String) map.get("interpretation");
            data.recommendations = (String) map.getOrDefault("recommendations", "");
            result.add(data);
        }
        return result;
    } catch (Exception e) {
        throw new RuntimeException("Failed to parse scoring JSON: " + e.getMessage());
    }
}




@Transactional
public UserAssessment saveAssessment(Integer userId, List<Integer> answers, 
                                     List<Integer> questionIds, Integer assessmentTypeId) {
    int totalScore = answers.stream().mapToInt(Integer::intValue).sum();

    AssessmentScoring scoring = getScoringResult(assessmentTypeId, totalScore);
    
    if (scoring == null) {
        throw new RuntimeException("No scoring criteria found for assessment type ID: " 
            + assessmentTypeId + " with score: " + totalScore);
    }
    
    String result = scoring.getInterpretation();
    String severity = scoring.getSeverityLevel();
    String recommendations = scoring.getRecommendations() != null 
        ? scoring.getRecommendations() 
        : getDefaultRecommendations(severity);

    UserAssessment assessment = new UserAssessment();
    assessment.setUserId(userId);
    assessment.setAssessmentTypeId(assessmentTypeId);
    assessment.setTotalScore(totalScore);
    assessment.setAssessmentResult(result);
    assessment.setSeverityLevel(severity);
    assessment.setAssessmentDate(LocalDateTime.now());

    UserAssessment savedAssessment = assessmentRepository.save(assessment);

    for (int i = 0; i < answers.size(); i++) {
        Integer questionId = questionIds.get(i);
        Integer selectedIndex = answers.get(i);

        List<AssessmentOption> options = optionRepository.findByQuestionId(questionId);

        if (selectedIndex == null || selectedIndex < 0 || selectedIndex >= options.size()) {
            throw new RuntimeException("Invalid answer index for questionId=" + questionId);
        }

        Integer realOptionId = options.get(selectedIndex).getOptionId();

        UserAssessmentAnswer answer = new UserAssessmentAnswer();
        answer.setAssessmentId(savedAssessment.getAssessmentId());
        answer.setQuestionId(questionId);
        answer.setSelectedOptionId(realOptionId);
        answer.setAnswerValue(selectedIndex);
        answerRepository.save(answer);
    }

    return savedAssessment;
}

    private List<String> parseJsonOptions(String jsonOptions) {
        try {
            return objectMapper.readValue(jsonOptions, new TypeReference<List<String>>(){});
        } catch (Exception e) {
            jsonOptions = jsonOptions.replace("[", "").replace("]", "")
                .replace("\"", "").trim();
            return Arrays.asList(jsonOptions.split(","));
        }
    }
    

    private List<AssessmentScoring> parseScoringCriteria(String jsonCriteria, Integer typeId) {
        try {
            List<Map<String, Object>> criteriaList = 
                objectMapper.readValue(jsonCriteria, new TypeReference<List<Map<String, Object>>>(){});
            
            List<AssessmentScoring> scorings = new ArrayList<>();
            for (Map<String, Object> criteria : criteriaList) {
                AssessmentScoring scoring = new AssessmentScoring();
                scoring.setAssessmentTypeId(typeId);
                scoring.setSeverityLevel((String) criteria.get("level"));
                scoring.setMinScore((Integer) criteria.get("min"));
                scoring.setMaxScore((Integer) criteria.get("max"));
                scoring.setInterpretation((String) criteria.get("interpretation"));
                scoring.setRecommendations((String) criteria.getOrDefault("recommendations", ""));
                scorings.add(scoring);
            }
            return scorings;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse scoring criteria: " + e.getMessage());
        }
    }

    
    public List<AssessmentQuestion> getAllQuestions() {
        return questionRepository.findAllByOrderByQuestionOrderAsc();
    }

    public AssessmentQuestion getQuestionById(Integer questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found: " + questionId));
    }

    public int getTotalQuestions() {
        return (int) questionRepository.count();
    }

    /**
    * Get user assessment history with type names enriched
    */
    public List<UserAssessment> getUserAssessmentHistoryWithTypeNames(Integer userId) {
        List<UserAssessment> assessments = getUserAssessmentHistory(userId);
        enrichAssessmentsWithTypeNames(assessments);
        return assessments;
    }

    /**
    * Get latest assessment with type name
    */
    public UserAssessment getLatestAssessmentWithTypeName(Integer userId) {
     UserAssessment latest = getLatestAssessment(userId);
    
        if (latest != null && latest.getAssessmentTypeId() != null) {
        AssessmentType type = getAssessmentTypeById(latest.getAssessmentTypeId());
            if (type != null) {
                latest.setAssessmentTypeName(type.getTypeName());
            }
        }
    
        return latest;
    }

    /**
    * Helper method to enrich assessments with type names
    */
private void enrichAssessmentsWithTypeNames(List<UserAssessment> assessments) {
    for (UserAssessment assessment : assessments) {
        if (assessment.getAssessmentTypeId() != null) {
            try {
                AssessmentType type = getAssessmentTypeById(assessment.getAssessmentTypeId());
                if (type != null) {
                    assessment.setAssessmentTypeName(type.getTypeName());
                    assessment.setMaxScore(type.getMaxScore());
                    
                    AssessmentScoring scoring = getScoringResult(
                        assessment.getAssessmentTypeId(), 
                        assessment.getTotalScore()
                    );
                    if (scoring != null && scoring.getRecommendations() != null) {
                        assessment.setRecommendations(scoring.getRecommendations());
                    } else if (scoring != null) {
                        assessment.setRecommendations(
                            getDefaultRecommendations(scoring.getSeverityLevel())
                        );
                    }
                }
            } catch (RuntimeException e) {
                assessment.setAssessmentTypeName("Unknown");
            }
        }
    }
}
    public List<UserAssessment> getUserAssessmentHistory(Integer userId) {
        return assessmentRepository.findByUserIdOrderByAssessmentDateDesc(userId);
    }

    public UserAssessment getLatestAssessment(Integer userId) {
        List<UserAssessment> assessments = assessmentRepository.findByUserIdOrderByAssessmentDateDesc(userId);
        return assessments.isEmpty() ? null : assessments.get(0);
    }

    public List<UserAssessmentSummary> getAllUsersLatestAssessments() {
        List<UserAssessment> allAssessments = assessmentRepository.findAll();

        return allAssessments.stream()
                .collect(Collectors.groupingBy(
                        UserAssessment::getUserId,
                        Collectors.maxBy(Comparator.comparing(UserAssessment::getAssessmentDate))
                ))
                .values().stream()
                .filter(Optional::isPresent)
                .map(opt -> {
                    UserAssessment assessment = opt.get();
                    UserAssessmentSummary summary = new UserAssessmentSummary();
                    summary.setUserId(assessment.getUserId());
                    summary.setLatestScore(assessment.getTotalScore());
                    summary.setSeverityLevel(assessment.getSeverityLevel());
                    summary.setAssessmentDate(assessment.getAssessmentDate());
                    summary.setAssessmentResult(assessment.getAssessmentResult());
                    return summary;
                })
                .collect(Collectors.toList());
    }

    public List<UserAssessment> getAllAssessments() {
        return assessmentRepository.findAll();
    }

    public AssessmentStatistics getAssessmentStatistics() {
        List<UserAssessment> allAssessments = assessmentRepository.findAll();

        AssessmentStatistics stats = new AssessmentStatistics();
        stats.setTotalAssessments(allAssessments.size());

        long minimalCount = allAssessments.stream().filter(a -> "minimal".equals(a.getSeverityLevel())).count();
        long mildCount = allAssessments.stream().filter(a -> "mild".equals(a.getSeverityLevel())).count();
        long moderateCount = allAssessments.stream().filter(a -> "moderate".equals(a.getSeverityLevel())).count();
        long severeCount = allAssessments.stream().filter(a -> "severe".equals(a.getSeverityLevel())).count();

        stats.setMinimalCount((int) minimalCount);
        stats.setMildCount((int) mildCount);
        stats.setModerateCount((int) moderateCount);
        stats.setSevereCount((int) severeCount);

        if (allAssessments.size() > 0) {
            stats.setMinimalPercentage((int) (minimalCount * 100.0 / allAssessments.size()));
            stats.setMildPercentage((int) (mildCount * 100.0 / allAssessments.size()));
            stats.setModeratePercentage((int) (moderateCount * 100.0 / allAssessments.size()));
            stats.setSeverePercentage((int) (severeCount * 100.0 / allAssessments.size()));
        }

        return stats;
    }

    public UserAssessment getAssessmentById(Integer assessmentId) {
        return assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found: " + assessmentId));
    }

    public List<UserAssessmentAnswer> getAssessmentAnswers(Integer assessmentId) {
        return answerRepository.findByAssessmentId(assessmentId);
    }

    public AssessmentOption getOptionById(Integer optionId) {
        return optionRepository.findById(optionId)
                .orElseThrow(() -> new RuntimeException("Option not found: " + optionId));
    }

    public Map<String, Map<String, Integer>> getMonthlyStatistics() {
        List<UserAssessment> allAssessments = assessmentRepository.findAll();

        Map<String, Map<String, Integer>> monthlyStats = new HashMap<>();

        for (UserAssessment assessment : allAssessments) {
            String month = assessment.getAssessmentDate()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM"));

            monthlyStats.putIfAbsent(month, new HashMap<>());
            Map<String, Integer> severityCounts = monthlyStats.get(month);

            severityCounts.merge(assessment.getSeverityLevel(), 1, Integer::sum);
        }

        return monthlyStats;
    }

    public List<AssessmentOption> getQuestionOptions(Integer questionId) {
        return optionRepository.findByQuestionId(questionId);
    }

    @Transactional
    public CounselorComment addComment(Long counselorId, Long studentId, Integer assessmentId,
                                      String commentText, Boolean visibleToStudent) {
        CounselorComment comment = new CounselorComment();
        comment.setCounselorId(counselorId);
        comment.setStudentId(studentId);
        comment.setAssessmentId(assessmentId);
        comment.setCommentText(commentText);
        comment.setIsVisibleToStudent(visibleToStudent != null ? visibleToStudent : true);

        return commentRepository.save(comment);
    }

    public List<CounselorComment> getStudentComments(Long studentId) {
        return commentRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    public List<CounselorComment> getVisibleStudentComments(Long studentId) {
        return commentRepository.findByStudentIdAndIsVisibleToStudentTrueOrderByCreatedAtDesc(studentId);
    }

    public List<CounselorComment> getAssessmentComments(Integer assessmentId) {
        return commentRepository.findByAssessmentIdOrderByCreatedAtDesc(assessmentId);
    }

    public long getPendingQuestionCount() {
        return questionRepository.countByStatus("PENDING");
    }

    public List<UserAssessmentSummary> getAllUsersLatestAssessmentsWithUserInfo() {
        List<UserAssessmentSummary> summaries = getAllUsersLatestAssessments();

        Set<Long> userIds = summaries.stream()
                .map(UserAssessmentSummary::getUserId)
                .filter(Objects::nonNull)
                .map(Integer::longValue)
                .collect(Collectors.toSet());

        Map<Long, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        for (UserAssessmentSummary s : summaries) {
            User u = users.get(s.getUserId().longValue());
            if (u != null) {
                s.setUserName(u.getFullName());
                s.setUserEmail(u.getEmail());
            }
        }

        return summaries;
    }

    public CounselorStudentPageData getCounselorStudentPageData(Integer userId) {
        User student = userService.findById(userId.longValue())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<UserAssessment> assessments = getUserAssessmentHistory(userId);
        List<CounselorComment> comments = getStudentComments(userId.longValue());

        List<String> dates = assessments.stream()
                .map(a -> a.getAssessmentDate().toLocalDate().toString())
                .collect(Collectors.toList());

        List<Integer> scores = assessments.stream()
                .map(UserAssessment::getTotalScore)
                .collect(Collectors.toList());

        return new CounselorStudentPageData(student, assessments, comments, dates, scores);
    }

    public CounselorAssessmentDetailsPageData getCounselorAssessmentDetailsPageData(Integer assessmentId) {
        UserAssessment assessment = getAssessmentById(assessmentId);
        List<UserAssessmentAnswer> answers = getAssessmentAnswers(assessmentId);

        User student = userService.findById(assessment.getUserId().longValue())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Set<Integer> questionIds = answers.stream()
                .map(UserAssessmentAnswer::getQuestionId)
                .collect(Collectors.toSet());

        Set<Integer> optionIds = answers.stream()
            .map(UserAssessmentAnswer::getSelectedOptionId)
            .collect(Collectors.toSet());

    Map<Integer, AssessmentQuestion> questionMap = questionRepository.findAllById(questionIds).stream()
            .collect(Collectors.toMap(AssessmentQuestion::getQuestionId, Function.identity()));

    Map<Integer, AssessmentOption> optionMap = optionRepository.findAllById(optionIds).stream()
            .collect(Collectors.toMap(AssessmentOption::getOptionId, Function.identity()));

    Map<Integer, String> questionTexts = new HashMap<>();
    Map<Integer, String> answerTexts = new HashMap<>();

    for (UserAssessmentAnswer a : answers) {
        AssessmentQuestion q = questionMap.get(a.getQuestionId());
        if (q != null) questionTexts.put(a.getQuestionId(), q.getQuestionText());

        AssessmentOption op = optionMap.get(a.getSelectedOptionId());
        if (op != null) answerTexts.put(a.getQuestionId(), op.getOptionText());
    }

    List<CounselorComment> comments = getAssessmentComments(assessmentId);

    return new CounselorAssessmentDetailsPageData(
            assessment, student, answers, questionTexts, answerTexts, comments
    );
}

public List<UserAssessmentAnswer> getAnswersByAssessmentId(Integer assessmentId) {
    return answerRepository.findByAssessmentId(assessmentId);
}

public Map<String, Object> getStudentTrendData(Integer userId) {
    List<UserAssessment> assessments = getUserAssessmentHistory(userId);
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

@Transactional
public void addCounselorCommentByEmail(String counselorEmail,
                                      Long studentId,
                                      Integer assessmentId,
                                      String commentText,
                                      Boolean visibleToStudent) {
    User counselor = userService.findByEmail(counselorEmail)
            .orElseThrow(() -> new RuntimeException("Counselor not found"));

    addComment(counselor.getId(), studentId, assessmentId, commentText, visibleToStudent);
}

private String getDefaultRecommendations(String severity) {
    switch (severity) {
        case "minimal":
            return "Continue monitoring your mental health and maintain healthy habits.";
        case "mild":
            return "Consider engaging in self-care activities and speaking with someone you trust.";
        case "moderate":
            return "We recommend speaking with a counselor to discuss your symptoms.";
        case "severe":
            return "Please seek professional help immediately. Contact a mental health professional or counselor.";
        default:
            return "";
    }
}

// private String analyseAssessment(int score) {
//     if (score <= 4) {
//         return "Minimal or no symptoms. You're doing well!";
//     } else if (score <= 9) {
//         return "Mild symptoms. Consider some self-care activities.";
//     } else if (score <= 14) {
//         return "Moderate symptoms. We recommend speaking with a counselor.";
//     } else {
//         return "Severe symptoms. Please seek professional help immediately.";
//     }
// }

// private String getSeverityLevel(int score) {
//     if (score <= 4) return "minimal";
//     else if (score <= 9) return "mild";
//     else if (score <= 14) return "moderate";
//     else return "severe";
// }

// ==================== Inner Classes ====================
/**
 * Get filtered and sorted assessments with enriched data
 */
public List<UserAssessment> getFilteredAssessments(
        String search,
        String type,
        String severity,
        LocalDate dateFrom,
        LocalDate dateTo,
        String sortBy) {
    
    // Get all assessments
    List<UserAssessment> allAssessments = assessmentRepository.findAll();
    
    // Enrich with user and type data
    enrichAssessmentsWithUserAndTypeData(allAssessments);
    
    // Apply filters
    List<UserAssessment> filteredAssessments = filterAssessments(
        allAssessments, search, type, severity, dateFrom, dateTo);
    
    // Apply sorting
    sortAssessments(filteredAssessments, sortBy);
    
    return filteredAssessments;
}

/**
 * Enrich assessments with user name, email, and assessment type info
 */
private void enrichAssessmentsWithUserAndTypeData(List<UserAssessment> assessments) {
    for (UserAssessment assessment : assessments) {
        // Get user information
        userRepository.findById(assessment.getUserId().longValue()).ifPresent(user -> {
            assessment.setUserName(user.getFullName());
            assessment.setUserEmail(user.getEmail());
        });
        
        // Get assessment type information
        if (assessment.getAssessmentTypeId() != null) {
            try {
                AssessmentType type = getAssessmentTypeById(assessment.getAssessmentTypeId());
                assessment.setAssessmentTypeName(type.getTypeName());
                assessment.setMaxScore(type.getMaxScore());
            } catch (RuntimeException e) {
                assessment.setAssessmentTypeName("Unknown Type");
            }
        }
    }
}

/**
 * Filter assessments based on criteria
 */
private List<UserAssessment> filterAssessments(
        List<UserAssessment> assessments,
        String search,
        String type,
        String severity,
        LocalDate dateFrom,
        LocalDate dateTo) {
    
    return assessments.stream()
        // Search filter
        .filter(a -> applySearchFilter(a, search))
        // Type filter
        .filter(a -> applyTypeFilter(a, type))
        // Severity filter
        .filter(a -> applySeverityFilter(a, severity))
        // Date from filter
        .filter(a -> applyDateFromFilter(a, dateFrom))
        // Date to filter
        .filter(a -> applyDateToFilter(a, dateTo))
        .collect(Collectors.toList());
}

/**
 * Apply search filter (student name or email)
 */
private boolean applySearchFilter(UserAssessment assessment, String search) {
    if (search == null || search.trim().isEmpty()) {
        return true;
    }
    
    String searchLower = search.toLowerCase().trim();
    boolean matchesName = assessment.getUserName() != null && 
                         assessment.getUserName().toLowerCase().contains(searchLower);
    boolean matchesEmail = assessment.getUserEmail() != null && 
                          assessment.getUserEmail().toLowerCase().contains(searchLower);
    
    return matchesName || matchesEmail;
}

/**
 * Apply assessment type filter
 */
private boolean applyTypeFilter(UserAssessment assessment, String type) {
    if (type == null || type.isEmpty()) {
        return true;
    }
    
    return assessment.getAssessmentTypeName() != null && 
           assessment.getAssessmentTypeName().contains(type);
}

/**
 * Apply severity level filter
 */
private boolean applySeverityFilter(UserAssessment assessment, String severity) {
    if (severity == null || severity.isEmpty()) {
        return true;
    }
    
    return assessment.getSeverityLevel() != null && 
           assessment.getSeverityLevel().equals(severity);
}

/**
 * Apply date from filter
 */
private boolean applyDateFromFilter(UserAssessment assessment, LocalDate dateFrom) {
    if (dateFrom == null) {
        return true;
    }
    
    LocalDate assessmentDate = assessment.getAssessmentDate().toLocalDate();
    return !assessmentDate.isBefore(dateFrom);
}

/**
 * Apply date to filter
 */
private boolean applyDateToFilter(UserAssessment assessment, LocalDate dateTo) {
    if (dateTo == null) {
        return true;
    }
    
    LocalDate assessmentDate = assessment.getAssessmentDate().toLocalDate();
    return !assessmentDate.isAfter(dateTo);
}

/**
 * Sort assessments based on sort criteria
 */
private void sortAssessments(List<UserAssessment> assessments, String sortBy) {
    if (sortBy == null || sortBy.isEmpty()) {
        sortBy = "date_desc";
    }
    
    switch (sortBy) {
        case "date_asc":
            assessments.sort(Comparator.comparing(UserAssessment::getAssessmentDate));
            break;
        case "score_desc":
            assessments.sort(Comparator.comparing(UserAssessment::getTotalScore).reversed());
            break;
        case "score_asc":
            assessments.sort(Comparator.comparing(UserAssessment::getTotalScore));
            break;
        case "name_asc":
            assessments.sort(Comparator.comparing(
                a -> a.getUserName() != null ? a.getUserName() : "", 
                String.CASE_INSENSITIVE_ORDER));
            break;
        case "name_desc":
        assessments.sort((a1, a2) -> {
            String name1 = a1.getUserName() != null ? a1.getUserName() : "";
            String name2 = a2.getUserName() != null ? a2.getUserName() : "";
            return String.CASE_INSENSITIVE_ORDER.compare(name2, name1);
        });
        break;
        default: // date_desc
            assessments.sort(Comparator.comparing(UserAssessment::getAssessmentDate).reversed());
    }
}

/**
 * Get total count of all assessments
 */
public int getTotalAssessmentCount() {
    return (int) assessmentRepository.count();
}
public static class UserAssessmentSummary {
    private Integer userId;
    private String userName;
    private String userEmail;
    private Integer latestScore;
    private String severityLevel;
    private LocalDateTime assessmentDate;
    private String assessmentResult;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Integer getLatestScore() { return latestScore; }
    public void setLatestScore(Integer latestScore) { this.latestScore = latestScore; }

    public String getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(String severityLevel) { this.severityLevel = severityLevel; }

    public LocalDateTime getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(LocalDateTime assessmentDate) { this.assessmentDate = assessmentDate; }

    public String getAssessmentResult() { return assessmentResult; }
    public void setAssessmentResult(String assessmentResult) { this.assessmentResult = assessmentResult; }
}

public static class AssessmentStatistics {
    private Integer totalAssessments;
    private Integer minimalCount;
    private Integer mildCount;
    private Integer moderateCount;
    private Integer severeCount;
    private Integer minimalPercentage;
    private Integer mildPercentage;
    private Integer moderatePercentage;
    private Integer severePercentage;

    public Integer getTotalAssessments() { return totalAssessments; }
    public void setTotalAssessments(Integer totalAssessments) { this.totalAssessments = totalAssessments; }

    public Integer getMinimalCount() { return minimalCount; }
    public void setMinimalCount(Integer minimalCount) { this.minimalCount = minimalCount; }

    public Integer getMildCount() { return mildCount; }
    public void setMildCount(Integer mildCount) { this.mildCount = mildCount; }

    public Integer getModerateCount() { return moderateCount; }
    public void setModerateCount(Integer moderateCount) { this.moderateCount = moderateCount; }

    public Integer getSevereCount() { return severeCount; }
    public void setSevereCount(Integer severeCount) { this.severeCount = severeCount; }

    public Integer getMinimalPercentage() { return minimalPercentage; }
    public void setMinimalPercentage(Integer minimalPercentage) { this.minimalPercentage = minimalPercentage; }

    public Integer getMildPercentage() { return mildPercentage; }
    public void setMildPercentage(Integer mildPercentage) { this.mildPercentage = mildPercentage; }

    public Integer getModeratePercentage() { return moderatePercentage; }
    public void setModeratePercentage(Integer moderatePercentage) { this.moderatePercentage = moderatePercentage; }

    public Integer getSeverePercentage() { return severePercentage; }
    public void setSeverePercentage(Integer severePercentage) { this.severePercentage = severePercentage; }
}


public static class CounselorStudentPageData {
    private final User student;
    private final List<UserAssessment> assessments;
    private final List<CounselorComment> comments;
    private final List<String> trendDates;
    private final List<Integer> trendScores;

    public CounselorStudentPageData(User student,
                                    List<UserAssessment> assessments,
                                    List<CounselorComment> comments,
                                    List<String> trendDates,
                                    List<Integer> trendScores) {
        this.student = student;
        this.assessments = assessments;
        this.comments = comments;
        this.trendDates = trendDates;
        this.trendScores = trendScores;
    }

    public User getStudent() { return student; }
    public List<UserAssessment> getAssessments() { return assessments; }
    public List<CounselorComment> getComments() { return comments; }
    public List<String> getTrendDates() { return trendDates; }
    public List<Integer> getTrendScores() { return trendScores; }
}

public static class CounselorAssessmentDetailsPageData {
    private final UserAssessment assessment;
    private final User student;
    private final List<UserAssessmentAnswer> answers;
    private final Map<Integer, String> questionTexts;
    private final Map<Integer, String> answerTexts;
    private final List<CounselorComment> comments;

    public CounselorAssessmentDetailsPageData(UserAssessment assessment,
                                              User student,
                                              List<UserAssessmentAnswer> answers,
                                              Map<Integer, String> questionTexts,
                                              Map<Integer, String> answerTexts,
                                              List<CounselorComment> comments) {
        this.assessment = assessment;
        this.student = student;
        this.answers = answers;
        this.questionTexts = questionTexts;
        this.answerTexts = answerTexts;
        this.comments = comments;
    }

    public UserAssessment getAssessment() { return assessment; }
    public User getStudent() { return student; }
    public List<UserAssessmentAnswer> getAnswers() { return answers; }
    public Map<Integer, String> getQuestionTexts() { return questionTexts; }
    public Map<Integer, String> getAnswerTexts() { return answerTexts; }
    public List<CounselorComment> getComments() { return comments; }
    }
    private static class QuestionData {
    String questionText;
    Integer questionOrder;
    String options; 
    }

    private static class ScoringData {
    String level;
    Integer min;
    Integer max;
    String interpretation;
    String recommendations;
    }
}