package com.github.everolfe.orderservice.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityHelper {

    public Long getCurrentUserId() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth.getPrincipal() instanceof Jwt jwt) {

            Object userId = jwt.getClaims().get("userId");

            if (userId != null) {
                return Long.parseLong(userId.toString());
            }
        }

        throw new AccessDeniedException("User ID not found in token");
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}