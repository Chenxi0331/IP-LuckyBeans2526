package com.example.mentalhealth.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.mentalhealth.model.AssessmentOption;
import com.example.mentalhealth.model.AssessmentQuestion;
import com.example.mentalhealth.model.CounselorComment;
import com.example.mentalhealth.model.User;
import com.example.mentalhealth.model.UserAssessment;
import com.example.mentalhealth.model.UserAssessmentAnswer;
import com.example.mentalhealth.repository.AssessmentOptionRepository;
import com.example.mentalhealth.repository.AssessmentQuestionRepository;
import com.example.mentalhealth.repository.CounselorCommentRepository;
import com.example.mentalhealth.repository.UserAssessmentAnswerRepository;
import com.example.mentalhealth.repository.UserAssessmentRepository;
import com.example.mentalhealth.repository.UserRepository;

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
        int totalScore = answers.stream().mapToInt(Integer::intValue).sum();

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
    Integer questionId = questionIds.get(i);
    Integer selectedIndex = answers.get(i); // 0..n-1

    List<AssessmentOption> options =
            optionRepository.findByQuestionId(questionId);

    if (selectedIndex == null
            || selectedIndex < 0
            || selectedIndex >= options.size()) {
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

    // ✅ Students should only see APPROVED questions
    public List<AssessmentQuestion> getApprovedQuestions() {
        return questionRepository.findByStatusOrderByQuestionOrderAsc("APPROVED");
    }

    // ✅ Always load options reliably (avoid lazy mapping surprises)
    public List<AssessmentOption> getQuestionOptions(Integer questionId) {
        return optionRepository.findByQuestionId(questionId);
    }

    // ✅ Dynamic max score: sum of (optionsCount - 1) for each approved question
        // Because your UI uses radio values 0..n-1; max points per question = n-1
    public int calculateMaxScoreForApprovedQuestions() {
        List<AssessmentQuestion> questions = getApprovedQuestions();
        int total = 0;
        for (AssessmentQuestion q : questions) {
            int count = optionRepository.findByQuestionId(q.getQuestionId()).size();
            if (count > 0) total += (count - 1);
        }
        return total;
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

    public AdminQuestionListPageData getAdminApprovedQuestionsPageData() {
        List<AssessmentQuestion> approved = questionRepository.findByStatusOrderByQuestionOrderAsc("APPROVED");
        long pending = questionRepository.countByStatus("PENDING");
        return new AdminQuestionListPageData(approved, pending);
    }

    public AdminPendingQuestionsPageData getAdminPendingQuestionsPageData() {
        List<AssessmentQuestion> pendingQuestions =
                questionRepository.findByStatusInOrderByCreatedAtDesc(Arrays.asList("PENDING", "PENDING_DELETE"));

        Set<Long> creatorIds = pendingQuestions.stream()
                .map(AssessmentQuestion::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> creators = userRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, String> counselorNames = new HashMap<>();
        Map<Long, String> counselorEmails = new HashMap<>();

        for (Long id : creatorIds) {
            User u = creators.get(id);
            if (u != null) {
                counselorNames.put(id, u.getFullName());
                counselorEmails.put(id, u.getEmail());
            }
        }

        return new AdminPendingQuestionsPageData(pendingQuestions, counselorNames, counselorEmails);
    }

    public AdminApprovalHistoryPageData getAdminApprovalHistoryPageData() {
        List<AssessmentQuestion> historyQuestions =
                questionRepository.findByStatusInOrderByCreatedAtDesc(Arrays.asList("APPROVED", "REJECTED"));

        Set<Long> creatorIds = historyQuestions.stream()
                .map(AssessmentQuestion::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> approverIds = historyQuestions.stream()
                .map(AssessmentQuestion::getApprovedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> allUserIds = new HashSet<>();
        allUserIds.addAll(creatorIds);
        allUserIds.addAll(approverIds);

        Map<Long, User> users = userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, String> counselorNames = new HashMap<>();
        Map<Long, String> counselorEmails = new HashMap<>();
        Map<Long, String> approverNames = new HashMap<>();

        for (Long id : creatorIds) {
            User u = users.get(id);
            if (u != null) {
                counselorNames.put(id, u.getFullName());
                counselorEmails.put(id, u.getEmail());
            }
        }

        for (Long id : approverIds) {
            User u = users.get(id);
            if (u != null) {
                approverNames.put(id, u.getFullName());
            }
        }

        return new AdminApprovalHistoryPageData(historyQuestions, counselorNames, counselorEmails, approverNames);
    }

    @Transactional
    public void adminAddQuestion(AssessmentQuestion question, List<String> optionTexts) {
        int nextOrder = ((Long) questionRepository.count()).intValue() + 1;
        question.setQuestionOrder(nextOrder);
        question.setStatus("APPROVED");

        AssessmentQuestion savedQuestion = questionRepository.save(question);

        for (String optionText : optionTexts) {
            AssessmentOption option = new AssessmentOption();
            option.setQuestionId(savedQuestion.getQuestionId());
            option.setOptionText(optionText);
            optionRepository.save(option);
        }
    }

    @Transactional
    public void adminApproveQuestion(Integer questionId, String adminEmail) {
        User admin = userService.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        AssessmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        question.setStatus("APPROVED");
        question.setApprovedBy(admin.getId());
        question.setApprovedAt(LocalDateTime.now());
        questionRepository.save(question);
    }

    @Transactional
    public void adminRejectQuestion(Integer questionId, String adminEmail) {
        User admin = userService.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        AssessmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        question.setStatus("REJECTED");
        question.setApprovedBy(admin.getId());
        question.setApprovedAt(LocalDateTime.now());
        questionRepository.save(question);
    }

    @Transactional
    public void adminApproveDeletion(Integer questionId, String adminEmail) {
        AssessmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        questionRepository.delete(question);
    }

    @Transactional
public void adminDeleteSubmissionQuestion(Integer questionId) {

    AssessmentQuestion question = questionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("Question not found"));

    List<AssessmentOption> options = optionRepository.findByQuestionId(questionId);
    optionRepository.deleteAll(options);

    questionRepository.delete(question);

    reorderQuestionOrders();
}

    @Transactional
public void reorderQuestionOrders() {
 
    List<AssessmentQuestion> questions =
            questionRepository.findByStatusOrderByQuestionOrderAsc("APPROVED");

    // List<AssessmentQuestion> questions = questionRepository.findAllByOrderByQuestionOrderAsc();

    int order = 1;
    for (AssessmentQuestion q : questions) {
        if (q.getQuestionOrder() == null || q.getQuestionOrder() != order) {
            q.setQuestionOrder(order);
        }
        order++;
    }
    questionRepository.saveAll(questions);
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

    public CounselorQuestionListPageData getCounselorQuestionsListPageData(String counselorEmail) {
        // approved questions
        List<AssessmentQuestion> approved =
                questionRepository.findByStatusOrderByQuestionOrderAsc("APPROVED");

        User counselor = userService.findByEmail(counselorEmail)
                .orElseThrow(() -> new RuntimeException("Counselor not found"));

        List<AssessmentQuestion> myPending = questionRepository
                .findByStatusInOrderByCreatedAtDesc(List.of("PENDING", "REJECTED"))
                .stream()
                .filter(q -> counselor.getId().equals(q.getCreatedBy()))
                .collect(Collectors.toList());

        return new CounselorQuestionListPageData(approved, myPending.size());
    }

    @Transactional
    public void counselorSubmitNewQuestion(String counselorEmail,
                                          AssessmentQuestion question,
                                          List<String> optionTexts) {
        User counselor = userService.findByEmail(counselorEmail)
                .orElseThrow(() -> new RuntimeException("Counselor not found"));

        question.setStatus("PENDING");
        question.setCreatedBy(counselor.getId());
        question.setCreatedAt(LocalDateTime.now());

        int nextOrder = ((Long) questionRepository.count()).intValue() + 1;
        question.setQuestionOrder(nextOrder);

        AssessmentQuestion saved = questionRepository.save(question);

        for (String optionText : optionTexts) {
            AssessmentOption option = new AssessmentOption();
            option.setQuestionId(saved.getQuestionId());
            option.setOptionText(optionText);
            optionRepository.save(option);
        }
    }

    @Transactional
    public void counselorSubmitEditQuestion(String counselorEmail,
                                           Integer questionId,
                                           AssessmentQuestion newQuestion,
                                           List<String> optionTexts) {
        User counselor = userService.findByEmail(counselorEmail)
                .orElseThrow(() -> new RuntimeException("Counselor not found"));

        AssessmentQuestion original = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        original.setQuestionText(newQuestion.getQuestionText());
        original.setStatus("PENDING");
        original.setCreatedBy(counselor.getId());
        original.setCreatedAt(LocalDateTime.now());

        questionRepository.save(original);

        // replace options
        List<AssessmentOption> oldOptions = optionRepository.findByQuestionId(questionId);
        optionRepository.deleteAll(oldOptions);

        for (String optionText : optionTexts) {
            AssessmentOption option = new AssessmentOption();
            option.setQuestionId(questionId);
            option.setOptionText(optionText);
            optionRepository.save(option);
        }
    }

    public AssessmentQuestion getQuestionWithOptions(Integer questionId) {
    AssessmentQuestion q = questionRepository.findById(questionId)
        .orElseThrow(() -> new RuntimeException("Question not found"));

    List<AssessmentOption> options = optionRepository.findByQuestionId(questionId);
    q.setOptions(options);
    return q;
}

@Transactional
public void adminEditQuestion(Integer questionId,
                              AssessmentQuestion newQuestion,
                              List<String> optionTexts) {

    AssessmentQuestion original = questionRepository.findById(questionId)
        .orElseThrow(() -> new RuntimeException("Question not found"));

    original.setQuestionText(newQuestion.getQuestionText());
    original.setStatus("APPROVED");

    questionRepository.save(original);

    List<AssessmentOption> oldOptions = optionRepository.findByQuestionId(questionId);
    optionRepository.deleteAll(oldOptions);

    for (String text : optionTexts) {
        if (text == null) continue;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) continue;

        AssessmentOption option = new AssessmentOption();
        option.setQuestionId(questionId);
        option.setOptionText(trimmed);
        optionRepository.save(option);
    }
}


    @Transactional
    public void counselorRequestDeleteQuestion(String counselorEmail, Integer questionId) {
        User counselor = userService.findByEmail(counselorEmail)
                .orElseThrow(() -> new RuntimeException("Counselor not found"));

        AssessmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        question.setStatus("PENDING_DELETE");
        question.setCreatedBy(counselor.getId());
        question.setCreatedAt(LocalDateTime.now());
        questionRepository.save(question);
    }

    public List<AssessmentQuestion> getCounselorMySubmissions(String counselorEmail) {
        User counselor = userService.findByEmail(counselorEmail)
                .orElseThrow(() -> new RuntimeException("Counselor not found"));

        return questionRepository.findByStatusInAndCreatedByOrderByCreatedAtDesc(
                Arrays.asList("PENDING", "PENDING_DELETE", "APPROVED", "REJECTED"),
                counselor.getId()
        );
    }


    public static class AdminQuestionListPageData {
        private final List<AssessmentQuestion> approvedQuestions;
        private final long pendingCount;

        public AdminQuestionListPageData(List<AssessmentQuestion> approvedQuestions, long pendingCount) {
            this.approvedQuestions = approvedQuestions;
            this.pendingCount = pendingCount;
        }

        public List<AssessmentQuestion> getApprovedQuestions() { return approvedQuestions; }
        public long getPendingCount() { return pendingCount; }
    }

    public static class AdminPendingQuestionsPageData {
        private final List<AssessmentQuestion> pendingQuestions;
        private final Map<Long, String> counselorNames;
        private final Map<Long, String> counselorEmails;

        public AdminPendingQuestionsPageData(List<AssessmentQuestion> pendingQuestions,
                                            Map<Long, String> counselorNames,
                                            Map<Long, String> counselorEmails) {
            this.pendingQuestions = pendingQuestions;
            this.counselorNames = counselorNames;
            this.counselorEmails = counselorEmails;
        }

        public List<AssessmentQuestion> getPendingQuestions() { return pendingQuestions; }
        public Map<Long, String> getCounselorNames() { return counselorNames; }
        public Map<Long, String> getCounselorEmails() { return counselorEmails; }
    }

    public static class AdminApprovalHistoryPageData {
        private final List<AssessmentQuestion> historyQuestions;
        private final Map<Long, String> counselorNames;
        private final Map<Long, String> counselorEmails;
        private final Map<Long, String> approverNames;

        public AdminApprovalHistoryPageData(List<AssessmentQuestion> historyQuestions,
                                            Map<Long, String> counselorNames,
                                            Map<Long, String> counselorEmails,
                                            Map<Long, String> approverNames) {
            this.historyQuestions = historyQuestions;
            this.counselorNames = counselorNames;
            this.counselorEmails = counselorEmails;
            this.approverNames = approverNames;
        }

        public List<AssessmentQuestion> getHistoryQuestions() { return historyQuestions; }
        public Map<Long, String> getCounselorNames() { return counselorNames; }
        public Map<Long, String> getCounselorEmails() { return counselorEmails; }
        public Map<Long, String> getApproverNames() { return approverNames; }
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

    public static class CounselorQuestionListPageData {
        private final List<AssessmentQuestion> approvedQuestions;
        private final int myPendingCount;

        public CounselorQuestionListPageData(List<AssessmentQuestion> approvedQuestions, int myPendingCount) {
            this.approvedQuestions = approvedQuestions;
            this.myPendingCount = myPendingCount;
        }

        public List<AssessmentQuestion> getApprovedQuestions() { return approvedQuestions; }
        public int getMyPendingCount() { return myPendingCount; }
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
}
