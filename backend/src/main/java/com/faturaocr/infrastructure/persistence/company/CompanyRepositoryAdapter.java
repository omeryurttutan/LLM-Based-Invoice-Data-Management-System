package com.faturaocr.infrastructure.persistence.company;

import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.company.port.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompanyRepositoryAdapter implements CompanyRepository {

    private final CompanyJpaRepository jpaRepository;
    private final CompanyMapper mapper;

    @Override
    public Company save(Company company) {
        CompanyJpaEntity entity = mapper.toJpaEntity(company);
        @SuppressWarnings("null")
        CompanyJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Company> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Company> findByTaxNumber(String taxNumber) {
        return jpaRepository.findByTaxNumber(taxNumber)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Company> findAllActive(Pageable pageable) {
        return jpaRepository.findAllByIsActiveTrueAndIsDeletedFalse(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Company> findAllByIsDeletedFalse(Pageable pageable) {
        return jpaRepository.findAllByIsDeletedFalse(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByTaxNumber(String taxNumber) {
        return jpaRepository.existsByTaxNumber(taxNumber);
    }

    @Override
    public void softDelete(UUID id) {
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setDeleted(true);
            entity.setDeletedAt(LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }
}
