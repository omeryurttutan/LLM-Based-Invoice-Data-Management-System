# PHASE 30-A: TEMPLATE LEARNING & RULE ENGINE LLM-based extraction
 - **Next.js Frontend**: Port 3001
 - **RabbitMQ**: Port 5673
 - **Redis**: Port 6380

### Current State (Phases 0-29 Completed)
- Phase 10-12: Frontend layout (sidebar, dark/light mode), auth pages, invoice list/CRUD UI
- preprocessing, Gemini + GPT + Claude fallback, validation & confidence score, e-Invoice XML parser
- Producer (Spring Boot) + Consumer (Python), extraction queue, result queue, DLQ, retry logic
- Phase 23-26: Filtering & Search, Export (XLSX/CSV + accounting formats), Dashboard (backend stats + frontend charts)
- Phase 29: Version History & Rollback (invoice_versions table, JSONB snapshots, diff, revert)

### Relevant Existing Infrastructure

**Invoice Entity (Phase 7)**: Full invoice with fields description, quantity, unit_price, tax_rate, tax_amount, line_total

**Verification Workflow (Phase 22)**: When a user verifies an invoice (changes status from PENDING to VERIFIED), corrections may have been made. The corrections are tracked. This is the key moment where template learning happens id, name, color, company_id. Used for organizing invoices.

**Notification Service (Phase 27)**: Can trigger notifications for system events. Will be used to notify when rules are auto-applied.

**Audit Log (Phase 8)**: Records all data changes. Template and rule operations should be logged.

**Company Scoping**: All data is isolated per company_id. Templates and rules belong to a specific company.

### What is the Template & Rule Engine?

This phase introduces two related but distinct features:

**1. Supplier Templates (Learning System):**
The system learns from verified invoices. When a supplier's invoice is verified (with or without corrections), the system captures the extraction pattern assign category 'Office Supplies'"
- "If total amount > 50,000 TRY auto-verify"
- "If confidence score < 60 database tables, template learning logic, rule engine, REST APIs, pipeline integration
- **30-B**: Frontend (FURKAN) companies(id) | Owning company |
| supplier_tax_number | VARCHAR(20) | NOT NULL | Supplier identifier (VKN or TCKN) |
| supplier_name | VARCHAR(255) | NOT NULL | Supplier display name (latest known) |
| sample_count | INTEGER | NOT NULL, DEFAULT 0 | How many verified invoices contributed to this template |
| learned_data | JSONB | NOT NULL, DEFAULT '{}' | Learned extraction patterns (see schema below) |
| default_category_id | BIGINT | NULL, FK users(id) | System or user who triggered creation |

**learned_data JSONB Schema:**
This stores aggregated patterns from verified invoices. Structure it as follows:

- `field_accuracy`: Object mapping field names to accuracy stats. For each field, track how often the LLM got it right vs. how often the user corrected it. Fields: invoice_number, invoice_date, due_date, supplier_name, supplier_tax_number, buyer_name, buyer_tax_number, subtotal, tax_amount, total_amount, currency.
 - Each field entry: `{ "correct_count": N, "corrected_count": N, "total_count": N }`
- `common_corrections`: Array of objects recording the most frequent corrections. Each: `{ "field": "category", "original_value": null, "corrected_to": "Office Supplies", "frequency": 5 }`
- `typical_line_item_count`: Average number of line items
- `typical_amount_range`: `{ "min": 100.00, "max": 5000.00, "average": 1250.50 }`

**Indexes:**
- UNIQUE constraint on (company_id, supplier_tax_number)
- idx_supplier_templates_company ON (company_id)
- idx_supplier_templates_supplier ON (supplier_tax_number)
- idx_supplier_templates_active ON (company_id, is_active) WHERE is_active = TRUE

---

### 2. Database: automation_rules Table

Add to the same Flyway migration or create a second: `V{next_number}__phase_30a_automation_rules.sql`

**automation_rules table:**

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | |
| company_id | BIGINT | NOT NULL, FK users(id) | |

**trigger_point Enum Values:**

| Value | Description |
|---|---|
| AFTER_EXTRACTION | Runs after LLM extraction is complete (before user sees it) |
| AFTER_VERIFICATION | Runs after user verifies an invoice |
| ON_MANUAL_CREATE | Runs when a user manually creates an invoice |

**conditions JSONB Schema:**
Array of condition objects. Each condition:

- `field`: The invoice field to evaluate. Allowed values: `supplier_name`, `supplier_tax_number`, `total_amount`, `subtotal`, `tax_amount`, `currency`, `source_type`, `confidence_score`, `category_name`, `status`, `llm_provider`, `invoice_date`
- `operator`: Comparison operator. Allowed values: `equals`, `not_equals`, `contains`, `not_contains`, `greater_than`, `less_than`, `greater_than_or_equal`, `less_than_or_equal`, `in`, `not_in`, `is_null`, `is_not_null`, `between`
- `value`: The comparison value. Type depends on operator. For `in`/`not_in` it is an array. For `between` it is an object with `min` and `max`. For `is_null`/`is_not_null` it is omitted.

Example conditions array:
- Supplier name equals "ABC Ltd": `{ "field": "supplier_name", "operator": "equals", "value": "ABC Ltd" }`
- Total amount greater than 50000: `{ "field": "total_amount", "operator": "greater_than", "value": 50000 }`
- Source type is LLM: `{ "field": "source_type", "operator": "equals", "value": "LLM" }`
- Confidence score between 50 and 70: `{ "field": "confidence_score", "operator": "between", "value": { "min": 50, "max": 70 } }`

**actions JSONB Schema:**
Array of action objects. Each action:

- `type`: The action to perform. Allowed values: `SET_CATEGORY`, `SET_STATUS`, `ADD_NOTE`, `FLAG_FOR_REVIEW`, `SEND_NOTIFICATION`, `SET_PRIORITY`
- `params`: Object with action-specific parameters.

Action type details:

| Action Type | Params | Description |
|---|---|---|
| SET_CATEGORY | `{ "category_id": 5 }` | Assign a specific category |
| SET_STATUS | `{ "status": "VERIFIED" }` | Change invoice status (only VERIFIED or REJECTED allowed from rules) |
| ADD_NOTE | `{ "note": "Auto-flagged by rule" }` | Append a note to the invoice |
| FLAG_FOR_REVIEW | `{ "reason": "High amount requires manager review" }` | Mark for manual review (sets a flag, does not change status) |
| SEND_NOTIFICATION | `{ "message": "Large invoice detected", "target_roles": ["MANAGER"] }` | Send notification via existing notification system |
| SET_PRIORITY | `{ "priority": "HIGH" }` | Placeholder for future priority field automation_rules(id) | Which rule was executed |
| invoice_id | BIGINT | NOT NULL, FK companies(id) | For company scoping |
| trigger_point | VARCHAR(50) | NOT NULL | When it was triggered |
| conditions_matched | JSONB | NOT NULL | Snapshot of the conditions that matched |
| actions_applied | JSONB | NOT NULL | Snapshot of the actions that were applied |
| execution_result | VARCHAR(20) | NOT NULL | SUCCESS or FAILED |
| error_message | TEXT | NULL | Error details if failed |
| executed_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**Indexes:**
- idx_rule_exec_log_invoice ON (invoice_id)
- idx_rule_exec_log_rule ON (rule_id)
- idx_rule_exec_log_company ON (company_id, executed_at DESC)

---
### 0. Pre-Requisite Migration: extraction_corrections Column

Phase 22 (Verification UI) sends a corrections object in the PUT request body when a user edits and saves an invoice. This object records `{ field_name, original_value, corrected_value }` for each modified field. However, no previous phase defined a database column to persist this data. The template learning service (section 4.1) depends on this data to calculate field accuracy and track common corrections.

**Before implementing the template learning service, create a Flyway migration** that adds the following column to the `invoices` table:

**Migration file**: `V{next_number}__phase_30a_extraction_corrections.sql`

Add to `invoices` table:
- `extraction_corrections` (JSONB, nullable, default NULL)

This column stores the corrections made during verification. The expected JSON structure is an array:
```json
[
  {
    "field": "supplier_name",
    "original_value": "ABC Ltd.",
    "corrected_value": "ABC Teknoloji Ltd. Şti."
  },
  {
    "field": "total_amount",
    "original_value": 1500.00,
    "corrected_value": 1750.00
  }
]
```

**Backend integration** (InvoiceService update):
- Modify the PUT /api/v1/invoices/{id} endpoint (Phase 7) to accept an optional `extraction_corrections` field in the request body
- When present, persist it to the new `extraction_corrections` column
- Do NOT overwrite: if the invoice already has corrections and the user saves again, merge or replace the array

This migration and service update must be done FIRST in this phase, before implementing the template learning service, because section 4.1 depends on this data.
---
### 4. Supplier Template Service

Build a service that manages supplier templates with the following capabilities:

**4.1 Template Learning (Automatic)**

When an invoice is verified (status changes to VERIFIED), the template learning service should:

1. Check if the invoice has a supplier_tax_number. If not, skip template learning.
2. Look up the existing supplier_template for this company_id + supplier_tax_number.
3. If no template exists, create a new one with initial data from this invoice.
4. If a template exists, update it with the new data point:
  - Increment sample_count
  - Update field_accuracy stats by reading the `extraction_corrections` JSONB column on the invoice (populated by Phase 22's verification flow). For each field in the corrections array, increment `corrected_count` in the template's field_accuracy. For fields NOT in the corrections array, increment `correct_count`. If `extraction_corrections` is NULL (e.g., the user verified without making any changes), treat all fields as correct. As a fallback, if `extraction_corrections` is unavailable, compare the latest version snapshot (Phase 29) with the previous version to detect field-level changes — but the corrections column is the primary and preferred source.
  - Update default_category_id if this category is more common than the previous default
  - Update typical_tax_rates array
  - Recalculate average_confidence
  - Update last_invoice_date
  - Track common_corrections
  - Update typical_amount_range

**4.2 Template Application (Query)**

When a new invoice is extracted (after LLM extraction), the system should:

1. Check if a supplier_template exists for the detected supplier_tax_number + company_id
2. If found and is_active and sample_count >= 3 (configurable minimum):
  - Return the template data so the extraction pipeline or frontend can use it
  - Template data includes: default_category_id, typical_tax_rates, average_confidence, field_accuracy
  - The template does NOT override LLM results standard pagination
- `search` filter by active/inactive
- `minSamples` returns what would happen without applying) | ADMIN, MANAGER |
| GET | /api/v1/rules/{id}/history | Get execution history for a specific rule (from rule_execution_log) | ADMIN, MANAGER |
| GET | /api/v1/rules/execution-log | Get all rule execution logs for the company (paginated) | ADMIN, MANAGER |

All endpoints must enforce company_id scoping.

**Query Parameters for GET /api/v1/rules:**
- `page`, `size`, `sort` search by rule name
- `isActive` filter by trigger point

**5.2 Rule Validation**

When creating or updating a rule, validate:
- `name` is not empty and not longer than 255 characters
- `conditions` array is not empty (at least one condition)
- `actions` array is not empty (at least one action)
- Each condition has a valid `field`, valid `operator`, and appropriate `value` type for the operator
- Each action has a valid `type` and valid `params` for that type
- `condition_logic` is either 'AND' or 'OR'
- `trigger_point` is a valid enum value
- `priority` is between 1 and 1000
- For SET_CATEGORY action: verify the category_id exists and belongs to the same company
- For SET_STATUS action: only allow VERIFIED or REJECTED as target statuses
- No circular dependencies (a rule that triggers on AFTER_VERIFICATION should not set status, which would trigger another verification cycle)

Return clear validation error messages in Turkish for the frontend to display.

**5.3 Rule Evaluation Engine**

The core engine that evaluates rules against an invoice:

1. **Input**: An invoice object and a trigger_point
2. **Fetch**: All active rules for the invoice's company_id with matching trigger_point, ordered by priority ASC
3. **For each rule**:
  - Evaluate all conditions against the invoice data
  - If condition_logic is 'AND': all conditions must match
  - If condition_logic is 'OR': at least one condition must match
  - If conditions match: execute all actions
  - Log the execution in rule_execution_log
  - Update the rule's execution_count and last_executed_at
4. **Output**: A summary of which rules matched and what actions were applied

**Condition Evaluation Logic:**

For each condition, extract the invoice field value and compare:

- `equals`: Direct comparison (case-insensitive for strings)
- `not_equals`: Negation of equals
- `contains`: String contains check (case-insensitive)
- `not_contains`: Negation of contains
- `greater_than`, `less_than`, `greater_than_or_equal`, `less_than_or_equal`: Numeric comparison
- `in`: Value is in the provided array
- `not_in`: Value is not in the provided array
- `is_null`: Field value is null or empty
- `is_not_null`: Field value is not null and not empty
- `between`: Value is between min and max (inclusive)

**Action Execution:**

For each matched action:

- `SET_CATEGORY`: Update the invoice's category. Validate category belongs to the same company.
- `SET_STATUS`: Update invoice status. Must follow valid status transitions. Be careful: do not trigger an infinite loop (AFTER_VERIFICATION rule sets status triggers rule again). Add a guard flag to prevent re-entry.
- `ADD_NOTE`: Append text to the invoice's notes field. Prefix with "[Auto-Rule: {rule_name}] ".
- `FLAG_FOR_REVIEW`: Add a note flagging for review. Does not change status.
- `SEND_NOTIFICATION`: Use the existing NotificationService (Phase 27) to send an in-app notification to users with the specified roles in the company.
- `SET_PRIORITY`: For now, append priority info to notes (future phases may add a priority field).

---

### 6. Pipeline Integration

**6.1 After Extraction (AFTER_EXTRACTION trigger)**

When the extraction result arrives from the Python service (via RabbitMQ result queue consumer):

1. After saving the extracted invoice data, look up the supplier template
2. If a template exists with sufficient samples:
  - Attach template suggestion data to the invoice response (as metadata, not overwriting LLM results)
  - If template has a default_category and the LLM didn't extract a category, auto-assign it
3. Run the rule engine with trigger_point = AFTER_EXTRACTION
4. Log any rule applications

**6.2 After Verification (AFTER_VERIFICATION trigger)**

When a user verifies an invoice (status depends on operator)

**ActionDTO:**

- type (String, required)
- params (Object, required)

**Rule Response:**

- id, name, description, conditions, actions, conditionLogic, priority, isActive, triggerPoint, executionCount, lastExecutedAt, createdAt, updatedAt, createdBy (user summary)

**Template Response:**

- id, supplierTaxNumber, supplierName, sampleCount, learnedData, defaultCategory (object with id+name), defaultCurrency, typicalTaxRates, averageConfidence, lastInvoiceDate, isActive, createdAt, updatedAt

**Rule Test Response (dry run):**

- ruleId, ruleName, invoiceId
- conditionsEvaluated: Array of { condition, matched: boolean, actualValue }
- allConditionsMet: boolean
- actionsToApply: Array of action descriptions (what would happen)
- note: "This is a dry run. No changes were made."

---

### 8. Testing Requirements

Write tests for the following:

- Template learning: verify that verifying an invoice creates/updates a supplier template
- Template lookup: verify template is found by supplier_tax_number + company_id
- Rule CRUD: create, read, update, delete, toggle
- Rule validation: invalid conditions, invalid actions, missing required fields
- Rule evaluation: test AND logic, OR logic, each operator type
- Rule execution: verify actions are applied correctly
- Pipeline integration: verify rules run at the correct trigger points
- Company scoping: verify templates and rules are isolated per company
- RBAC: verify only allowed roles can create/update/delete rules and templates
- Dry run: verify test endpoint returns expected results without modifying data
- Re-entry guard: verify that AFTER_VERIFICATION rules with SET_STATUS do not cause infinite loops

---

### 9. Configuration

Add the following to application.yml (or environment variables):

- `app.templates.min-samples-for-suggestion`: Minimum verified invoices before a template is used for suggestions (default: 3)
- `app.templates.learning-enabled`: Global toggle for template learning (default: true)
- `app.rules.max-rules-per-company`: Maximum number of rules a company can create (default: 50)
- `app.rules.max-conditions-per-rule`: Maximum conditions in a single rule (default: 10)
- `app.rules.max-actions-per-rule`: Maximum actions in a single rule (default: 5)
- `app.rules.execution-timeout-ms`: Timeout for rule execution per invoice (default: 5000)

---

### 10. Error Handling

- If a rule action fails (e.g., invalid category_id at execution time), log the error in rule_execution_log with execution_result = FAILED and error_message, but do NOT fail the entire invoice processing. Continue with the next rule.
- If template learning fails, log the error but do NOT fail the verification operation.
- All errors should be logged with structured logging including company_id, invoice_id, rule_id/template_id.

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/FURKAN/step_results/faz_30.0_result.md`

The result file must include:

1. Phase summary (what was implemented)
2. Database changes (list all migration files created, table definitions)
3. Files created or modified (with paths)
4. API endpoints implemented (method, path, description, RBAC)
5. Template learning flow documentation
6. Rule engine evaluation flow documentation
7. Pipeline integration points documented
8. Configuration properties added
9. Test results (unit and integration tests summary)
10. Coordination notes for Furkan (API contracts, request/response formats for frontend)
11. Issues encountered and solutions
12. Edge cases discovered during testing
13. Next steps (Phase 30-B frontend requirements, any improvements identified)

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 7**: Invoice CRUD API (invoice entity, status workflow, categories)
- **Phase 8**: Audit Log (rule and template operations should be logged)
- **Phase 19**: RabbitMQ (extraction result consumer depends on all REST APIs from this phase
- **Phase 22**: Verification UI workflow (verification triggers template learning). Phase 22's frontend sends an `extraction_corrections` array in the PUT request body when saving corrections. This phase adds the backend column to persist that data and uses it as the primary input for template learning field accuracy calculations.
- **Phase 35**: Unit Tests list works with pagination and search
- [ ] GET /api/v1/templates/{id} update default category works
- [ ] PATCH /api/v1/templates/{id}/toggle resets learned data with confirmation
- [ ] POST /api/v1/rules list rules works with pagination
- [ ] PUT /api/v1/rules/{id} delete rule works
- [ ] PATCH /api/v1/rules/{id}/toggle dry run returns evaluation result without modifying data
- [ ] Rule execution logged in rule_execution_log
- [ ] Company scoping enforced on all template and rule operations
- [ ] RBAC: only ADMIN/MANAGER can create/update/delete rules
- [ ] RBAC: ACCOUNTANT/INTERN can only view rules
- [ ] All tests pass
- [ ] Result file created at docs/OMER/step_results/faz_30.0_result.md
---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. automation_rules table is created with JSONB conditions/actions and proper indexes
3. Template learning automatically triggers when an invoice is verified
5. Template lookup by supplier_tax_number + company_id works correctly
7. Rule validation catches invalid conditions, actions, and circular dependencies
9. Rule actions are applied correctly (SET_CATEGORY, SET_STATUS, ADD_NOTE, FLAG_FOR_REVIEW, SEND_NOTIFICATION)
11. Re-entry guard prevents infinite loops from AFTER_VERIFICATION + SET_STATUS combinations
13. Rule execution is logged in rule_execution_log
15. RBAC is enforced (only ADMIN/MANAGER can create/update/delete rules)
17. Result file is created at docs/OMER/step_results/faz_30.0_result.md

---

## IMPORTANT NOTES

1. **Templates Suggest, They Don't Override**: Supplier templates provide suggestions and metadata. They should NEVER silently override LLM extraction results. The LLM result is always the primary data source. Templates enhance confidence and provide context (e.g., "Based on 15 previous invoices from this supplier, category is usually 'Office Supplies'").

2. **Rules Are Auditable**: Every rule execution must be logged. Users should be able to see exactly which rules were applied to an invoice and what they did. This is critical for trust and debugging.

3. **Fail Gracefully**: Neither template learning nor rule execution should ever cause the main invoice processing flow to fail. If a rule has a bug or a template update fails, log the error and continue normally.

4. **Company Isolation**: Templates and rules are strictly per-company. A supplier template learned in Company A must NOT be visible or usable in Company B, even if the supplier_tax_number is the same.

5. **Performance**: Rule evaluation should be fast. Fetch all active rules for a company + trigger_point in a single query. Evaluate conditions in memory. Set a timeout to prevent runaway rules.

6. **Status Transition Safety**: The SET_STATUS action must respect the valid status transitions defined in Phase 7. Do not allow rules to set arbitrary statuses. Also, be very careful with the re-entry guard to prevent infinite loops.

7. **Coordinate with Furkan**: Share the exact API contracts (endpoints, request/response formats, condition field list, operator list, action type list) with Furkan so he can build the frontend rule builder and template management pages in Phase 30-B.

8. **Correction Data is the Bridge Between Phase 22 and Phase 30**: The `extraction_corrections` column added in this phase connects the frontend verification workflow (Phase 22) to the template learning system. Without this column, the template learning service cannot accurately calculate which fields the LLM got right vs. wrong per supplier. Ensure the PUT /api/v1/invoices/{id} endpoint is updated to accept and persist this field BEFORE implementing the template learning logic. Test this by: (1) verify an invoice with corrections via the API, (2) confirm the `extraction_corrections` column is populated, (3) trigger template learning and verify field_accuracy stats are updated correctly.
