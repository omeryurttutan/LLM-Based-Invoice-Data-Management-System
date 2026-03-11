package com.faturaocr.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to set company context after authentication.
 */
@Component
@Order(2) // After JwtAuthenticationFilter
public class CompanyContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null &&
                    authentication.getPrincipal() instanceof AuthenticatedUser user) {

                String headerCompanyId = request.getHeader("X-Company-Id");
                if (org.springframework.util.StringUtils.hasText(headerCompanyId)) {
                    if (user.accessibleCompanyIds().contains(headerCompanyId) ||
                            (user.companyId() != null && headerCompanyId.equals(user.companyId().toString())) ||
                            user.isAdmin()) {
                        CompanyContextHolder.setCompanyId(java.util.UUID.fromString(headerCompanyId));
                    } else {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                "Access to the requested company context is denied");
                        return;
                    }
                } else {
                    CompanyContextHolder.setCompanyId(user.companyId());
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            CompanyContextHolder.clear();
        }
    }
}
