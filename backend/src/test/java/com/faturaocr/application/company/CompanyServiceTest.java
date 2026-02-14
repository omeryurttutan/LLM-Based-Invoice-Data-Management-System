package com.faturaocr.application.company;

import com.faturaocr.application.company.dto.CompanyResponse;
import com.faturaocr.application.company.dto.CreateCompanyCommand;
import com.faturaocr.application.company.dto.UpdateCompanyCommand;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.company.port.CompanyRepository;
import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.common.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyService companyService;

    private Company company;
    private CreateCompanyCommand createCommand;

    @BeforeEach
    void setUp() {
        company = new Company("Test Company", "1234567890");
        // ID is generated in constructor

        createCommand = CreateCompanyCommand.builder()
                .name("Test Company")
                .taxNumber("1234567890")
                .email("test@company.com")
                .build();
    }

    @Test
    void createCompany_Success() {
        when(companyRepository.existsByTaxNumber(any())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        CompanyResponse response = companyService.createCompany(createCommand);

        assertNotNull(response);
        assertEquals(company.getName(), response.getName());
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void createCompany_DuplicateTaxNumber_ThrowsException() {
        when(companyRepository.existsByTaxNumber(any())).thenReturn(true);

        assertThrows(DomainException.class, () -> companyService.createCompany(createCommand));
        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void updateCompany_Success() {
        UUID id = company.getId();
        UpdateCompanyCommand updateCommand = UpdateCompanyCommand.builder()
                .name("Updated Name")
                .email("updated@test.com")
                .build();

        when(companyRepository.findById(id)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        CompanyResponse response = companyService.updateCompany(id, updateCommand);

        assertEquals("Updated Name", company.getName());
        assertEquals("updated@test.com", company.getEmail());
    }

    @Test
    void getCompanyById_Success() {
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));

        CompanyResponse response = companyService.getCompanyById(company.getId());

        assertEquals(company.getId(), response.getId());
    }

    @Test
    void deleteCompany_Success() {
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));

        companyService.deleteCompany(company.getId());

        verify(companyRepository).softDelete(company.getId());
    }

    @Test
    void listCompanies_Success() {
        Page<Company> companies = new PageImpl<>(Collections.singletonList(company));
        when(companyRepository.findAllByIsDeletedFalse(any(Pageable.class))).thenReturn(companies);

        Page<CompanyResponse> response = companyService.listCompanies(Pageable.unpaged());

        assertFalse(response.isEmpty());
        assertEquals(1, response.getTotalElements());
    }
}
