package com.example.mentalhealth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.mentalhealth.model.User;
import com.example.mentalhealth.model.Role;
import com.example.mentalhealth.repository.UserRepository;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner loadData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("testuser@test.com").isEmpty()) {
                User user = new User();
                user.setFullName("Test User");
                user.setEmail("testuser@test.com");
                user.setPassword(passwordEncoder.encode("testpass"));
                user.setRole(Role.STUDENT);
                userRepository.save(user);
            }

            // Demo accounts
            createUser(userRepository, passwordEncoder, "John Smith", "student@innerly.com", "password", Role.STUDENT);
            createUser(userRepository, passwordEncoder, "Dr. Sarah Johnson", "counselor@innerly.com", "password", Role.COUNSELOR);
            createUser(userRepository, passwordEncoder, "Admin User", "admin@innerly.com", "password", Role.ADMIN);

            System.out.println("Demo users created successfully!");
            System.out.println("Login with: student@innerly.com / password");
            System.out.println("           counselor@innerly.com / password");
            System.out.println("           admin@innerly.com / password");

            System.out.println("--- CURRENT USERS IN DB ---");
            userRepository.findAll().forEach(u -> {
                System.out.println("User: " + u.getEmail() + " | Role: " + u.getRole() + " | Active: " + u.isActive());
            });
            System.out.println("---------------------------");
        };
    }

    private void createUser(UserRepository repo, PasswordEncoder encoder, 
                            String fullName, String email, String password, Role role) {
        User user = repo.findByEmail(email).orElse(new User());
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setRole(role);
        repo.save(user);
    }
}