package com.faturaocr.application.category;

import com.faturaocr.application.category.dto.CategoryResponse;
import com.faturaocr.application.category.dto.CreateCategoryCommand;
import com.faturaocr.application.category.dto.UpdateCategoryCommand;
import com.faturaocr.application.common.service.ApplicationService;
// Exception imports will be fixed after finding them
import com.faturaocr.application.common.exception.BusinessException;
import com.faturaocr.application.common.exception.ResourceNotFoundException;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.category.entity.Category;
import com.faturaocr.domain.category.port.CategoryRepository;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.audit.annotation.Auditable;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final InvoiceRepository invoiceRepository; // To check usage before delete

    @Auditable(action = AuditActionType.CREATE, entityType = "CATEGORY")
    public CategoryResponse createCategory(CreateCategoryCommand command) {
        UUID companyId = CompanyContextHolder.getCompanyId();

        if (categoryRepository.existsByNameAndCompanyId(command.getName(), companyId)) {
            // throw new BusinessException("CATEGORY_NAME_EXISTS", "Category name already
            // exists");
            throw new BusinessException("CATEGORY_NAME_EXISTS", "Category name already exists"); // Placeholder
        }

        Category category = new Category();
        category.setCompanyId(companyId);
        category.setName(command.getName());
        category.setDescription(command.getDescription());
        category.setColor(command.getColor());
        category.setIcon(command.getIcon());
        category.setParentId(command.getParentId());

        // Validate parent if set
        if (command.getParentId() != null) {
            if (!categoryRepository.findByIdAndCompanyId(command.getParentId(), companyId).isPresent()) {
                // throw new ResourceNotFoundException("Parent category not found");
                throw new ResourceNotFoundException("Parent category not found"); // Placeholder
            }
        }

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Auditable(action = AuditActionType.UPDATE, entityType = "CATEGORY")
    public CategoryResponse updateCategory(UUID id, UpdateCategoryCommand command) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        Category category = getCategoryOrThrow(id);

        if (!category.getName().equals(command.getName()) &&
                categoryRepository.existsByNameAndCompanyId(command.getName(), companyId)) {
            // throw new BusinessException("CATEGORY_NAME_EXISTS", "Category name already
            // exists");
            throw new BusinessException("CATEGORY_NAME_EXISTS", "Category name already exists");
        }

        category.setName(command.getName());
        category.setDescription(command.getDescription());
        category.setColor(command.getColor());
        category.setIcon(command.getIcon());
        category.setParentId(command.getParentId());

        if (command.getIsActive() != null) {
            category.setActive(command.getIsActive());
        }

        if (command.getParentId() != null) {
            if (command.getParentId().equals(category.getId())) {
                // throw new BusinessException("CATEGORY_PARENT_loop", "Category cannot be its
                // own parent");
                throw new BusinessException("CATEGORY_PARENT_LOOP", "Category cannot be its own parent");
            }
            if (!categoryRepository.findByIdAndCompanyId(command.getParentId(), companyId).isPresent()) {
                // throw new ResourceNotFoundException("Parent category not found");
                throw new ResourceNotFoundException("Parent category not found");
            }
        }

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    public CategoryResponse getCategoryById(UUID id) {
        return mapToResponse(getCategoryOrThrow(id));
    }

    public List<CategoryResponse> listCategories(boolean includeInactive) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        List<Category> categories;
        if (includeInactive) {
            categories = categoryRepository.findAllByCompanyId(companyId);
        } else {
            categories = categoryRepository.findAllActiveByCompanyId(companyId);
        }
        return categories.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Auditable(action = AuditActionType.DELETE, entityType = "CATEGORY")
    public void deleteCategory(UUID id) {
        getCategoryOrThrow(id); // Verify exists and belongs to company
        long invoiceCount = invoiceRepository.countByCategoryId(id);
        if (invoiceCount > 0) {
            throw new BusinessException("CATEGORY_HAS_INVOICES",
                    "Cannot delete category with " + invoiceCount + " assigned invoice(s)");
        }
        categoryRepository.softDelete(id);
    }

    private Category getCategoryOrThrow(UUID id) {
        UUID companyId = CompanyContextHolder.getCompanyId();
        return categoryRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private CategoryResponse mapToResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setColor(category.getColor());
        response.setIcon(category.getIcon());
        response.setParentId(category.getParentId());
        response.setActive(category.isActive());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());

        if (category.getParentId() != null) {
            categoryRepository.findById(category.getParentId()).ifPresent(p -> response.setParentName(p.getName()));
        }
        return response;
    }
}
