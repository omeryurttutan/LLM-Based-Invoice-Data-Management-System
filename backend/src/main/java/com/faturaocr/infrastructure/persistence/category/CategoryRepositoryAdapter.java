package com.faturaocr.infrastructure.persistence.category;

import com.faturaocr.domain.category.entity.Category;
import com.faturaocr.domain.category.port.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public Category save(Category category) {
        CategoryJpaEntity entity = categoryMapper.toJpa(category);
        CategoryJpaEntity savedEntity = categoryJpaRepository.save(entity);
        return categoryMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return categoryJpaRepository.findById(id)
                .map(categoryMapper::toDomain);
    }

    @Override
    public Optional<Category> findByIdAndCompanyId(UUID id, UUID companyId) {
        return categoryJpaRepository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)
                .map(categoryMapper::toDomain);
    }

    @Override
    public List<Category> findAllByCompanyId(UUID companyId) {
        return categoryJpaRepository.findAllByCompanyIdAndIsDeletedFalse(companyId).stream()
                .map(categoryMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Category> findAllActiveByCompanyId(UUID companyId) {
        return categoryJpaRepository.findAllByCompanyIdAndIsActiveTrueAndIsDeletedFalse(companyId).stream()
                .map(categoryMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByNameAndCompanyId(String name, UUID companyId) {
        return categoryJpaRepository.existsByNameAndCompanyIdAndIsDeletedFalse(name, companyId);
    }

    @Override
    public void softDelete(UUID id) {
        categoryJpaRepository.findById(id).ifPresent(entity -> {
            entity.setDeleted(true);
            entity.setDeletedAt(java.time.LocalDateTime.now());
            categoryJpaRepository.save(entity);
        });
    }
}
