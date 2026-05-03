# Notification Service

Payment event’lerini RabbitMQ’dan tüketir ve bunun sonucunda e-posta veya SMS bildirimi üretir. Kullanıcı tarafına doğrudan HTTP API sunmak yerine asenkron arka plan işçisi gibi çalışır.

## Port

- `8768`

## Sorumluluklar

- `payment.succeeded.v1` event’ini işleme
- `payment.failed.v1` event’ini işleme
- Bildirim kaydı oluşturma
- E-posta gönderme
- SMS gönderme
- Idempotent event işleme

## Tüketilen Event’ler

| Event | Amaç |
| --- | --- |
| `payment.succeeded.v1` | Başarılı ödeme sonrası kullanıcıya bilgilendirme |
| `payment.failed.v1` | Başarısız ödeme sonrası kullanıcıya bilgilendirme |

Dinlenen kuyruk:

- `${notification.payment.events.queue:notification.payment-events.v1}`

## Çalışma Şekli

1. RabbitMQ’dan event alınır.
2. Event tipi `PaymentSucceededEventPayload` veya `PaymentFailedEventPayload` olarak ayrıştırılır.
3. Bildirim kaydı oluşturulur.
4. SMTP veya SMS kanalı üzerinden gönderim denenir.

## Ana Bileşenler

- `NotificationPaymentEventConsumer`
- `NotificationService`

## Çalıştırma

```bash
./mvnw -f notification-service/pom.xml spring-boot:run
```

Test:

```bash
./mvnw -pl notification-service test
```

## Bağımlılıklar

- `config-server`
- `discovery-server`
- `postgres`
- `rabbitmq`
- `mailhog`

## Önemli Dosyalar

- [src/main/java/com/ecommerce/microservices/notification_service/notification/consumer/NotificationPaymentEventConsumer.java](src/main/java/com/ecommerce/microservices/notification_service/notification/consumer/NotificationPaymentEventConsumer.java)
- [src/main/java/com/ecommerce/microservices/notification_service/notification/service/NotificationService.java](src/main/java/com/ecommerce/microservices/notification_service/notification/service/NotificationService.java)
