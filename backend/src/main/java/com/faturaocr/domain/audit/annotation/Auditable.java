package com.faturaocr.domain.audit.annotation;

import com.faturaocr.domain.audit.valueobject.AuditActionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for automatic audit logging via AOP.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    AuditActionType action();

    String entityType();

    String description() default "";
}
