package com.faturaocr.domain.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be masked in audit logs.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AuditMask {
    MaskType value() default MaskType.PARTIAL;

    enum MaskType {
        FULL, // [MASKED]
        PARTIAL, // 1234****
        EMAIL, // a***@b.com
        PHONE // +90 5** *** **12 (Simplified masking)
    }
}
