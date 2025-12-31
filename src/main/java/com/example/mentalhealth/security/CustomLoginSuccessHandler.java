package com.example.mentalhealth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        var authorities = authentication.getAuthorities();
        String redirectUrl = "/progress/dashboard"; // Default for STUDENT

        for (var authority : authorities) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_COUNSELOR")) {
                redirectUrl = "/counselling/approval";
                break;
            } else if (role.equals("ROLE_ADMIN")) {
                redirectUrl = "/forum";
                break;
            }
        }

        response.sendRedirect(redirectUrl);
    }
}
