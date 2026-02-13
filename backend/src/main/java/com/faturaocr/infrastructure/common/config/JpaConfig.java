package com.faturaocr.infrastructure.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA configuration for the application.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.faturaocr.infrastructure.persistence")
@EnableJpaAuditing
@EnableTransactionManagement
public class JpaConfig {
}
