# service-common-events

Standalone servis değildir. Payment event envelope, topology sabitleri ve payload contract’larını merkezi tutan paylaşılan kütüphanedir.

## İçerik

- `EventEnvelope`
- `PaymentEventTopology`
- `PaymentEventTypes`
- `PaymentSucceededEventPayload`
- `PaymentFailedEventPayload`

## Nerede Kullanılır

- `payment-service`: event üretimi
- `order-service`: event tüketimi
- `notification-service`: event tüketimi

## Sağladığı Fayda

- Event adlarının servisler arasında tutarlı kalması
- JSON payload sözleşmesinin tek yerde tanımlanması
- Producer ve consumer tarafında sürüklenen kopya DTO riskinin azalması

## Build

```bash
./mvnw -pl service-common-events test
```

## Not

Bu modül image üretmez; kütüphane olarak kullanılır.
