# Inventory Service

Stok miktarı ve rezervasyon sürecinin sahibi servistir. Sipariş akışı burada stok rezervasyonu ile başlar; ödeme sonucuna göre rezervasyon onaylanır veya serbest bırakılır.

## Port

- `8764`

## Sorumluluklar

- Ürün bazlı stok kaydı
- Stok rezervasyonu açma
- Rezervasyon onayı
- Rezervasyon serbest bırakma

## HTTP Uç Noktaları

| Metot | Path | Açıklama |
| --- | --- | --- |
| `GET` | `/api/v1/inventory/{productId}` | Ürünün stok bilgisini döner |
| `PUT` | `/api/v1/inventory/{productId}` | Ürün için stok kaydı oluşturur veya günceller |
| `POST` | `/api/v1/inventory/reservations` | Sipariş için rezervasyon açar |
| `GET` | `/api/v1/inventory/reservations/{reservationCode}` | Rezervasyon detayını döner |
| `POST` | `/api/v1/inventory/reservations/{reservationCode}/confirm` | Rezervasyonu onaylar |
| `POST` | `/api/v1/inventory/reservations/{reservationCode}/release` | Rezervasyonu serbest bırakır |

## Sipariş Akışındaki Yeri

1. `order-service`, sipariş oluştururken rezervasyon açar.
2. Ödeme başarılı olursa rezervasyon onaylanır.
3. Ödeme başarısız olursa rezervasyon serbest bırakılır.

## Veri Sahipliği

Servis aşağıdaki verilerin sahibidir:

- ürün bazlı stok miktarı
- rezervasyon kodu ve rezervasyon kalemleri

## Ana Bileşenler

- `InventoryController`
- `InventoryService`

## Çalıştırma

```bash
./mvnw -f inventory-service/pom.xml spring-boot:run
```

Test:

```bash
./mvnw -pl inventory-service test
```

## Bağımlılıklar

- `config-server`
- `discovery-server`
- `postgres`
- `keycloak`

## Önemli Dosyalar

- [src/main/java/com/ecommerce/microservices/inventory_service/inventory/controller/InventoryController.java](src/main/java/com/ecommerce/microservices/inventory_service/inventory/controller/InventoryController.java)
- [src/main/java/com/ecommerce/microservices/inventory_service/inventory/service/InventoryService.java](src/main/java/com/ecommerce/microservices/inventory_service/inventory/service/InventoryService.java)
- [src/main/resources/db/migration](src/main/resources/db/migration)
