package com.example.mentalhealth.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.mentalhealth.model.AssessmentOption;
import com.example.mentalhealth.model.AssessmentQuestion;
import com.example.mentalhealth.model.CounselorComment;
import com.example.mentalhealth.model.UserAssessment;
import com.example.mentalhealth.model.UserAssessmentAnswer;
import com.example.mentalhealth.repository.AssessmentOptionRepository;
import com.example.mentalhealth.repository.AssessmentQuestionRepository;
import com.example.mentalhealth.repository.CounselorCommentRepository;
import com.example.mentalhealth.repository.UserAssessmentAnswerRepository;
import com.example.mentalhealth.repository.UserAssessmentRepository;

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
    
    @Transactional
    public UserAssessment saveAssessment(Integer userId, List<Integer> answers, List<Integer> questionIds) {
        int totalScore = answers.stream()
            .mapToInt(Integer::intValue)
            .sum();
        
        String result = analyseAssessment(totalScore);
        String severity = getSeverityLevel(totalScore);
        
        UserAssessment assessment = new UserAssessment();
        assessment.setUserId(userId);
        assessment.setTotalScore(totalScore);
        assessment.setAssessmentResult(result);
        assessment.setSeverityLevel(severity);
        assessment.setAssessmentDate(LocalDateTime.now());
        
        UserAssessment savedAssessment = assessmentRepository.save(assessment);
        
        for (int i = 0; i < answers.size(); i++) {
            UserAssessmentAnswer answer = new UserAssessmentAnswer();
            answer.setAssessmentId(savedAssessment.getAssessmentId());
            answer.setQuestionId(questionIds.get(i));
            answer.setSelectedOptionId(answers.get(i) + 1);
            answer.setAnswerValue(answers.get(i));
            answerRepository.save(answer);
        }
        
        return savedAssessment;
    }
    
    public List<UserAssessment> getUserAssessmentHistory(Integer userId) {
        return assessmentRepository.findByUserIdOrderByAssessmentDateDesc(userId);
    }
    
    public UserAssessment getLatestAssessment(Integer userId) {
        List<UserAssessment> assessments = assessmentRepository
            .findByUserIdOrderByAssessmentDateDesc(userId);
        return assessments.isEmpty() ? null : assessments.get(0);
    }
   
    public List<UserAssessmentSummary> getAllUsersLatestAssessments() {
        List<UserAssessment> allAssessments = assessmentRepository.findAll();
        
        return allAssessments.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                UserAssessment::getUserId,
                java.util.stream.Collectors.maxBy(
                    java.util.Comparator.comparing(UserAssessment::getAssessmentDate)
                )
            ))
            .values().stream()
            .filter(java.util.Optional::isPresent)
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
            .collect(java.util.stream.Collectors.toList());
    }
    
    public List<UserAssessment> getAllAssessments() {
        return assessmentRepository.findAll();
    }
    
    public AssessmentStatistics getAssessmentStatistics() {
        List<UserAssessment> allAssessments = assessmentRepository.findAll();
        
        AssessmentStatistics stats = new AssessmentStatistics();
        stats.setTotalAssessments(allAssessments.size());
        
        long minimalCount = allAssessments.stream()
            .filter(a -> "minimal".equals(a.getSeverityLevel())).count();
        long mildCount = allAssessments.stream()
            .filter(a -> "mild".equals(a.getSeverityLevel())).count();
        long moderateCount = allAssessments.stream()
            .filter(a -> "moderate".equals(a.getSeverityLevel())).count();
        long severeCount = allAssessments.stream()
            .filter(a -> "severe".equals(a.getSeverityLevel())).count();
        
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
            String month = assessment.getAssessmentDate().format(
                DateTimeFormatter.ofPattern("yyyy-MM"));
            
            monthlyStats.putIfAbsent(month, new HashMap<>());
            Map<String, Integer> severityCounts = monthlyStats.get(month);
            
            severityCounts.merge(assessment.getSeverityLevel(), 1, Integer::sum);
        }
        
        return monthlyStats;
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
    
    private String analyseAssessment(int score) {
        if (score <= 4) {
            return "Minimal or no symptoms. You're doing well!";
        } else if (score <= 9) {
            return "Mild symptoms. Consider some self-care activities.";
        } else if (score <= 14) {
            return "Moderate symptoms. We recommend speaking with a counselor.";
        } else {
            return "Severe symptoms. Please seek professional help immediately.";
        }
    }
    
    private String getSeverityLevel(int score) {
        if (score <= 4) return "minimal";
        else if (score <= 9) return "mild";
        else if (score <= 14) return "moderate";
        else return "severe";
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
}