package com.faturaocr.infrastructure.export.accounting;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "export.accounting")
public class AccountingExportConfig {

    private LogoConfig logo = new LogoConfig();
    private MikroConfig mikro = new MikroConfig();
    private NetsisConfig netsis = new NetsisConfig();
    private LucaConfig luca = new LucaConfig();

    @Data
    public static class LogoConfig {
        private String dateFormat = "dd.MM.yyyy";
        private String numberFormat = "###,##0.00";
        private Map<String, String> fieldMapping;
    }

    @Data
    public static class MikroConfig {
        private String dateFormat = "dd.MM.yyyy";
        private String delimiter = "|";
        private Map<String, String> fieldMapping;
    }

    @Data
    public static class NetsisConfig {
        private String dateFormat = "yyyy-MM-dd";
        private Map<String, String> fieldMapping;
    }

    @Data
    public static class LucaConfig {
        private String dateFormat = "dd.MM.yyyy";
        private Map<String, String> fieldMapping;
    }
}
