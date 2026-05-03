package com.faturaocr.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomMetricsConfig {

    @Bean
    public Counter invoiceExtractionTotal(MeterRegistry registry) {
        return Counter.builder("invoice.extraction.total")
                .description("Total number of extraction requests sent to the Python service")
                .register(registry);
    }

    @Bean
    public Counter invoiceExtractionSuccess(MeterRegistry registry) {
        return Counter.builder("invoice.extraction.success")
                .description("Successful extractions")
                .register(registry);
    }

    @Bean
    public Counter invoiceExtractionFailure(MeterRegistry registry) {
        return Counter.builder("invoice.extraction.failure")
                .description("Failed extractions (all providers failed)")
                .register(registry);
    }

    @Bean
    public Timer invoiceExtractionDuration(MeterRegistry registry) {
        return Timer.builder("invoice.extraction.duration")
                .description("Time taken for extraction")
                .register(registry);
    }

    @Bean
    public Counter invoiceVerificationTotal(MeterRegistry registry) {
        return Counter.builder("invoice.verification.total")
                .description("Total invoices verified manually")
                .register(registry);
    }

    @Bean
    public Counter invoiceUploadTotal(MeterRegistry registry) {
        return Counter.builder("invoice.upload.total")
                .description("Total file uploads")
                .register(registry);
    }

    @Bean
    public Counter loginAttemptSuccess(MeterRegistry registry) {
        return Counter.builder("login.attempt")
                .tag("result", "success")
                .description("Successful login attempts")
                .register(registry);
    }

    @Bean
    public Counter loginAttemptFailure(MeterRegistry registry) {
        return Counter.builder("login.attempt")
                .tag("result", "failure")
                .description("Failed login attempts")
                .register(registry);
    }
}
