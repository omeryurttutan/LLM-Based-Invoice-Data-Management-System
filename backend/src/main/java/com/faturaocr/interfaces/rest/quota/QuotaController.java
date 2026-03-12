package com.faturaocr.interfaces.rest.quota;

import com.faturaocr.application.company.QuotaService;
import com.faturaocr.interfaces.rest.common.ApiResponse;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import com.faturaocr.domain.common.exception.DomainException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller to provide quota information to the frontend.
 */
@RestController
@RequestMapping("/api/v1/quota")
public class QuotaController {

    private final QuotaService quotaService;

    public QuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @GetMapping
    public ApiResponse<QuotaService.QuotaInfo> getQuotaInfo() {
        UUID companyId = CompanyContextHolder.getCompanyId();
        if (companyId == null) {
            throw new DomainException("COMPANY_CONTEXT_REQUIRED", "Company context is required");
        }
        QuotaService.QuotaInfo info = quotaService.getQuotaInfo(companyId);
        return ApiResponse.success("Quota information retrieved", info);
    }
}
