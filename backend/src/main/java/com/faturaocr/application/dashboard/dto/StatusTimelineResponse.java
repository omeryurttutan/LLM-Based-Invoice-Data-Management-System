package com.faturaocr.application.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Daily invoice status changes")
public class StatusTimelineResponse {
    @Schema(description = "Date", example = "2026-02-18")
    private LocalDate date;

    @Schema(description = "Number of invoices created", example = "10")
    private int created;

    @Schema(description = "Number of invoices verified", example = "8")
    private int verified;

    @Schema(description = "Number of invoices rejected", example = "2")
    private int rejected;
}
