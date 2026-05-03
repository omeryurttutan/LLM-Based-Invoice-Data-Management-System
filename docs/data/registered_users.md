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

---

## Süper Admin (SaaS Yöneticisi)

SaaS platformunun tümünü (şirketleri onaylama, askıya alma, kotalar) yönetmek için kullanılan en yetkili hesaptır. Güvenlik nedeniyle bu hesap veritabanına otomatik (hardcoded) olarak eklenmez.

Süper Admin hesabı, backend çalıştırılırken ortam değişkenleri (environment variables) aracılığıyla otomatik oluşturulur:

**E-posta:** `admin@yourplatform.com` (veya belirlediğiniz e-posta)
**Şifre:** `MySecureP@ss123!` (veya belirlediğiniz şifre)
**Rol:** **SUPER_ADMIN**

*Bu hesaba erişmek için backend'i aşağıdaki değişkenlerle başlatmanız gerekir:*
```bash
export SUPER_ADMIN_EMAIL=admin@yourplatform.com
export SUPER_ADMIN_PASSWORD=MySecureP@ss123!
```
*(Bu hesap platform seviyesindedir, herhangi bir şirkete bağlı değildir)*
