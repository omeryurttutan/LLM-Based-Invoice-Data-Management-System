package com.faturaocr.application.company;

import com.faturaocr.application.company.dto.CompanyResponse;
import com.faturaocr.application.company.dto.CreateCompanyCommand;
import com.faturaocr.application.company.dto.UpdateCompanyCommand;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.company.port.CompanyRepository;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.domain.common.exception.EntityNotFoundException;
import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.audit.annotation.Auditable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Transactional
    @Auditable(action = AuditActionType.CREATE, entityType = "COMPANY")
    public CompanyResponse createCompany(CreateCompanyCommand command) {
        if (command.getTaxNumber() != null && companyRepository.existsByTaxNumber(command.getTaxNumber())) {
            throw new DomainException("Company with tax number " + command.getTaxNumber() + " already exists");
        }

        Company company = new Company(command.getName(), command.getTaxNumber());
        company.setTaxOffice(command.getTaxOffice());
        company.setAddress(command.getAddress());
        company.setCity(command.getCity());
        company.setDistrict(command.getDistrict());
        company.setPostalCode(command.getPostalCode());
        company.setPhone(command.getPhone());
        company.setEmail(command.getEmail());
        company.setWebsite(command.getWebsite());
        if (command.getDefaultCurrency() != null) {
            company.setDefaultCurrency(command.getDefaultCurrency());
        }
        company.setInvoicePrefix(command.getInvoicePrefix());

        Company savedCompany = companyRepository.save(company);
        return CompanyResponse.fromDomain(savedCompany);
    }

    @Transactional
    @Auditable(action = AuditActionType.UPDATE, entityType = "COMPANY")
    public CompanyResponse updateCompany(UUID id, UpdateCompanyCommand command) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company", id));

        company.updateInfo(
                command.getName(),
                command.getTaxOffice(),
                command.getAddress(),
                command.getCity(),
                command.getDistrict(),
                command.getPostalCode(),
                command.getPhone(),
                command.getEmail(),
                command.getWebsite(),
                command.getDefaultCurrency(),
                command.getInvoicePrefix());

        Company savedCompany = companyRepository.save(company);
        return CompanyResponse.fromDomain(savedCompany);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company", id));
        return CompanyResponse.fromDomain(company);
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> listCompanies(Pageable pageable) {
        return companyRepository.findAllByIsDeletedFalse(pageable)
                .map(CompanyResponse::fromDomain);
    }

    @Transactional
    @Auditable(action = AuditActionType.DELETE, entityType = "COMPANY")
    public void deleteCompany(UUID id) {
        if (!companyRepository.findById(id).isPresent()) {
            throw new EntityNotFoundException("Company", id);
        }

        // Check for ANY users before deleting
        long count = 0;
        for (Role role : Role.values()) {
            count += userRepository.countByCompanyIdAndRole(id, role);
        }

        if (count > 0) {
            throw new DomainException(
                    "Cannot delete company with " + count + " users. Please deactivate or delete users first.");
        }

        companyRepository.softDelete(id);
    }

    @Transactional
    public CompanyResponse activateCompany(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company", id));

        company.activate();
        Company savedCompany = companyRepository.save(company);
        return CompanyResponse.fromDomain(savedCompany);
    }

    @Transactional
    public CompanyResponse deactivateCompany(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company", id));

        company.deactivate();
        Company savedCompany = companyRepository.save(company);
        return CompanyResponse.fromDomain(savedCompany);
    }
}
