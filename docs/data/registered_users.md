# Kayıtlı Test Kullanıcıları

Sistemi test edebilmeniz için veritabanına varsayılan olarak eklenen kullanıcıların listesi aşağıdadır. Tüm kullanıcılar aynı test şirketine ("Demo Şirketi") bağlıdır.

**Ortak Şifre:** `Admin123!`

| İsim Soyisim | E-posta | Rol |
| :--- | :--- | :--- |
| Sistem Yöneticisi | `admin@demo.com` | **ADMIN** |
| Test Yöneticisi | `manager@demo.com` | **MANAGER** |
| Test Muhasebeci | `accountant@demo.com` | **ACCOUNTANT** |
| Test Stajyer | `intern@demo.com` | **INTERN** |

> **Not:** Eğer daha önce Docker üzerinden veritabanını (`docker-compose up -d`) başlattıysanız, backend tekrar çalıştığında (Flyway) bu yeni test kullanıcılarını otomatik olarak veritabanına ekleyecektir.
