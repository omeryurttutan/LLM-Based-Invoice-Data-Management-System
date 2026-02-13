package com.faturaocr.interfaces.rest.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateCategoryRequest {
    @NotBlank
    private String name;

    private String description;
    private String color;
    private String icon;
    private UUID parentId;
}
