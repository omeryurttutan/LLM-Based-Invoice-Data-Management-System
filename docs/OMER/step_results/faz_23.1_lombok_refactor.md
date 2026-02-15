# Refactoring Result: Explicit Lombok Annotations

## Actions Taken

In response to the request to usage of getter/setter annotations, I have refactored the key DTOs involved in the recent Invoice Filtering feature to use explicit Lombok annotations instead of the aggregate `@Data` annotation. This improves code clarity and follows strict Java Bean conventions.

### Refactored Classes

- **`InvoiceFilterRequest.java`**
  - Replaced `@Data` with `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode`.
- **`FilterOptionsResponse.java`**
  - Replaced `@Data` with explicit annotations for the main class and all nested static inner classes (`StatusOption`, `CategoryOption`, `Range`).
- **`InvoiceListResponse.java`**
  - Replaced `@Data` with explicit annotations.

### Cleanup

- Removed unused `mapToListResponse` method in `InvoiceService`.
- Removed unused imports in `InvoiceController`, `InvoiceSpecificationTest`, and `InvoiceFilteringIntegrationTest`.

## Verification

- **Compilation**: Successful.
- **Tests**:
  - `InvoiceFilteringIntegrationTest`: **PASSED**
  - `InvoiceSpecificationTest`: **PASSED**
  - `CategoryControllerIntegrationTest`: **PASSED**
  - `ArchitectureTest`: Fails on pre-existing domain rule violations (e.g. entities not extending `BaseEntity`), which are unrelated to these changes.

The codebase now explicitly uses Lombok's `@Getter` and `@Setter` annotations for the modified data structures.
