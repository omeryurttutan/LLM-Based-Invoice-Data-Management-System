package com.faturaocr.application.company.dto;

import com.faturaocr.domain.company.entity.Company;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CompanyResponse {
    private UUID id;
    private String name;
    private String taxNumber;
    private String taxOffice;
    private String address;
    private String city;
    private String district;
    private String postalCode;
    private String phone;
    private String email;
    private String website;
    private String defaultCurrency;
    private String invoicePrefix;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CompanyResponse fromDomain(Company company) {
        if (company == null) {
            return null;
        }
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .taxNumber(company.getTaxNumber())
                .taxOffice(company.getTaxOffice())
                .address(company.getAddress())
                .city(company.getCity())
                .district(company.getDistrict())
                .postalCode(company.getPostalCode())
                .phone(company.getPhone())
                .email(company.getEmail())
                .website(company.getWebsite())
                .defaultCurrency(company.getDefaultCurrency())
                .invoicePrefix(company.getInvoicePrefix())
                .isActive(company.isActive())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}
