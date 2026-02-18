package com.faturaocr.application.export.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Metadata for available export format")
public class ExportFormatMetadata {
    @Schema(description = "Format identifier code", example = "logo")
    private String format;

    @Schema(description = "Display label", example = "Logo Muhasebe")
    private String label;

    @Schema(description = "Description of the format", example = "Exports invoices in Logo XML format")
    private String description;

    @Schema(description = "Category (Basic vs Accounting)", example = "Accounting")
    private String category;

    @Schema(description = "File extension", example = "xml")
    private String fileExtension;

    @Schema(description = "Icon identifier", example = "file-code")
    private String icon;
}
