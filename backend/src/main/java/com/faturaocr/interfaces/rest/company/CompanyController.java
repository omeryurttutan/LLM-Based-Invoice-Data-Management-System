package com.faturaocr.interfaces.rest.company;

import com.faturaocr.application.company.CompanyService;
import com.faturaocr.application.company.dto.CompanyResponse;
import com.faturaocr.interfaces.rest.common.ApiResponse;
import com.faturaocr.interfaces.rest.company.dto.CreateCompanyRequest;
import com.faturaocr.interfaces.rest.company.dto.UpdateCompanyRequest;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final com.faturaocr.application.user.UserManagementService userManagementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyResponse> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        CompanyResponse response = companyService.createCompany(request.toCommand());
        return ApiResponse.success("Company created successfully", response);
    }

    @GetMapping("/me")
    public ApiResponse<CompanyResponse> getMyCompany() {
        UUID companyId = CompanyContextHolder.getCompanyId();
        if (companyId == null) {
            return ApiResponse.success(null);
        }
        try {
            CompanyResponse response = companyService.getCompanyById(companyId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.success(null);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CompanyResponse> getCompanyById(@PathVariable UUID id) {
        CompanyResponse response = companyService.getCompanyById(id);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ApiResponse<CompanyResponse> updateCompany(@PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyRequest request) {
        CompanyResponse response = companyService.updateCompany(id, request.toCommand());
        return ApiResponse.success("Company updated successfully", response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompany(@PathVariable UUID id) {
        companyService.deleteCompany(id);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CompanyResponse> activateCompany(@PathVariable UUID id) {
        CompanyResponse response = companyService.activateCompany(id);
        return ApiResponse.success("Company activated successfully", response);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CompanyResponse> deactivateCompany(@PathVariable UUID id) {
        CompanyResponse response = companyService.deactivateCompany(id);
        return ApiResponse.success("Company deactivated successfully", response);
    }

    @GetMapping("/{id}/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'MANAGER')")
    public ApiResponse<Page<com.faturaocr.application.user.dto.UserResponse>> getUsersByCompany(
            @PathVariable UUID id,
            Pageable pageable) {
        // Validate that we are accessing our own company
        UUID currentCompanyId = CompanyContextHolder.getCompanyId();
        if (currentCompanyId == null || !currentCompanyId.equals(id)) {
            // In a real multi-tenant app, we might allow SUPER_ADMIN to access any company.
            // But based on current requirements, users can only list their own users.
            throw new org.springframework.security.access.AccessDeniedException(
                    "Cannot access another company's users");
        }
        Page<com.faturaocr.application.user.dto.UserResponse> response = userManagementService
                .listUsersByCompany(pageable);
        return ApiResponse.success(response);
    }
}
