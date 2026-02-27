-- Widen original_file_type to accommodate long MIME types like
-- application/vnd.openxmlformats-officedocument.spreadsheetml.sheet (71 chars)
ALTER TABLE invoices ALTER COLUMN original_file_type TYPE VARCHAR(255);
