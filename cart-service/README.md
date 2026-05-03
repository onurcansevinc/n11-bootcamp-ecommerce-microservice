# Cart Service

Aktif sepetin sahibi servistir. Müşteri bazlı aktif sepeti bulur veya oluşturur; kalem ekleme, silme ve miktar güncelleme akışını yönetir.

## Port

- `8765`

## Sorumluluklar

- Aktif sepet oluşturma
- Sepet kalemi ekleme
- Sepet kalemi güncelleme
- Sepet kalemi silme
- Ürün bilgisini katalog servisinden alma

## HTTP Uç Noktaları

| Metot | Path | Açıklama |
| --- | --- | --- |
| `POST` | `/api/v1/carts` | Mevcut kullanıcı için aktif sepet oluşturur veya döner |
| `GET` | `/api/v1/carts/{cartId}` | Sepet detayını döner |
| `POST` | `/api/v1/carts/{cartId}/items` | Sepete yeni kalem ekler |
| `PATCH` | `/api/v1/carts/{cartId}/items/{itemId}` | Kalem miktarını günceller |
| `DELETE` | `/api/v1/carts/{cartId}/items/{itemId}` | Kalemi sepetten siler |

## Entegrasyon Notları

- Sepetin sahibi `cart-service`tir; ürün verisinin sahibi değildir.
- Kalem eklerken ürün doğrulaması ve katalog bilgisi için `product-service` kullanılır.
- Her işlem mevcut JWT’den çözülen müşteri kimliği ile yapılır.

## Ana Bileşenler

- `CartController`
- `CartService`
- `RestProductCatalogClient`

## Çalıştırma

```bash
./mvnw -f cart-service/pom.xml spring-boot:run
```

Test:

```bash
./mvnw -pl cart-service test
```

## Bağımlılıklar

- `config-server`
- `discovery-server`
- `postgres`
- `keycloak`
- `product-service`

## Önemli Dosyalar

- [src/main/java/com/ecommerce/microservices/cart_service/cart/controller/CartController.java](src/main/java/com/ecommerce/microservices/cart_service/cart/controller/CartController.java)
- [src/main/java/com/ecommerce/microservices/cart_service/cart/service/CartService.java](src/main/java/com/ecommerce/microservices/cart_service/cart/service/CartService.java)
- [src/main/java/com/ecommerce/microservices/cart_service/catalog/client/RestProductCatalogClient.java](src/main/java/com/ecommerce/microservices/cart_service/catalog/client/RestProductCatalogClient.java)
