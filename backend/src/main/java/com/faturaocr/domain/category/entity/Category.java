package com.faturaocr.domain.category.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Category {
    private UUID id;
    private UUID companyId;
    private String name;
    private String description;
    private String color;
    private String icon;
    private UUID parentId;
    private boolean isActive;
    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Category() {
        this.id = UUID.randomUUID();
        this.isActive = true;
        this.isDeleted = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
