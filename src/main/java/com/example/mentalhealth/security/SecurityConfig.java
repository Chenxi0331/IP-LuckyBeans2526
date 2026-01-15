package com.example.mentalhealth.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AuthenticationSuccessHandler loginSuccessHandler;

    @Autowired
    private CustomLoginFailureHandler loginFailureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Updated AuthenticationManager bean.
     * We use HttpSecurity to build the manager to avoid the 
     * AuthenticationConfiguration "bean not found" error.
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder())
                .and()
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Publicly accessible paths
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/reset-password",
                                "/generate-password",
                                "/h2-console/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/resources/**",
                                "/uploads/**")
                        .permitAll()
                        
                        // Dashboard access
                        .requestMatchers("/dashboard/student").hasAuthority("ROLE_STUDENT")
                        .requestMatchers("/dashboard/counselor").hasAuthority("ROLE_COUNSELOR")
                        .requestMatchers("/dashboard/admin").hasAuthority("ROLE_ADMIN")
                        
                        // Profile and Chatbot
                        .requestMatchers("/profile/**", "/chatbot/**").authenticated()
                        
                        // Counselling logic
                        .requestMatchers("/counselling/book", "/counselling/schedule").hasAuthority("ROLE_STUDENT")
                        .requestMatchers("/counselling/approval", "/counselling/approve/**").hasAuthority("ROLE_COUNSELOR")
                        
                        // Awareness Library & Campaigns
                        .requestMatchers("/awareness/library").authenticated()
                        .requestMatchers("/awareness/resource/view/**",
                                "/awareness/campaign/view/**",
                                "/awareness/campaigns/join/**")
                        .hasAnyAuthority("ROLE_STUDENT", "ROLE_COUNSELOR", "ROLE_ADMIN")
                        .requestMatchers("/awareness/manage", "/awareness/resource/**",
                                "/awareness/campaign/**")
                        .hasAnyAuthority("ROLE_COUNSELOR", "ROLE_ADMIN")
                        
                        // Progress tracking and Symptoms
                        .requestMatchers("/progress/search", "/progress/students/**").hasAuthority("ROLE_COUNSELOR")
                        .requestMatchers("/symptoms", "/symptoms/**").hasAuthority("ROLE_STUDENT")
                        
                        // Assessment Management
                        .requestMatchers("/counselor/assessment", "/counselor/assessment/**").hasAuthority("ROLE_COUNSELOR")
                        .requestMatchers("/admin/assessment/**").hasAuthority("ROLE_ADMIN")
                        
                        .anyRequest().authenticated())
                
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .permitAll())
                // Needed for H2 Console
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}