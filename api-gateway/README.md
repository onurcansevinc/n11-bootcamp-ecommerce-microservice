# API Gateway

Tüm istemci istekleri için tek giriş noktasıdır. Route tanımları, JWT resource server güvenliği, CORS, correlation id ve merkezi hata cevabı burada toplanır.

## Port

- `8760`

## Sorumluluklar

- İstemciyi servis topolojisinden izole etmek
- JWT doğrulaması yapmak
- CORS politikasını merkezi yönetmek
- Correlation id üretmek ve taşımak
- Servis hatalarını tutarlı HTTP cevabına dönüştürmek

## Route Haritası

| Path | Hedef |
| --- | --- |
| `/api/v1/products/**` | `product-service` |
| `/api/v1/categories/**` | `product-service` |
| `/api/v1/inventory/**` | `inventory-service` |
| `/api/v1/carts/**` | `cart-service` |
| `/api/v1/orders/**` | `order-service` |
| `/api/v1/payments/**` | `payment-service` |

## Çapraz Kesit Davranışları

- `SecurityConfig`: resource server güvenliği
- `KeycloakJwtAuthoritiesConverter`: JWT claim’lerinden role/authority üretimi
- `CorrelationIdFilter`: istek izleme için correlation id
- `GatewayErrorWebExceptionHandler`: ortak hata formatı

## Konfigürasyon Kaynağı

Gateway route ve CORS ayarları config server üzerinden gelir:

- [../config-server/src/main/resources/config/api-gateway.properties](../config-server/src/main/resources/config/api-gateway.properties)

Burada:

- route tanımları
- `allowedOriginPatterns`
- timeout değerleri
- rate limit anahtarları

tanımlıdır.

## Çalıştırma

```bash
./mvnw -f api-gateway/pom.xml spring-boot:run
```

Test:

```bash
./mvnw -pl api-gateway test
```

## Bağımlılıklar

- `config-server`
- `discovery-server`
- `keycloak`

## Önemli Dosyalar

- [src/main/resources/application.properties](src/main/resources/application.properties)
- [src/main/java/com/ecommerce/microservices/api_gateway/common/security/SecurityConfig.java](src/main/java/com/ecommerce/microservices/api_gateway/common/security/SecurityConfig.java)
- [src/main/java/com/ecommerce/microservices/api_gateway/common/filter/CorrelationIdFilter.java](src/main/java/com/ecommerce/microservices/api_gateway/common/filter/CorrelationIdFilter.java)
