package com.faturaocr.domain.category.port;

import com.faturaocr.domain.category.entity.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    Category save(Category category);

    Optional<Category> findById(UUID id);

    Optional<Category> findByIdAndCompanyId(UUID id, UUID companyId);

    List<Category> findAllByCompanyId(UUID companyId);

    List<Category> findAllActiveByCompanyId(UUID companyId);

    boolean existsByNameAndCompanyId(String name, UUID companyId);

    List<Category> findAllById(Iterable<UUID> ids);

    void softDelete(UUID id);
}
