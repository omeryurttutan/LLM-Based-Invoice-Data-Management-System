package com.faturaocr.infrastructure.common.config;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Role;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes the first SUPER_ADMIN user from environment variables on startup.
 * If a SUPER_ADMIN already exists, no action is taken.
 * 
 * Required environment variables:
 *   SUPER_ADMIN_EMAIL    (e.g., admin@platform.com)
 *   SUPER_ADMIN_PASSWORD (e.g., MySecureP@ss123!)
 */
@Component
public class SuperAdminInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email:}")
    private String superAdminEmail;

    @Value("${app.super-admin.password:}")
    private String superAdminPassword;

    public SuperAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (superAdminEmail == null || superAdminEmail.isBlank()) {
            LOGGER.info("No SUPER_ADMIN_EMAIL configured. Skipping Super Admin initialization.");
            return;
        }

        if (superAdminPassword == null || superAdminPassword.isBlank()) {
            LOGGER.warn("SUPER_ADMIN_EMAIL is set but SUPER_ADMIN_PASSWORD is empty. Skipping.");
            return;
        }

        // Check if a super admin already exists with this email
        var existingUser = userRepository.findByEmailValue(superAdminEmail);
        if (existingUser.isPresent()) {
            if (existingUser.get().getRole() == Role.SUPER_ADMIN) {
                LOGGER.info("Super Admin already exists: {}. Skipping.", superAdminEmail);
            } else {
                LOGGER.warn("User {} exists but is not SUPER_ADMIN (role: {}). Not modifying.",
                        superAdminEmail, existingUser.get().getRole());
            }
            return;
        }

        // Create SUPER_ADMIN (no companyId — platform-level user)
        User superAdmin = User.builder()
                .email(superAdminEmail)
                .passwordHash(passwordEncoder.encode(superAdminPassword))
                .fullName("Platform Super Admin")
                .role(Role.SUPER_ADMIN)
                .isActive(true)
                .build();

        userRepository.save(superAdmin);
        LOGGER.info("✅ Super Admin created successfully: {}", superAdminEmail);
    }
}
