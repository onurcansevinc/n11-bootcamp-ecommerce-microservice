# Order Service

Sipariş orkestrasyonunun merkezidir. Sepetten sipariş oluşturur, stok rezervasyonu açar ve ödeme event’leri ile sipariş durumunu günceller.

## Port

- `8766`

## Sorumluluklar

- Sipariş oluşturma
- Sepet verisini çekme
- Stok rezervasyonu başlatma
- Ödeme success ve fail sonuçlarını işleme
- Kullanıcıya sipariş listeleme ve detay döndürme

## HTTP Uç Noktaları

### Kullanıcı Uç Noktaları

| Metot | Path | Açıklama |
| --- | --- | --- |
| `POST` | `/api/v1/orders` | Sepetten sipariş oluşturur |
| `GET` | `/api/v1/orders/{orderId}` | Tekil sipariş döner |
| `GET` | `/api/v1/orders` | Kullanıcının siparişlerini sayfalı listeler |

### İç Sistem Uç Noktaları

| Metot | Path | Açıklama |
| --- | --- | --- |
| `POST` | `/internal/orders/{orderId}/payment-success` | Siparişi ödemesi başarılı olarak işaretler |
| `POST` | `/internal/orders/{orderId}/payment-failure` | Siparişi ödemesi başarısız olarak işaretler |

## Asenkron Akış

- `OrderPaymentEventConsumer`, RabbitMQ üzerinden ödeme event’lerini dinler.
- Ödeme başarılı ise sipariş durumu ilerletilir.
- Ödeme başarısız ise rezervasyon ve sipariş durumu buna göre güncellenir.

## Entegrasyonlar

- `cart-service`: siparişe dönüşecek aktif sepet verisi
- `inventory-service`: rezervasyon açma ve stok yönetimi
- `payment-service`: ödeme akışının devamı
- `rabbitmq`: payment event tüketimi

## Ana Bileşenler

- `OrderController`
- `InternalOrderController`
- `OrderService`
- `RestOrderCartClient`
- `RestOrderInventoryClient`
- `OrderPaymentEventConsumer`

## Çalıştırma

```bash
./mvnw -f order-service/pom.xml spring-boot:run
```

Test:

```bash
./mvnw -pl order-service test
```

## Bağımlılıklar

- `config-server`
- `discovery-server`
- `postgres`
- `rabbitmq`
- `keycloak`
- `cart-service`
- `inventory-service`

## Önemli Dosyalar

- [src/main/java/com/ecommerce/microservices/order_service/order/controller/OrderController.java](src/main/java/com/ecommerce/microservices/order_service/order/controller/OrderController.java)
- [src/main/java/com/ecommerce/microservices/order_service/order/controller/InternalOrderController.java](src/main/java/com/ecommerce/microservices/order_service/order/controller/InternalOrderController.java)
- [src/main/java/com/ecommerce/microservices/order_service/order/service/OrderService.java](src/main/java/com/ecommerce/microservices/order_service/order/service/OrderService.java)
- [src/main/java/com/ecommerce/microservices/order_service/payment/consumer/OrderPaymentEventConsumer.java](src/main/java/com/ecommerce/microservices/order_service/payment/consumer/OrderPaymentEventConsumer.java)
