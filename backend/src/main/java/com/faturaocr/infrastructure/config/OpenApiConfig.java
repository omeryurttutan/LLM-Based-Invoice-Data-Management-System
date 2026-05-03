package com.faturaocr.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${faturaocr.openapi.dev-url}")
    private String devUrl;

    @Value("${faturaocr.openapi.staging-url}")
    private String stagingUrl;

    @Value("${faturaocr.openapi.prod-url}")
    private String prodUrl;

    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Development environment");

        Server stagingServer = new Server();
        stagingServer.setUrl(stagingUrl);
        stagingServer.setDescription("Staging environment");

        Server prodServer = new Server();
        prodServer.setUrl(prodUrl);
        prodServer.setDescription("Production environment");

        Contact contact = new Contact();
        contact.setEmail("muhammedfurkanakdag@gmail.com"); // Placeholder email from context
        contact.setName("Muhammed Furkan Akdağ & Ömer Talha Yurttutan");

        License mitLicense = new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("Fatura OCR ve Veri Yönetim Sistemi API")
                .version("1.0.0")
                .contact(contact)
                .description(
                        "A comprehensive API for Invoice OCR Processing, Data Management, and LLM-based extraction.")
                .license(mitLicense);

        SecurityScheme securityScheme = new SecurityScheme()
                .name("Bearer Authentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(
                        "Enter your JWT access token obtained from POST /api/v1/auth/login. Format: Bearer {token}");

        SecurityRequirement securityRequirement = new SecurityRequirement().addList("Bearer Authentication");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, stagingServer, prodServer))
                .addSecurityItem(securityRequirement)
                .components(new Components().addSecuritySchemes("Bearer Authentication", securityScheme));
    }
}
