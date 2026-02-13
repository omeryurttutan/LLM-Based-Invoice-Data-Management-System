package com.faturaocr.domain.company.port;

import com.faturaocr.domain.company.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository {
    Company save(Company company);

    Optional<Company> findById(UUID id);

    Optional<Company> findByTaxNumber(String taxNumber);

    Page<Company> findAllActive(Pageable pageable);

    Page<Company> findAllByIsDeletedFalse(Pageable pageable);

    boolean existsByTaxNumber(String taxNumber);

    void softDelete(UUID id);
}
