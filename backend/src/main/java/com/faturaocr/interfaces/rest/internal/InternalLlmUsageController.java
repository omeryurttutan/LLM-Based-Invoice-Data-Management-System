package com.faturaocr.interfaces.rest.internal;

import com.faturaocr.domain.monitoring.entity.LlmApiUsage;
import com.faturaocr.infrastructure.monitoring.LlmCostMonitoringService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/internal/llm-usage")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal LLM Usage", description = "Internal endpoints for recording LLM usage")
public class InternalLlmUsageController {

    private final LlmCostMonitoringService costMonitoringService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record LLM usage", description = "Internal endpoint to record LLM token usage and cost")
    @ApiResponse(responseCode = "201", description = "Usage recorded successfully")
    public void recordUsage(@RequestBody LlmUsageRequest request) {
        // TODO: Validate internal API key if needed, or rely on network security/IP
        // whitelist

        log.debug("Received LLM usage report: {}", request);

        LlmApiUsage usage = LlmApiUsage.builder()
                .companyId(UUID.fromString("00000000-0000-0000-0000-000000000000")) // Default/System company for now if
                                                                                    // not provided
                .provider(request.getProvider())
                .model(request.getModel())
                .requestType(request.getRequestType())
                .inputTokens(request.getInputTokens())
                .outputTokens(request.getOutputTokens())
                .estimatedCostUsd(request.getEstimatedCostUsd())
                .success(request.isSuccess())
                .durationMs(request.getDurationMs())
                .invoiceId(request.getInvoiceId())
                .correlationId(request.getCorrelationId())
                .build();

        if (request.getCompanyId() != null) {
            usage.setCompanyId(request.getCompanyId());
        }

        costMonitoringService.recordUsage(usage);
    }

    @Data
    @Schema(description = "LLM usage report request")
    public static class LlmUsageRequest {
        @Schema(description = "Company ID")
        private UUID companyId;
        @Schema(description = "LLM Provider", example = "OPENAI")
        private String provider;
        @Schema(description = "Model name", example = "gpt-3.5-turbo")
        private String model;
        @Schema(description = "Request type", example = "EXTRACTION")
        private String requestType;
        @Schema(description = "Input tokens used", example = "100")
        private Integer inputTokens;
        @Schema(description = "Output tokens used", example = "50")
        private Integer outputTokens;
        @Schema(description = "Estimated cost in USD", example = "0.002")
        private BigDecimal estimatedCostUsd;
        @Schema(description = "Was request successful", example = "true")
        private boolean success;
        @Schema(description = "Duration in milliseconds", example = "1500")
        private Integer durationMs;
        @Schema(description = "Invoice ID")
        private UUID invoiceId;
        @Schema(description = "Correlation ID")
        private String correlationId;
    }
}
