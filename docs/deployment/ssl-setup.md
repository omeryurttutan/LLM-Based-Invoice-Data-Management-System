# SSL/TLS Setup Guide

## 1. Development & Staging (Self-Signed)

For development, testing, or internal staging environments, you can use the provided script to generate a self-signed certificate.

### Usage
```bash
# Run from the project root
cd scripts
./generate-ssl-cert.sh
```
This will create `cert.pem` and `key.pem` in `nginx/ssl/`.
**Note**: Browsers will show a security warning because the certificate authority is unknown. You can proceed by clicking "Advanced" -> "Proceed to localhost (unsafe)".

## 2. Production (Let's Encrypt)

For production, you should use a valid certificate from a trusted authority like Let's Encrypt. We recommend using **Certbot**.

### Step-by-Step Guide

1.  **Install Certbot** on your host machine:
    ```bash
    sudo apt-get update
    sudo apt-get install certbot
    ```

2.  **Obtain Certificate** (Standalone Mode):
    Stop any service running on port 80 before running this.
    ```bash
    sudo certbot certonly --standalone -d your-domain.com
    ```

3.  **Mount Certificates in Docker**
    Update `docker-compose.prod.yml` to mount the live certificates:

    ```yaml
    nginx:
      volumes:
        - /etc/letsencrypt/live/your-domain.com/fullchain.pem:/etc/nginx/ssl/cert.pem:ro
        - /etc/letsencrypt/live/your-domain.com/privkey.pem:/etc/nginx/ssl/key.pem:ro
    ```

4.  **Auto-Renewal**
    Certbot automatically installs a renewal cron job. However, Nginx needs to reload the certificate after renewal.
    Add a deploy hook to Certbot:
    ```bash
    # /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh
    docker compose -f /path/to/docker-compose.prod.yml restart nginx
    ```
