package com.faturaocr.domain.monitoring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "llm_api_usage", indexes = {
        @Index(name = "idx_llm_usage_company_date", columnList = "company_id, created_at"),
        @Index(name = "idx_llm_usage_provider", columnList = "provider, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmApiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 20)
    private String provider; // GEMINI, GPT, CLAUDE

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "request_type", nullable = false, length = 30)
    private String requestType; // EXTRACTION, RE_EXTRACTION, VALIDATION

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "estimated_cost_usd", precision = 10, scale = 6)
    private BigDecimal estimatedCostUsd;

    @Column(nullable = false)
    @Builder.Default
    private boolean success = true;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
