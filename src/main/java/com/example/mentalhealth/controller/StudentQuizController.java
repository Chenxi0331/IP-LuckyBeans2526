package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.repository.*;
import com.example.mentalhealth.service.SelfCareModuleProgressService;
import com.example.mentalhealth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/self-care")
public class StudentQuizController {
    
    @Autowired
    private ModuleQuizRepository quizRepository;
    
    @Autowired
    private StudentQuizAnswerRepository answerRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SelfCareModuleProgressService moduleProgressService;

    @PostMapping("/module/{id}/complete-content")
    @ResponseBody
    public String completeContent(@PathVariable Integer id,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Long userId = Long.valueOf(user.getId());
            moduleProgressService.markContentCompleted(userId, id);
            return "{\"success\": true}";
        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    @GetMapping("/module/{id}/quiz")
    public String viewQuiz(@PathVariable Integer id,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        
        User user = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        Long userId = Long.valueOf(user.getId());
        
        List<ModuleQuiz> quizzes = quizRepository.findByModuleIdAndStatusOrderByQuestionOrderAsc(id, "APPROVED");
        Long answeredCount = answerRepository.countByUserIdAndModuleId(userId, id);
        
        int quizSize = quizzes.size();
        boolean isCompleted = answeredCount >= quizSize;
        
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("moduleId", id);
        model.addAttribute("alreadyCompleted", isCompleted);
        
        return "self-care/module-quiz";
    }
    
    @PostMapping("/module/{id}/quiz/submit")
    public String submitQuiz(@PathVariable Integer id,
                            @RequestParam Map<String, String> answers,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        
        User user = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        Long userId = Long.valueOf(user.getId());
        List<ModuleQuiz> quizzes = quizRepository.findByModuleIdAndStatusOrderByQuestionOrderAsc(id, "APPROVED");
        
        int correctCount = 0;
        
        for (ModuleQuiz quiz : quizzes) {
            String selectedAnswer = answers.get("question_" + quiz.getQuizId());
            if (selectedAnswer != null) {
                boolean isCorrect = selectedAnswer.equalsIgnoreCase(quiz.getCorrectAnswer());
                if (isCorrect) {
                    correctCount = correctCount + 1;
                }
                
                if (!answerRepository.existsByUserIdAndQuizId(userId, quiz.getQuizId())) {
                    StudentQuizAnswer ans = new StudentQuizAnswer();
                    ans.setUserId(userId);
                    ans.setModuleId(id);
                    ans.setQuizId(quiz.getQuizId());
                    ans.setSelectedAnswer(selectedAnswer);
                    ans.setIsCorrect(isCorrect);
                    ans.setAnsweredAt(LocalDateTime.now());
                    answerRepository.save(ans);
                }
            }
        }

        moduleProgressService.markQuizCompleted(userId, id);
        
        redirectAttributes.addFlashAttribute("quizScore", correctCount);
        return "redirect:/self-care/module/" + id + "/quiz/result";
    }

    @GetMapping("/module/{id}/quiz/result")
    public String viewQuizResult(@PathVariable Integer id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User user = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        Long userId = Long.valueOf(user.getId());
        
        List<StudentQuizAnswer> userAnswers = answerRepository.findByUserIdAndModuleId(userId, id);
        List<ModuleQuiz> quizzes = quizRepository.findByModuleIdAndStatusOrderByQuestionOrderAsc(id, "APPROVED");
        
        Map<Integer, StudentQuizAnswer> answerMap = new HashMap<>();
        for (StudentQuizAnswer a : userAnswers) {
            answerMap.put(a.getQuizId(), a);
        }
        
        long correctAnswerCount = userAnswers.stream()
            .filter(StudentQuizAnswer::getIsCorrect)
            .count();
        
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("userAnswers", answerMap);
        model.addAttribute("correctCount", correctAnswerCount);
        model.addAttribute("totalQuestions", quizzes.size());
        model.addAttribute("moduleId", id);
        
        return "self-care/quiz-result";
    }
}