package com.faturaocr.infrastructure.common.config;

import com.faturaocr.infrastructure.audit.RequestIdFilter;
import com.faturaocr.infrastructure.security.CompanyContextFilter;
import com.faturaocr.infrastructure.security.CustomAccessDeniedHandler;
import com.faturaocr.infrastructure.security.CustomAuthenticationEntryPoint;
import com.faturaocr.infrastructure.security.JwtAuthenticationFilter;
import com.faturaocr.infrastructure.security.RateLimitFilter;
import com.faturaocr.infrastructure.security.RequestSizeFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration with RBAC.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final CompanyContextFilter companyContextFilter;
        private final CustomAccessDeniedHandler accessDeniedHandler;
        private final CustomAuthenticationEntryPoint authenticationEntryPoint;
        private final RequestIdFilter requestIdFilter;
        private final RateLimitFilter rateLimitFilter;
        private final RequestSizeFilter requestSizeFilter;

        @Value("${app.security.cors.allowed-origins}")
        private List<String> allowedOrigins;

        @Value("${app.security.headers.hsts-enabled:false}")
        private boolean hstsEnabled;

        @Value("${app.security.headers.csp-enabled:true}")
        private boolean cspEnabled;

        public SecurityConfig(
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        CompanyContextFilter companyContextFilter,
                        CustomAccessDeniedHandler accessDeniedHandler,
                        CustomAuthenticationEntryPoint authenticationEntryPoint,
                        RequestIdFilter requestIdFilter,
                        RateLimitFilter rateLimitFilter,
                        RequestSizeFilter requestSizeFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.companyContextFilter = companyContextFilter;
                this.accessDeniedHandler = accessDeniedHandler;
                this.authenticationEntryPoint = authenticationEntryPoint;
                this.requestIdFilter = requestIdFilter;
                this.rateLimitFilter = rateLimitFilter;
                this.requestSizeFilter = requestSizeFilter;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // Disable CSRF (using JWT)
                                .csrf(AbstractHttpConfigurer::disable)

                                // Enable CORS
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // Security Headers
                                .headers(headers -> {
                                        headers.frameOptions(frame -> frame.deny());
                                        headers.xssProtection(xss -> xss.disable()); // Modern browsers use CSP
                                        headers.contentTypeOptions(contentType -> {
                                        });
                                        headers.referrerPolicy(referrer -> referrer
                                                        .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                                        headers.permissionsPolicy(permissions -> permissions
                                                        .policy("camera=(), microphone=(), geolocation=(), payment=()"));

                                        headers.cacheControl(cache -> cache.disable()); // Custom cache control
                                        headers.addHeaderWriter(new StaticHeadersWriter("Cache-Control",
                                                        "no-store, no-cache, must-revalidate, max-age=0"));
                                        headers.addHeaderWriter(new StaticHeadersWriter("Pragma", "no-cache"));

                                        if (hstsEnabled) {
                                                headers.httpStrictTransportSecurity(hsts -> hsts
                                                                .includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000));
                                        }

                                        if (cspEnabled) {
                                                headers.contentSecurityPolicy(csp -> csp
                                                                .policyDirectives(
                                                                                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'"));
                                        }
                                })

                                // Stateless session
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Exception handling
                                .exceptionHandling(exceptions -> exceptions
                                                .accessDeniedHandler(accessDeniedHandler)
                                                .authenticationEntryPoint(authenticationEntryPoint))

                                // Authorization rules
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints
                                                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register",
                                                                "/api/v1/auth/refresh")
                                                .permitAll()
                                                .requestMatchers("/api/v1/health").permitAll()
                                                .requestMatchers("/actuator/health").permitAll()
                                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                                                // Admin only endpoints (URL-based)
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/api/v1/system/**").hasRole("ADMIN")

                                                // Manager and above (URL-based)
                                                .requestMatchers("/api/v1/audit-logs/**").hasRole("ADMIN")

                                                // All other endpoints require authentication
                                                .anyRequest().authenticated())

                                // Add filters
                                .addFilterBefore(requestSizeFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(requestIdFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class) // First
                                                                                                              // security
                                                                                                              // filter
                                .addFilterAfter(jwtAuthenticationFilter, RateLimitFilter.class)
                                .addFilterAfter(companyContextFilter, JwtAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration authenticationConfiguration) throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(allowedOrigins);
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(
                                List.of("Authorization", "Content-Type", "Accept", "X-Requested-With",
                                                "X-Internal-API-Key", "X-Company-Id"));
                configuration.setExposedHeaders(List.of("Authorization", "X-RateLimit-Limit", "X-RateLimit-Remaining",
                                "X-RateLimit-Reset", "Content-Disposition"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
