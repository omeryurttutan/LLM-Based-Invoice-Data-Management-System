package com.faturaocr.infrastructure.persistence.category;

import com.faturaocr.domain.category.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toDomain(CategoryJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Category category = new Category();
        category.setId(entity.getId());
        category.setCompanyId(entity.getCompanyId());
        category.setName(entity.getName());
        category.setDescription(entity.getDescription());
        category.setColor(entity.getColor());
        category.setIcon(entity.getIcon());
        category.setParentId(entity.getParentId());
        category.setActive(entity.isActive());
        category.setDeleted(entity.isDeleted());
        category.setDeletedAt(entity.getDeletedAt());
        category.setCreatedAt(entity.getCreatedAt());
        category.setUpdatedAt(entity.getUpdatedAt());
        return category;
    }

    public CategoryJpaEntity toJpa(Category domain) {
        if (domain == null) {
            return null;
        }

        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.setId(domain.getId());
        entity.setCompanyId(domain.getCompanyId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setColor(domain.getColor());
        entity.setIcon(domain.getIcon());
        entity.setParentId(domain.getParentId());
        entity.setActive(domain.isActive());
        entity.setDeleted(domain.isDeleted());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
