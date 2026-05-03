package com.faturaocr.interfaces.rest.company.dto;

import com.faturaocr.application.company.dto.UpdateCompanyCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCompanyRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String name;

    @Size(max = 255)
    private String taxOffice;

    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String district;

    @Size(max = 10)
    private String postalCode;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must act as valid phone format")
    private String phone;

    @Email(message = "Email must be valid")
    @Size(max = 255)
    private String email;

    @Size(max = 255)
    private String website;

    @Size(max = 3)
    private String defaultCurrency;

    @Size(max = 10)
    private String invoicePrefix;

    public UpdateCompanyCommand toCommand() {
        return UpdateCompanyCommand.builder()
                .name(name)
                .taxOffice(taxOffice)
                .address(address)
                .city(city)
                .district(district)
                .postalCode(postalCode)
                .phone(phone)
                .email(email)
                .website(website)
                .defaultCurrency(defaultCurrency)
                .invoicePrefix(invoicePrefix)
                .build();
    }
}
