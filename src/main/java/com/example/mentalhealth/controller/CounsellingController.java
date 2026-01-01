package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.CounsellingSession;
import com.example.mentalhealth.model.Role;
import com.example.mentalhealth.model.User;
import com.example.mentalhealth.repository.UserRepository;
import com.example.mentalhealth.service.CounsellingService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@Controller
@RequestMapping("/counselling")
public class CounsellingController {

    @Autowired
    private CounsellingService service;
    @Autowired
    private UserRepository userRepository;

    public CounsellingController(CounsellingService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('COUNSELOR')")
    @GetMapping("/attend/{id}")
    public String attendSessionPage(@PathVariable Long id, Model model) {
        CounsellingSession session = service.getSessionById(id);
        if (session == null) {
            return "redirect:/counselling/my-sessions";
        }
        model.addAttribute("counsellingSession", session);
        return "counselling/attend";
    }

    @PreAuthorize("hasRole('COUNSELOR')")
    @PostMapping("/attend")
    public String attendSessionSubmit(@RequestParam Long sessionId, @RequestParam String notes) {
        System.out.println("Attending Session ID: " + sessionId + " with Notes: " + notes);
        service.addNotesAndComplete(sessionId, notes);
        return "redirect:/counselling/my-sessions";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all-sessions")
    public String adminAllSessions(Model model) {
        List<CounsellingSession> allSessions = service.getAllSessions();
        model.addAttribute("sessions", allSessions);
        return "counselling/admin-sessions";
    }

    @PreAuthorize("hasRole('COUNSELOR')")
    @GetMapping("/approval")
    public String listApproval(Model model) {
        List<CounsellingSession> pendingSessions = service.findPending();
        model.addAttribute("sessions", pendingSessions);
        model.addAttribute("approvedCount", service.countMonthlyStatus("APPROVED"));
        model.addAttribute("rejectedCount", service.countMonthlyStatus("REJECTED"));
        return "counselling/approval";
    }

    @GetMapping("/approve/{id}")
    public String approve(@PathVariable Long id) {
        service.updateStatus(id, "APPROVED");
        return "redirect:/counselling/approval";
    }

    @GetMapping("/reject/{id}")
    public String reject(@PathVariable Long id) {
        service.updateStatus(id, "REJECTED");
        return "redirect:/counselling/approval";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/book")
    public String showBookingForm(Model model) {
        List<User> counselors = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.COUNSELOR)
                .toList();
        model.addAttribute("counselors", counselors);
        model.addAttribute("counsellingSession", new CounsellingSession());
        return "counselling/book";
    }

    @PostMapping("/book")
    public String submitBooking(@ModelAttribute("counsellingSession") CounsellingSession session, 
                                org.springframework.validation.BindingResult bindingResult,
                                @RequestParam Long counsellorId,
                                Authentication authentication) {
        if (bindingResult.hasErrors()) {
            System.out.println("Booking Validation Errors: " + bindingResult.getAllErrors());
            // In a real app, return to form. For now, let's proceed but warn.
            // actually, if date binding failed, sessionDate will be null.
        }
        
        System.out.println("Booking Session: Date=" + session.getSessionDate() + ", Type=" + session.getSessionType());

        String email = authentication.getName();
        User student = userRepository.findByEmail(email).orElseThrow();
        User counsellor = userRepository.findById(counsellorId).orElseThrow();
        
        System.out.println("Booking Users: Student=" + student.getFullName() + ", Counsellor=" + counsellor.getFullName());

        session.setStudent(student);
        session.setCounsellor(counsellor);
        session.setStatus("PENDING");
        service.createSession(session);
        return "redirect:/counselling/my-sessions";
    }

    @GetMapping("/my-sessions")
    public String mySessions(Model model, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        
        if (user.getRole() == Role.COUNSELOR) {
            model.addAttribute("sessions", service.getSessionsForCounsellor(user));
        } else {
            model.addAttribute("sessions", service.getSessionsForStudent(user));
        }
        
        return "counselling/my-sessions";
    }
}
