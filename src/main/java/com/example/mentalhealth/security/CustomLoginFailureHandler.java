package com.example.mentalhealth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        System.out.println("=== LOGIN FAILURE ===");
        System.out.println("User: " + request.getParameter("email"));
        System.out.println("Error Type: " + exception.getClass().getName());
        System.out.println("Error Message: " + exception.getMessage());
        if (exception.getCause() != null) {
            System.out.println("Cause: " + exception.getCause().getMessage());
        }
        exception.printStackTrace();
        System.out.println("=====================");

        response.sendRedirect("/login?error=true");
    }
}
