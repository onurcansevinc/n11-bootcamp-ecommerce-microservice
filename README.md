# n11 Bootcamp E-Commerce Microservice

Spring Boot ve React tabanlı bu proje, tipik bir e-ticaret akışını mikroservis mimarisi ile böler. Katalog, stok, sepet, sipariş, ödeme, bildirim, kimlik doğrulama ve yapılandırma sorumlulukları birbirinden ayrılmıştır.

## Canlı Ortam

- Storefront: https://java-ecommerce.onurcansevinc.com
- API health: https://java-ecommerce-api.onurcansevinc.com/actuator/health
- Keycloak realm: https://java-ecommerce-auth.onurcansevinc.com/realms/ecommerce

Demo kullanıcı akışı ve örnek veriler için aşağıdaki `Seed Data ve Demo` bölümüne bakılabilir.

## Modüller

| Modül | Rol | Doküman |
| --- | --- | --- |
| `storefront` | React/Vite kullanıcı arayüzü | [storefront/README.md](storefront/README.md) |
| `api-gateway` | Tek giriş noktası, route ve güvenlik | [api-gateway/README.md](api-gateway/README.md) |
| `product-service` | Ürün ve kategori kataloğu | [product-service/README.md](product-service/README.md) |
| `inventory-service` | Stok ve rezervasyon yönetimi | [inventory-service/README.md](inventory-service/README.md) |
| `cart-service` | Aktif sepet ve sepet kalemleri | [cart-service/README.md](cart-service/README.md) |
| `order-service` | Sipariş orkestrasyonu | [order-service/README.md](order-service/README.md) |
| `payment-service` | Ödeme kaydı, Iyzico ve outbox akışı | [payment-service/README.md](payment-service/README.md) |
| `notification-service` | E-posta ve SMS bildirimleri | [notification-service/README.md](notification-service/README.md) |
| `config-server` | Merkezi Spring konfigürasyonu | [config-server/README.md](config-server/README.md) |
| `discovery-server` | Eureka servis keşfi | [discovery-server/README.md](discovery-server/README.md) |
| `service-common-web` | Ortak HTTP ve güvenlik yardımcıları | [service-common-web/README.md](service-common-web/README.md) |
| `service-common-events` | Ortak event contract’ları | [service-common-events/README.md](service-common-events/README.md) |

## Mimari

```text
Storefront
   |
   v
API Gateway
   |
   +--> Product Service
   +--> Inventory Service
   +--> Cart Service
   +--> Order Service ---> Payment Service ---> RabbitMQ ---> Notification Service
                                 |                    |
                                 +--> Keycloak        +--> Iyzico

Config Server -> tüm Spring servisleri
Discovery Server -> tüm Spring servisleri
PostgreSQL -> ürün, stok, sepet, sipariş, ödeme, bildirim verisi
Redis -> katalog cache
MailHog -> demo SMTP çıkışı
```

## Servisler Arası Temel Akışlar

### Katalog Akışı

1. `storefront`, ürün ve kategori isteklerini `api-gateway` üzerinden gönderir.
2. `api-gateway`, `/api/v1/products/**` ve `/api/v1/categories/**` route’larını `product-service`e yönlendirir.
3. `product-service`, PostgreSQL ve Redis cache ile katalog verisini döner.

### Sepet Akışı

1. Kullanıcı giriş yaptıktan sonra `cart-service` aktif sepeti oluşturur veya mevcut sepeti döner.
2. Ürün ekleme sırasında `cart-service`, ürün bilgisini `product-service`ten çeker.
3. Sepet servisi kendi veritabanında kalemleri tutar; katalog verisinin sahibi değildir.

### Sipariş ve Ödeme Akışı

1. `order-service`, sepeti doğrular ve `inventory-service` üzerinden rezervasyon açar.
2. Sipariş oluştuğunda `payment-service` için ödeme başlatılır.
3. `payment-service`, Iyzico callback’i veya sandbox simulate uç noktaları ile sonucu işler.
4. Ödeme sonucu önce kendi outbox tablosuna yazılır, sonra RabbitMQ üzerinden yayınlanır.
5. `order-service` event’i dinleyip sipariş durumunu günceller.
6. `notification-service` aynı event’i dinleyip e-posta veya SMS kaydı üretir.

### Kimlik Doğrulama Akışı

1. `storefront`, OIDC login akışını Keycloak ile başlatır.
2. Kullanıcı JWT aldıktan sonra backend çağrılarını `api-gateway` üzerinden yapar.
3. Gateway ve ilgili servisler JWT issuer bilgisine göre token doğrulaması yapar.
4. Servisler arası teknik çağrılar için `service-internal` client credentials akışı kullanılır.

## Ana Özellikler

- Ürün ve kategori listeleme
- Sayfalama, filtreleme ve ürün detay akışı
- JWT tabanlı OAuth2 güvenliği
- Sepet oluşturma ve sepet kalemi yönetimi
- Stok rezervasyonu, onay ve serbest bırakma
- Sipariş oluşturma ve ödeme sonucu ile durum güncelleme
- Iyzico entegrasyonu ve sandbox simulate uç noktaları
- Outbox deseni ile event publish
- RabbitMQ tabanlı bildirim akışı
- Docker, Jib, GitHub Actions ve Jenkins hazırlığı

## Teknoloji Yığını

- Java 21
- Spring Boot 3.5.x
- Spring Cloud Config, Eureka ve Gateway
- Spring Security OAuth2 Resource Server
- PostgreSQL
- Redis
- RabbitMQ
- Flyway
- React 19
- Vite
- Keycloak
- Docker ve Docker Compose
- Jib
- GitHub Actions
- Jenkins

## Portlar

| Bileşen | Port |
| --- | --- |
| API Gateway | `8760` |
| Discovery Server | `8761` |
| Config Server | `8762` |
| Product Service | `8763` |
| Inventory Service | `8764` |
| Cart Service | `8765` |
| Order Service | `8766` |
| Payment Service | `8767` |
| Notification Service | `8768` |
| Keycloak | `8080` |
| PostgreSQL | `5432` |
| Redis | `6379` |
| RabbitMQ | `5672` |
| RabbitMQ UI | `15672` |
| MailHog SMTP | `1025` |
| MailHog UI | `8025` |
| Storefront | `4173` |

## Hızlı Başlangıç

### Gereksinimler

- Java 21
- Node.js 22
- Docker ve Docker Compose

### Sadece Altyapıyı Kaldırma

```bash
docker compose up -d
```

Bu mod, veritabanı ve diğer bağımlılıkları ayağa kaldırır. Spring servislerini yerelde `spring-boot:run` ile çalıştırmak istediğinde kullanışlıdır.

### Tam Stack’i Docker ile Kaldırma

```bash
docker compose --env-file .env.production.example -f docker-compose.yml -f docker-compose.apps.yml up -d --build
```

Bu modda config, discovery, tüm Spring servisleri, Keycloak ve storefront birlikte ayağa kalkar.

### Frontend’i Yerelde, Backend’i Docker’da Çalıştırma

```bash
cd storefront
npm ci
npm run dev
```

Bu senaryoda `VITE_API_BASE_URL` ve `VITE_OIDC_AUTHORITY` değerlerinin doğru ortamı işaret etmesi gerekir.

### Tek Bir Servisi Yerelde Çalıştırma

Örnek:

```bash
./mvnw -f product-service/pom.xml spring-boot:run
```

Notlar:

- Spring servisleri config’i `config-server` üzerinden alır.
- Docker içinde `localhost` yerine servis adı kullanılmalıdır.
- Docker senaryosunda `CONFIG_URI=http://config-server:8762` ve `EUREKA_URI=http://discovery-server:8761/eureka` beklenir.

## Ortam Değişkenleri

Production referansı:

- [.env.production.example](.env.production.example)

Önemli değişken grupları:

- Altyapı adresleri: `CONFIG_URI`, `EUREKA_URI`, `POSTGRES_HOST`, `REDIS_HOST`, `RABBITMQ_HOST`
- Kimlik doğrulama: `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_TOKEN_URI`, `INTERNAL_SERVICE_CLIENT_*`
- Frontend public adresleri: `VITE_API_BASE_URL`, `VITE_OIDC_AUTHORITY`
- Ödeme: `IYZICO_*`, `PAYMENT_RESULT_URL`
- Bildirim: `SMTP_*`, `NOTIFICATION_MAIL_FROM`

Kritik notlar:

- `VITE_*` değişkenleri build anında gömülür. Değer değiştiyse storefront image’ı yeniden build edilmelidir.
- Docker ağı içinde servisler `localhost` ile değil servis adı ile haberleşir.
- Domain kullanılıyorsa Keycloak redirect URI ve gateway CORS ayarları domainlerle uyumlu olmalıdır.

## Test ve Doğrulama

Tüm backend testleri:

```bash
./mvnw test
```

Belirli bir servis:

```bash
./mvnw -pl order-service test
./mvnw -pl payment-service test
./mvnw -pl notification-service test
```

Frontend build:

```bash
cd storefront
npm ci
npm run build
```

Compose doğrulaması:

```bash
docker compose --env-file .env.production.example -f docker-compose.yml -f docker-compose.apps.yml config
```

## Docker

Ana dosyalar:

- [docker-compose.yml](docker-compose.yml)
- [docker-compose.apps.yml](docker-compose.apps.yml)
- [docker-compose.proxy.yml](docker-compose.proxy.yml)
- [Dockerfile.spring](Dockerfile.spring)
- [storefront/Dockerfile](storefront/Dockerfile)

Katmanlar:

- `docker-compose.yml`: PostgreSQL, Redis, RabbitMQ, MailHog, Keycloak gibi temel altyapı
- `docker-compose.apps.yml`: Spring servisleri ve storefront
- `docker-compose.proxy.yml`: Caddy ile domain bazlı yönlendirme

## Jib

Runnable Spring servislerinde `jib-maven-plugin` tanımlıdır. Image build alırken `package` fazını da çalıştırmak gerekir.

Ortak kullanım:

```bash
./mvnw -f <service>/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
```

Örnekler:

```bash
./mvnw -f product-service/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f inventory-service/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f cart-service/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f order-service/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f payment-service/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f notification-service/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f api-gateway/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f config-server/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f discovery-server/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
```

## CI/CD

GitHub Actions workflow dosyaları:

- [.github/workflows/ci.yml](.github/workflows/ci.yml)
- [.github/workflows/docker-images.yml](.github/workflows/docker-images.yml)

`ci.yml` kapsamı:

- backend testleri
- frontend build
- Docker Compose config doğrulaması
- Jib smoke build

`docker-images.yml` kapsamı:

- `main` push veya manuel tetikte GHCR image üretimi
- runnable servisler için Jib tabanlı publish

Jenkins pipeline:

- [Jenkinsfile](Jenkinsfile)

Jenkins aşamaları:

- checkout
- backend test
- frontend build
- Docker Compose config
- Jib smoke build
- opsiyonel GHCR publish

## Domain ve Reverse Proxy

Production örnek domain seti:

- `java-ecommerce.onurcansevinc.com`
- `java-ecommerce-api.onurcansevinc.com`
- `java-ecommerce-auth.onurcansevinc.com`

Hazır nginx örnekleri:

- [deploy/nginx/java-ecommerce.onurcansevinc.com.conf](deploy/nginx/java-ecommerce.onurcansevinc.com.conf)
- [deploy/nginx/java-ecommerce.onurcansevinc.com.http.conf](deploy/nginx/java-ecommerce.onurcansevinc.com.http.conf)
- [deploy/nginx/java-ecommerce.onurcansevinc.com.cloudflare.conf](deploy/nginx/java-ecommerce.onurcansevinc.com.cloudflare.conf)

Önemli notlar:

- Cloudflare edge SSL kullanılıyorsa origin nginx tarafında sadece `80` portu ile çalışmak yeterlidir.
- Host nginx kullanıldığında Docker servisleri tercihen `127.0.0.1` üzerine bind edilmelidir.
- Frontend ve API farklı domainlerdeyse CORS ayarları gateway üzerinden yönetilir.

## Keycloak ve Yetkilendirme

Realm import dosyası:

- [keycloak/import/ecommerce-realm.json](keycloak/import/ecommerce-realm.json)

Temel client’lar:

- `storefront-web`: kullanıcı login akışı
- `service-internal`: servisler arası teknik token akışı

Notlar:

- Storefront login redirect’leri frontend domaini ile eşleşmelidir.
- Gateway ve servisler `issuer-uri` üzerinden JWT doğrulaması yapar.
- Domain değiştiğinde realm içindeki `redirectUris`, `webOrigins` ve logout redirect ayarları güncellenmelidir.

## Seed Data ve Demo

Demo katalog ve stok migration’ları:

- [product-service/src/main/resources/db/migration/V5__normalize_demo_catalog_data.sql](product-service/src/main/resources/db/migration/V5__normalize_demo_catalog_data.sql)
- [inventory-service/src/main/resources/db/migration/V2__seed_demo_inventory.sql](inventory-service/src/main/resources/db/migration/V2__seed_demo_inventory.sql)

Demo akışı:

1. Storefront ana sayfasında ürün ve kategorileri yükle.
2. Demo kullanıcı ile Keycloak üzerinden giriş yap.
3. Bir sepet oluşturup ürünü sepete ekle.
4. Sipariş oluştur ve ödeme başlat.
5. Iyzico callback’i veya simulate uç noktası ile sonucu ilerlet.
6. `notification-service` event’i consume etsin.
7. MailHog üzerinden bildirimleri doğrula.

## Lisans

Bu repo bootcamp ve öğrenme amaçlı hazırlanmıştır.
