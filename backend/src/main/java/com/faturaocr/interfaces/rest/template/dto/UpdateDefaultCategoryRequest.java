package com.faturaocr.interfaces.rest.template.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class UpdateDefaultCategoryRequest {
    @NotNull
    private UUID categoryId;
}
