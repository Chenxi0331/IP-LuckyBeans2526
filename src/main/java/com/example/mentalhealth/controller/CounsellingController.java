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

    @GetMapping("/schedule")
    public String schedulePage(Model model) {
        // provide today's date string for any template min attributes
        model.addAttribute("todayDate", java.time.LocalDate.now().toString());
        return "counselling/schedule";
    }

    @PostMapping("/schedule")
    public String scheduleSubmit(@RequestParam String date,
                                 @RequestParam String time,
                                 @RequestParam(required = false) Long counsellorId,
                                Authentication authentication) {
        // date expected in yyyy-MM-dd and time in HH:mm (from date + time inputs)
        java.time.LocalDate d = java.time.LocalDate.parse(date);
        java.time.LocalTime t = java.time.LocalTime.parse(time);
        java.time.LocalDateTime dt = java.time.LocalDateTime.of(d, t);

        // String currentPrincipalName = authentication.getName();
        // User currentUser = userService.findByEmail(currentPrincipalName);   
        // CounsellingSession session = new CounsellingSession();
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow(); 
        CounsellingSession session = new CounsellingSession();
        session.setStudentId(currentUser.getId());
        session.setSessionDate(dt);
        session.setStatus("PENDING");
        if (counsellorId != null) {
            session.setCounsellorId(counsellorId);
        } else {
            session.setCounsellorId(1L); // fallback temporary ID
        }
        service.save(session);
        return "redirect:/counselling/my-sessions";
    }

    @PreAuthorize("hasAnyRole('COUNSELOR', 'ADMIN')")
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
        model.addAttribute("session", new CounsellingSession());
        return "counselling/book";
    }

    @PostMapping("/book")
    public String submitBooking(@ModelAttribute CounsellingSession session, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        session.setStudentId(user.getId());
        service.createSession(session);
        return "redirect:/counselling/my-sessions";
    }

    @GetMapping("/my-sessions")
    public String mySessions(Model model, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        model.addAttribute("sessions",
                user.getRole() == Role.COUNSELOR 
                    ? service.getSessionsForCounsellor(user.getId())
                    : service.getSessionsForStudent(user.getId()));
        return "counselling/my-sessions";
    }
}
