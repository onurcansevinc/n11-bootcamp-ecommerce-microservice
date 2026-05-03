# Payment Service

Ödeme kaydını oluşturur, Iyzico ile konuşur, callback veya simulate sonucu ile ödeme durumunu günceller ve sonucu outbox üzerinden event olarak yayınlar.

## Port

- `8767`

## Sorumluluklar

- Ödeme kaydı oluşturma
- Iyzico checkout entegrasyonu
- Sandbox simulate success ve failure akışları
- Callback sonucu işleme
- Outbox tablosuna event yazma
- RabbitMQ’ya payment event publish etme

## HTTP Uç Noktaları

| Metot | Path | Açıklama |
| --- | --- | --- |
| `POST` | `/api/v1/payments` | Sipariş için ödeme oluşturur |
| `GET` | `/api/v1/payments/{paymentId}` | Tekil ödeme döner |
| `GET` | `/api/v1/payments` | Kullanıcının ödemelerini listeler |
| `POST` | `/api/v1/payments/{paymentId}/simulate-success` | Sandbox’ta başarılı ödeme simülasyonu yapar |
| `POST` | `/api/v1/payments/{paymentId}/simulate-failure` | Sandbox’ta başarısız ödeme simülasyonu yapar |
| `POST` | `/api/v1/payments/iyzico/callback` | Iyzico callback’ini işler ve frontend’e redirect eder |

## Event Akışı

1. Ödeme sonucu önce ödeme kaydına işlenir.
2. Sonuç outbox tablosuna yazılır.
3. `PaymentOutboxPublisher`, bekleyen event’leri periyodik olarak yayınlar.
4. `order-service` ve `notification-service` aynı event’i tüketir.

## Zamanlanmış İş

- `PaymentOutboxPublisher`
- Varsayılan yayın aralığı: `payment.outbox.publish.fixed-delay=5000`

## Entegrasyonlar

- `order-service`: ödeme oluştururken sipariş doğrulama ve iç callback
- `rabbitmq`: payment event publish
- `iyzico`: harici ödeme sağlayıcısı

## Ana Bileşenler

- `PaymentController`
- `PaymentService`
- `RestPaymentOrderClient`
- `PaymentOutboxService`
- `PaymentOutboxPublisher`

## Çalıştırma

```bash
./mvnw -f payment-service/pom.xml spring-boot:run
```

Test:

```bash
./mvnw -pl payment-service test
```

## Bağımlılıklar

- `config-server`
- `discovery-server`
- `postgres`
- `rabbitmq`
- `keycloak`
- `order-service`
- `iyzico`

## Önemli Dosyalar

- [src/main/java/com/ecommerce/microservices/payment_service/payment/controller/PaymentController.java](src/main/java/com/ecommerce/microservices/payment_service/payment/controller/PaymentController.java)
- [src/main/java/com/ecommerce/microservices/payment_service/payment/service/PaymentService.java](src/main/java/com/ecommerce/microservices/payment_service/payment/service/PaymentService.java)
- [src/main/java/com/ecommerce/microservices/payment_service/outbox/service/PaymentOutboxPublisher.java](src/main/java/com/ecommerce/microservices/payment_service/outbox/service/PaymentOutboxPublisher.java)
