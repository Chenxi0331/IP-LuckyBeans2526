package com.example.mentalhealth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import com.example.mentalhealth.service.CounsellingService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private CounsellingService counsellingService;

    @GetMapping("/dashboard") // Kept original path as per @RequestMapping, but snippet showed /admin/dashboard. Sticking to original logic.
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/sessions") // Adjusted to be relative to @RequestMapping("/admin")
    public String viewAllSessions(Model model) {
        model.addAttribute("sessions", counsellingService.findAllSessions());
        return "admin/sessions";
    }

    @GetMapping("/users")
    public String users() {
        return "admin/users";
    }
    
    @GetMapping("/reports")
    public String reports() {
        return "admin/reports";
    }

    @GetMapping("/settings")
    public String settings() {
        return "admin/settings";
    }
}
