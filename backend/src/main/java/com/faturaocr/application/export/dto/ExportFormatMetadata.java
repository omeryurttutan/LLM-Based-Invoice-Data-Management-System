package com.faturaocr.application.export.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportFormatMetadata {
    private String format;
    private String label;
    private String description;
    private String category;
    private String fileExtension;
    private String icon;
}
