package com.faturaocr.infrastructure.security.test;

import com.faturaocr.infrastructure.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Collections;
import java.util.UUID;

public class WithMockAuthenticatedUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockAuthenticatedUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockAuthenticatedUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        AuthenticatedUser principal = new AuthenticatedUser(
                UUID.randomUUID(),
                annotation.email(),
                UUID.randomUUID(),
                annotation.role());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + annotation.role())));

        context.setAuthentication(auth);
        return context;
    }
}
