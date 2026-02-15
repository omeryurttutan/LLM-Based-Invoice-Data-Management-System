package com.faturaocr.testutil;

import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Role;

import java.util.UUID;

public class TestFixtures {

    public static final UUID COMPANY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID ADMIN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID INVOICE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    public static Company defaultCompany() {
        return TestDataBuilder.aCompany()
                .withId(COMPANY_ID)
                .withName("Default Inc.")
                .withTaxNumber("1234567890")
                .build();
    }

    public static User defaultUser() {
        return TestDataBuilder.aUser()
                .withId(USER_ID)
                .withCompanyId(COMPANY_ID)
                .withEmail("user@default.com")
                .withRole(Role.ACCOUNTANT) // Fixed
                .build();
    }

    public static User adminUser() {
        return TestDataBuilder.aUser()
                .withId(ADMIN_ID)
                .withCompanyId(COMPANY_ID)
                .withEmail("admin@default.com")
                .withRole(Role.ADMIN)
                .build();
    }

    public static Invoice defaultInvoice() {
        return TestDataBuilder.anInvoice()
                .withId(INVOICE_ID)
                .withCompanyId(COMPANY_ID)
                .withInvoiceNumber("INV-001")
                .build();
    }
}
