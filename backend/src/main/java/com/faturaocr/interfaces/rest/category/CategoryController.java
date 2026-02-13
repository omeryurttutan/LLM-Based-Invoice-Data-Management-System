package com.faturaocr.interfaces.rest.category;

import com.faturaocr.application.category.CategoryService;
import com.faturaocr.application.category.dto.CategoryResponse;
import com.faturaocr.application.category.dto.CreateCategoryCommand;
import com.faturaocr.application.category.dto.UpdateCategoryCommand;
import com.faturaocr.interfaces.rest.category.dto.CreateCategoryRequest;
import com.faturaocr.interfaces.rest.category.dto.UpdateCategoryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CreateCategoryCommand command = new CreateCategoryCommand();
        command.setName(request.getName());
        command.setDescription(request.getDescription());
        command.setColor(request.getColor());
        command.setIcon(request.getIcon());
        command.setParentId(request.getParentId());
        return ResponseEntity.ok(categoryService.createCategory(command));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        UpdateCategoryCommand command = new UpdateCategoryCommand();
        command.setName(request.getName());
        command.setDescription(request.getDescription());
        command.setColor(request.getColor());
        command.setIcon(request.getIcon());
        command.setParentId(request.getParentId());
        command.setIsActive(request.getIsActive());
        return ResponseEntity.ok(categoryService.updateCategory(id, command));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    public ResponseEntity<List<CategoryResponse>> listCategories(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(categoryService.listCategories(includeInactive));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
