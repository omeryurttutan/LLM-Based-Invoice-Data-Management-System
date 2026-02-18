#!/bin/bash
set -e

# Directory where certificates will be stored
SSL_DIR="../nginx/ssl"
mkdir -p "$SSL_DIR"

# Generate a self-signed certificate
# -x509: Output a X.509 certificate structure instead of a cert request
# -nodes: Do not encrypt the private key
# -days 365: Validity of the certificate
# -newkey rsa:2048: Generate a new RSA key of 2048 bits
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout "$SSL_DIR/key.pem" \
  -out "$SSL_DIR/cert.pem" \
  -subj "/C=TR/ST=Istanbul/L=Istanbul/O=FaturaOCR/OU=Dev/CN=localhost"

echo "✅ Self-signed SSL certificate generated at $SSL_DIR"
chmod 644 "$SSL_DIR/cert.pem"
chmod 600 "$SSL_DIR/key.pem"
