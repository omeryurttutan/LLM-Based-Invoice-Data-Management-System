package com.faturaocr.application.company.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateCompanyCommand {
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
}
