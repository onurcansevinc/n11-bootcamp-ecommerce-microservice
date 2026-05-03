# Config Server

Tüm Spring servislerinin merkezi konfigürasyon kaynağıdır. Native profile ile repo içindeki `classpath:/config` dosyalarını servis eder.

## Port

- `8762`

## Sorumluluklar

- Servis bazlı yapılandırma sağlamak
- Ortak environment değerlerini merkezi tutmak
- Gateway route ve CORS ayarlarını tek merkezden vermek

## Servis Ettiği Dosyalar

- `api-gateway.properties`
- `cart-service.properties`
- `inventory-service.properties`
- `notification-service.properties`
- `order-service.properties`
- `payment-service.properties`
- `product-service.properties`

Kaynak dizin:

- [src/main/resources/config](src/main/resources/config)

## Çalışma Notları

- Spring servisleri açılışta `spring.config.import` ile bu servise bağlanır.
- Config server erişilemiyorsa diğer servisler fail-fast davranışı nedeniyle açılmayabilir.
- Docker senaryosunda servis adı olarak `config-server` kullanılır.

## Çalıştırma

```bash
./mvnw -f config-server/pom.xml spring-boot:run
```

Test:

```bash
./mvnw -pl config-server test
```

## Bağımlılıklar

- `discovery-server`

## Önemli Dosyalar

- [src/main/resources/application.properties](src/main/resources/application.properties)
- [src/main/resources/config](src/main/resources/config)
