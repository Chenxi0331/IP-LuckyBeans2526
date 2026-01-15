package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.repository.*;
import com.example.mentalhealth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/counselor/modules")
@PreAuthorize("hasAuthority('ROLE_COUNSELOR')")
public class CounselorModuleController {
    
    @Autowired
    private ModuleEditRequestRepository editRequestRepository;
    
    @Autowired
    private SelfCareModuleRepository moduleRepository;
    
    @Autowired
    private ModuleQuizRepository quizRepository;
    
    @Autowired
    private ModuleEditRequestQuizRepository requestQuizRepository;
    
    @Autowired
    private UserService userService;
    
    private static final String UPLOAD_DIR = "uploads/";
    
    @GetMapping
    public String listModules(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User counselor = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Counselor not found"));
        
        List<SelfCareModule> modules = moduleRepository.findAll();
        List<ModuleEditRequest> pendingRequests = editRequestRepository
            .findByRequestedByOrderByRequestedAtDesc(counselor.getUserId().longValue());
        
        model.addAttribute("modules", modules);
        model.addAttribute("pendingRequests", pendingRequests);
        
        return "self-care/counselor-module-list";
    }
    
    @GetMapping("/request-edit/{id}")
    public String showEditRequestForm(@PathVariable Integer id, Model model) {
        SelfCareModule module = moduleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Module not found"));
        
        ModuleEditRequest request = new ModuleEditRequest();
        request.setModuleId(module.getModuleId());
        request.setTitle(module.getTitle());
        request.setDescription(module.getDescription());
        request.setCategory(module.getCategory());
        request.setLevel(module.getLevel());
        request.setDuration(module.getDuration());
        request.setIcon(module.getIcon());
        request.setContentType(module.getContentType());
        request.setVideoUrl(module.getVideoUrl());
        request.setVideoFilePath(module.getVideoFilePath());
        request.setPdfFilePath(module.getPdfFilePath());
        request.setTextContent(module.getTextContent());
        request.setRequestType("UPDATE");
        
        model.addAttribute("request", request);
        model.addAttribute("module", module);
        model.addAttribute("requestType", "UPDATE");
        model.addAttribute("isEdit", true);
        
        return "self-care/counselor-module-request-form";
    }
    
    @GetMapping("/request-create")
    public String showCreateRequestForm(Model model) {
        model.addAttribute("request", new ModuleEditRequest());
        model.addAttribute("module", null);
        model.addAttribute("requestType", "CREATE");
        model.addAttribute("isEdit", false);
        
        return "self-care/counselor-module-request-form";
    }
    
    @PostMapping("/submit-request")
    public String submitModuleRequest(@ModelAttribute ModuleEditRequest request,
                                     @RequestParam(required = false) MultipartFile videoFile,
                                     @RequestParam(required = false) MultipartFile pdfFile,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) throws IOException {
        
        User counselor = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Counselor not found"));
        
        if (videoFile != null && !videoFile.isEmpty()) {
            String videoPath = saveFile(videoFile, "videos");
            request.setVideoFilePath(videoPath);
            request.setContentType("video_file");
        } else if (pdfFile != null && !pdfFile.isEmpty()) {
            String pdfPath = saveFile(pdfFile, "pdfs");
            request.setPdfFilePath(pdfPath);
            request.setContentType("pdf");
        } else if (request.getVideoUrl() != null && !request.getVideoUrl().isEmpty()) {
            request.setContentType("video_url");
        } else if (request.getTextContent() != null && !request.getTextContent().isEmpty()) {
            request.setContentType("text");
        }
        
        request.setRequestedBy(counselor.getUserId().longValue());
        request.setRequestedAt(LocalDateTime.now());
        request.setStatus("PENDING");
        
        ModuleEditRequest savedRequest = editRequestRepository.save(request);
        
        redirectAttributes.addFlashAttribute("message", 
            "Module request submitted successfully! You can now add quiz questions.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        redirectAttributes.addFlashAttribute("newRequestId", savedRequest.getRequestId());
        
        return "redirect:/counselor/modules/request/" + savedRequest.getRequestId() + "/quiz";
    }
    
    @GetMapping("/request/{requestId}/quiz")
    public String manageRequestQuiz(@PathVariable Integer requestId, Model model) {
        ModuleEditRequest request = editRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found"));
        
        List<ModuleEditRequestQuiz> quizzes = requestQuizRepository
            .findByRequestIdOrderByQuestionOrderAsc(requestId);
        
        model.addAttribute("request", request);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("newQuiz", new ModuleEditRequestQuiz());
        
        return "self-care/counselor-request-quiz";
    }
    
    @PostMapping("/request/{requestId}/quiz/add")
    public String addRequestQuiz(@PathVariable Integer requestId,
                                @ModelAttribute ModuleEditRequestQuiz quiz,
                                RedirectAttributes redirectAttributes) {
        
        ModuleEditRequest request = editRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found"));
        
        if (!"PENDING".equals(request.getStatus())) {
            redirectAttributes.addFlashAttribute("message", 
                "Cannot add quiz to non-pending request!");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/counselor/modules";
        }
        
        quiz.setRequestId(requestId);
        requestQuizRepository.save(quiz);
        
        redirectAttributes.addFlashAttribute("message", 
            "Quiz question added successfully!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        
        return "redirect:/counselor/modules/request/" + requestId + "/quiz";
    }
    
    @GetMapping("/request/quiz/edit/{quizId}")
    public String showEditRequestQuizForm(@PathVariable Integer quizId, Model model) {
        ModuleEditRequestQuiz quiz = requestQuizRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        ModuleEditRequest request = editRequestRepository.findById(quiz.getRequestId())
            .orElseThrow(() -> new RuntimeException("Request not found"));
        
        model.addAttribute("request", request);
        model.addAttribute("quiz", quiz);
        
        return "self-care/counselor-request-quiz-edit";
    }
    
    @PostMapping("/request/quiz/update/{quizId}")
    public String updateRequestQuiz(@PathVariable Integer quizId,
                                   @ModelAttribute ModuleEditRequestQuiz quizData,
                                   RedirectAttributes redirectAttributes) {
        
        ModuleEditRequestQuiz quiz = requestQuizRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        quiz.setQuestionText(quizData.getQuestionText());
        quiz.setOptionA(quizData.getOptionA());
        quiz.setOptionB(quizData.getOptionB());
        quiz.setOptionC(quizData.getOptionC());
        quiz.setOptionD(quizData.getOptionD());
        quiz.setCorrectAnswer(quizData.getCorrectAnswer());
        quiz.setQuestionOrder(quizData.getQuestionOrder());
        
        requestQuizRepository.save(quiz);
        
        redirectAttributes.addFlashAttribute("message", 
            "Quiz question updated successfully!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        
        return "redirect:/counselor/modules/request/" + quiz.getRequestId() + "/quiz";
    }
    
    @GetMapping("/request/quiz/delete/{quizId}")
    public String deleteRequestQuiz(@PathVariable Integer quizId,
                                   RedirectAttributes redirectAttributes) {
        
        ModuleEditRequestQuiz quiz = requestQuizRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        Integer requestId = quiz.getRequestId();
        requestQuizRepository.deleteById(quizId);
        
        redirectAttributes.addFlashAttribute("message", "Quiz question deleted!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        
        return "redirect:/counselor/modules/request/" + requestId + "/quiz";
    }
    
    @GetMapping("/{moduleId}/quiz")
    public String manageQuiz(@PathVariable Integer moduleId, Model model) {
        SelfCareModule module = moduleRepository.findById(moduleId)
            .orElseThrow(() -> new RuntimeException("Module not found"));
        
        List<ModuleQuiz> quizzes = quizRepository.findByModuleIdOrderByQuestionOrderAsc(moduleId);
        
        model.addAttribute("module", module);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("newQuiz", new ModuleQuiz());
        
        return "self-care/counselor-module-quiz";
    }
    
    @PostMapping("/{moduleId}/quiz/add")
    public String addQuizQuestion(@PathVariable Integer moduleId,
                                 @ModelAttribute ModuleQuiz quiz,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        
        User counselor = userService.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Counselor not found"));
        
        quiz.setModuleId(moduleId);
        quiz.setCreatedBy(counselor.getUserId().longValue());
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setStatus("PENDING");
        
        quizRepository.save(quiz);
        
        redirectAttributes.addFlashAttribute("message", 
            "Quiz question submitted for approval!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        
        return "redirect:/counselor/modules/" + moduleId + "/quiz";
    }
    
    @GetMapping("/quiz/edit/{quizId}")
    public String showEditQuizForm(@PathVariable Integer quizId, Model model) {
        ModuleQuiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        SelfCareModule module = moduleRepository.findById(quiz.getModuleId())
            .orElseThrow(() -> new RuntimeException("Module not found"));
        
        model.addAttribute("module", module);
        model.addAttribute("quiz", quiz);
        
        return "self-care/counselor-quiz-edit";
    }
    
    @PostMapping("/quiz/update/{quizId}")
    public String updateQuizQuestion(@PathVariable Integer quizId,
                                    @ModelAttribute ModuleQuiz quizData,
                                    RedirectAttributes redirectAttributes) {
        
        ModuleQuiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        quiz.setQuestionText(quizData.getQuestionText());
        quiz.setOptionA(quizData.getOptionA());
        quiz.setOptionB(quizData.getOptionB());
        quiz.setOptionC(quizData.getOptionC());
        quiz.setOptionD(quizData.getOptionD());
        quiz.setCorrectAnswer(quizData.getCorrectAnswer());
        quiz.setQuestionOrder(quizData.getQuestionOrder());
        quiz.setStatus("PENDING");
        
        quizRepository.save(quiz);
        
        redirectAttributes.addFlashAttribute("message", 
            "Quiz question updated and submitted for re-approval!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        
        return "redirect:/counselor/modules/" + quiz.getModuleId() + "/quiz";
    }
    
    @GetMapping("/quiz/delete/{quizId}")
    public String deleteQuizQuestion(@PathVariable Integer quizId,
                                    RedirectAttributes redirectAttributes) {
        
        ModuleQuiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        Integer moduleId = quiz.getModuleId();
        quizRepository.deleteById(quizId);
        
        redirectAttributes.addFlashAttribute("message", "Quiz question deleted!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        
        return "redirect:/counselor/modules/" + moduleId + "/quiz";
    }
    
    private String saveFile(MultipartFile file, String subDir) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String uploadPath = UPLOAD_DIR + subDir + "/";
        
        Path path = Paths.get(uploadPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        
        Path filePath = path.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);
        
        return uploadPath + fileName;
    }
}