package com.faturaocr.application.category.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CategoryResponse {
    private UUID id;
    private String name;
    private String description;
    private String color;
    private String icon;
    private UUID parentId;
    private String parentName;
    private boolean isActive;
    // invoiceCount can be added later if needed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
