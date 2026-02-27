#!/bin/bash
export PGPASSWORD=fatura123
psql -h localhost -p 5436 -U fatura -d fatura_ocr -c "SELECT email FROM users WHERE email LIKE '%admin%';"
