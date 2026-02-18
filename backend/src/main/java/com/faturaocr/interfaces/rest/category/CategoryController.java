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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category management endpoints")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a new category", description = "Creates a new manual category. Requires ADMIN or MANAGER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
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
    @Operation(summary = "Update a category", description = "Updates an existing category. Requires ADMIN or MANAGER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
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
    @Operation(summary = "List categories", description = "Lists all categories for the current company.")
    @ApiResponse(responseCode = "200", description = "List of categories retrieved")
    public ResponseEntity<List<CategoryResponse>> listCategories(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        // We need CompanyContextHolder here because Service expects it for caching key
        // Service also calls it internally, but we need it for the key.
        // Actually Service calls it for key generation via SpEL #companyId
        // So we must pass it.
        UUID companyId = com.faturaocr.infrastructure.security.CompanyContextHolder.getCompanyId();
        return ResponseEntity.ok(categoryService.listCategories(companyId, includeInactive));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'ACCOUNTANT', 'INTERN')")
    @Operation(summary = "Get category details", description = "Retrieves details of a specific category.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a category", description = "Soft deletes a category. Requires ADMIN or MANAGER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
