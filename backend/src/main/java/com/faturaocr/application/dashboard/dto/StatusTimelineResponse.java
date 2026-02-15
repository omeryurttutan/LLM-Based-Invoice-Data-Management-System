package com.faturaocr.application.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class StatusTimelineResponse {
    private LocalDate date;
    private int created;
    private int verified;
    private int rejected;
}
