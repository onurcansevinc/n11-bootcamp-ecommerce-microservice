# Discovery Server

Netflix Eureka tabanlı servis keşif sunucusudur. Spring servisleri kendilerini buraya kaydeder ve birbirlerini servis adı üzerinden bulur.

## Port

- `8761`

## Sorumluluklar

- Servis kaydı tutmak
- Servis registry bilgisini dağıtmak
- Gateway ve servisler arası isim çözümünü kolaylaştırmak

## Çalışma Notları

- Gateway, `lb://service-name` route’ları için Eureka bilgisini kullanır.
- `config-server`, `product-service`, `cart-service` ve diğer Spring servisleri burada register olur.
- Docker içinde servisler arası çağrılar için hem container DNS hem de discovery birlikte çalışır; gateway tarafında discovery kritiktir.

## Çalıştırma

```bash
./mvnw -f discovery-server/pom.xml spring-boot:run
```

Test:

```bash
./mvnw -pl discovery-server test
```

## Bağımlılıklar

- Yok

## Önemli Dosyalar

- [src/main/resources/application.properties](src/main/resources/application.properties)
