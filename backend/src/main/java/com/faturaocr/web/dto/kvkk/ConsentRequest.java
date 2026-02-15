package com.faturaocr.web.dto.kvkk;

import com.faturaocr.domain.kvkk.valueobject.ConsentType;
import lombok.Data;

@Data
public class ConsentRequest {
    private ConsentType consentType;
    private String consentVersion;
    private boolean isGranted;
}
