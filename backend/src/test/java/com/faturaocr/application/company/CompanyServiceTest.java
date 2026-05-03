package com.faturaocr.application.company;

import com.faturaocr.domain.common.exception.EntityNotFoundException;
import com.faturaocr.application.company.dto.CompanyResponse;
import com.faturaocr.application.company.dto.CreateCompanyCommand;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.company.port.CompanyRepository;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.testutil.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CompanyService companyService;

    @Test
    @DisplayName("Should create company successfully")
    void shouldCreateCompanySuccessfully() {
        // Given
        CreateCompanyCommand command = CreateCompanyCommand.builder()
                .name("New Co")
                .taxNumber("1112223334")
                .taxOffice("TaxOffice")
                .address("Addr")
                .city("City")
                .district("District")
                .postalCode("12345")
                .phone("Phone")
                .email("email")
                .website("web")
                .defaultCurrency("TRY")
                .invoicePrefix("INV")
                .build();

        when(companyRepository.save(any(Company.class))).thenAnswer(i -> {
            Company c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        // When
        CompanyResponse response = companyService.createCompany(command);

        // Then
        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("New Co");
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    @DisplayName("Should get company by id")
    void shouldGetCompanyById() {
        // Given
        UUID id = UUID.randomUUID();
        Company company = TestDataBuilder.aCompany().withId(id).build();
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));

        // When
        CompanyResponse response = companyService.getCompanyById(id);

        // Then
        assertThat(response.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Should throw when company not found")
    void shouldThrowWhenCompanyNotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.empty());

        // When
        Throwable thrown = catchThrowable(() -> companyService.getCompanyById(id));

        // Then
        assertThat(thrown).isInstanceOf(EntityNotFoundException.class);
    }
}
