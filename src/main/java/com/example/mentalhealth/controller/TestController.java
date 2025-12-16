package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.User;
import com.example.mentalhealth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserRepository userRepository;

    // Visit: http://localhost:8080/generate-password?password=admin123
    @GetMapping("/generate-password")
    public String generatePassword(@RequestParam String password) {
        String encoded = passwordEncoder.encode(password);
        return "<h2>Password Generator</h2>" +
               "<p><b>Original password:</b> " + password + "</p>" +
               "<p><b>Encrypted password:</b> " + encoded + "</p>" +
               "<br><p>Copy the encrypted password and use this SQL:</p>" +
               "<code>UPDATE user SET password = '" + encoded + "' WHERE email = 'your@email.com';</code>";
    }
    
    // Visit: http://localhost:8080/reset-password?email=admin@innerly.com&password=admin123
    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam String email, @RequestParam String password) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "User not found: " + email;
        }
        
        String oldHash = user.getPassword();
        String newHash = passwordEncoder.encode(password);
        
        user.setPassword(newHash);
        user.setActive(true);
        userRepository.save(user);
        
        return "<h2>Password Reset Successful!</h2>" +
               "<p><b>Email:</b> " + email + "</p>" +
               "<p><b>New password:</b> " + password + "</p>" +
               "<p><b>Old hash:</b> " + oldHash.substring(0, 20) + "...</p>" +
               "<p><b>New hash:</b> " + newHash.substring(0, 20) + "...</p>" +
               "<br><p><a href='/login'>Go to Login</a></p>";
    }
}