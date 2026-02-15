package com.faturaocr.infrastructure.adapter.extraction;

import com.faturaocr.infrastructure.adapter.extraction.dto.ExtractionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.time.Duration;

@Component
public class PythonExtractionClient {

    private final RestClient restClient;
    private final String extractionServiceUrl;
    private final long timeoutSeconds;

    public PythonExtractionClient(
            RestClient.Builder restClientBuilder,
            @Value("${upload.extraction-service-url:http://extraction-service:8000}") String extractionServiceUrl,
            @Value("${upload.extraction-timeout-seconds:90}") long timeoutSeconds) {
        this.extractionServiceUrl = extractionServiceUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.restClient = restClientBuilder
                .baseUrl(extractionServiceUrl)
                .build();
    }

    public ExtractionResult extract(Path filePath) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new FileSystemResource(filePath));

        return restClient.post()
                .uri("/extract")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(ExtractionResult.class);
    }
}
