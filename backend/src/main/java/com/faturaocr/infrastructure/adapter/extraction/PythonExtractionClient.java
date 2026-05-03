package com.faturaocr.infrastructure.adapter.extraction;

import com.faturaocr.infrastructure.adapter.extraction.dto.ExtractionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;

@Component
public class PythonExtractionClient {

    private final RestTemplate restTemplate;
    private final String extractionServiceUrl;
    private final String internalApiKey;

    public PythonExtractionClient(
            @Value("${upload.extraction-service-url:http://localhost:8000}") String extractionServiceUrl,
            @Value("${upload.extraction-timeout-seconds:90}") long timeoutSeconds,
            @Value("${app.security.internal-api-key}") String internalApiKey) {
        this.extractionServiceUrl = extractionServiceUrl;
        this.internalApiKey = internalApiKey;
        this.restTemplate = new RestTemplate();
    }

    public ExtractionResult extract(Path filePath) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Internal-API-Key", internalApiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(filePath));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<ExtractionResult> response = restTemplate.postForEntity(
                extractionServiceUrl + "/api/v1/extraction/extract",
                requestEntity,
                ExtractionResult.class);

        return response.getBody();
    }
}
