package com.faturaocr.domain.invoice.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ExtractionRequestMessage {
    @JsonProperty("message_id")
    private UUID messageId;

    @JsonProperty("invoice_id")
    private UUID invoiceId;

    @JsonProperty("company_id")
    private UUID companyId;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_type")
    private String fileType;

    @JsonProperty("file_size")
    private Long fileSize;

    @Builder.Default
    private String priority = "NORMAL";

    @Builder.Default
    private Integer attempt = 1;

    @Builder.Default
    @JsonProperty("max_attempts")
    private Integer maxAttempts = 3;

    private LocalDateTime timestamp;

    @JsonProperty("correlation_id")
    private String correlationId;
}
