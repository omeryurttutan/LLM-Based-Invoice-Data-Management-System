package com.faturaocr.infrastructure.security.test;

import com.faturaocr.domain.user.valueobject.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockAuthenticatedUserSecurityContextFactory.class)
public @interface WithMockAuthenticatedUser {
    String username() default "user";

    String email() default "user@example.com";

    String role() default "ACCOUNTANT";
}
