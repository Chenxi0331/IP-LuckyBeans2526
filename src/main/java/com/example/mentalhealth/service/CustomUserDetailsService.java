package com.example.mentalhealth.service;

import com.example.mentalhealth.model.User;
import com.example.mentalhealth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("=== Login Attempt ===");
        System.out.println("Email: " + email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        System.out.println("User found: " + user.getFullName());
        System.out.println("User role: " + user.getRole());
        System.out.println("User active: " + (user.isActive() ? "Yes" : "No"));
        System.out.println("Password hash: " + user.getPassword());
        System.out.println("Password length: " + (user.getPassword() != null ? user.getPassword().length() : "null"));

        // Create authorities list with ROLE_ prefix
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
        
        System.out.println("Assigned authorities: " + authorities);
        System.out.println("====================");

        // Return Spring Security User object
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            user.isActive(),        // enabled
            true,                   // accountNonExpired
            true,                   // credentialsNonExpired
            true,                   // accountNonLocked
            authorities
        );
    }
}