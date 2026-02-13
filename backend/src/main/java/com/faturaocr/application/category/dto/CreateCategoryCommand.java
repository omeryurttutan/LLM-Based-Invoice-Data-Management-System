package com.faturaocr.application.category.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CreateCategoryCommand {
    private String name;
    private String description;
    private String color;
    private String icon;
    private UUID parentId;
}
