# Product Service

Ürün ve kategori kataloğunun sahibi servistir. Listeleme, detay, yönetim, filtreleme ve kampanya etiketi bilgileri burada tutulur.

## Port

- `8763`

## Sorumluluklar

- Ürün CRUD
- Kategori CRUD
- Sayfalama ve filtreleme
- `campaignLabel` ve görsel bilgisi
- Redis cache desteği

## HTTP Uç Noktaları

### Ürünler

| Metot | Path | Açıklama |
| --- | --- | --- |
| `GET` | `/api/v1/products` | Ürünleri sayfalı ve filtreli listeler |
| `GET` | `/api/v1/products/{productId}` | Tekil ürün detayını döner |
| `POST` | `/api/v1/products` | Yeni ürün oluşturur |
| `PUT` | `/api/v1/products/{productId}` | Ürünü tamamen günceller |
| `PATCH` | `/api/v1/products/{productId}` | Kısmi ürün güncellemesi yapar |
| `DELETE` | `/api/v1/products/{productId}` | Ürünü siler |

### Kategoriler

| Metot | Path | Açıklama |
| --- | --- | --- |
| `GET` | `/api/v1/categories` | Kategorileri sayfalı listeler |
| `GET` | `/api/v1/categories/{categoryId}` | Tekil kategori döner |
| `POST` | `/api/v1/categories` | Yeni kategori oluşturur |
| `PUT` | `/api/v1/categories/{categoryId}` | Kategoriyi tamamen günceller |
| `PATCH` | `/api/v1/categories/{categoryId}` | Kısmi kategori güncellemesi yapar |
| `DELETE` | `/api/v1/categories/{categoryId}` | Kategoriyi siler |

## Filtreleme Kabiliyeti

`GET /api/v1/products` için öne çıkan parametreler:

- `page`
- `size`
- `search`
- `active`
- `categoryId`
- `minPrice`
- `maxPrice`

## Veri Sahipliği

Servis aşağıdaki katalog verilerinin sahibidir:

- kategoriler
- ürünler
- ürün görselleri

Demo seed migration’ları bu serviste yer alır.

## Ana Bileşenler

- `ProductController`
- `CategoryController`
- `ProductService`
- `CategoryService`
- `RedisCacheConfig`

## Çalıştırma

```bash
./mvnw -f product-service/pom.xml spring-boot:run
```

Test:

```bash
./mvnw -pl product-service test
```

## Bağımlılıklar

- `config-server`
- `discovery-server`
- `postgres`
- `redis`
- `keycloak`

## Önemli Dosyalar

- [src/main/java/com/ecommerce/microservices/product_service/product/controller/ProductController.java](src/main/java/com/ecommerce/microservices/product_service/product/controller/ProductController.java)
- [src/main/java/com/ecommerce/microservices/product_service/category/controller/CategoryController.java](src/main/java/com/ecommerce/microservices/product_service/category/controller/CategoryController.java)
- [src/main/java/com/ecommerce/microservices/product_service/common/config/RedisCacheConfig.java](src/main/java/com/ecommerce/microservices/product_service/common/config/RedisCacheConfig.java)
- [src/main/resources/db/migration](src/main/resources/db/migration)
