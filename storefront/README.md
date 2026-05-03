# Storefront

React ve Vite tabanlı kullanıcı arayüzü. Katalog listeleme, kategori filtreleme, ürün detay sayfası, sepet aksiyonları, ödeme sonucu ekranı ve OIDC giriş akışı burada bulunur.

## Rol

- Ürün ve kategori verisini `api-gateway` üzerinden çeker
- Keycloak login akışını başlatır
- Sepet ve sipariş aksiyonlarını kullanıcı adına tetikler
- Ödeme sonucu sonrası kullanıcıyı bilgilendirir

## Teknoloji

- React 19
- Vite
- React Router
- `react-oidc-context`

## Route’lar

| Route | Amaç |
| --- | --- |
| `/` | Ana katalog sayfası |
| `/products/:productId` | Ürün detay ekranı |
| `/payment/result` | Ödeme sonucu ekranı |
| `*` | Bilinmeyen route’ları ana sayfaya yönlendirir |

## Veri ve Kimlik Akışı

- Katalog istekleri `VITE_API_BASE_URL` üzerinden gateway’e gider.
- Login akışı `VITE_OIDC_AUTHORITY` ile Keycloak’a yönlenir.
- Backend isteklerinde bearer token kullanılır.
- Ödeme callback sonrası kullanıcı `payment/result` ekranına geri döner.

## Önemli Konfigürasyon

- `VITE_API_BASE_URL`
- `VITE_OIDC_AUTHORITY`
- `VITE_OIDC_CLIENT_ID`

Not:

- `VITE_*` değişkenleri build anında derlenir.
- Domain veya API adresi değiştiğinde storefront image’ı yeniden build edilmelidir.

## Çalıştırma

```bash
cd storefront
npm ci
npm run dev
```

Build:

```bash
cd storefront
npm run build
```

## Bağımlılıklar

- `api-gateway`
- `keycloak`

## Önemli Dosyalar

- [src/app/App.jsx](src/app/App.jsx)
- [src/lib/config.js](src/lib/config.js)
- [src/lib/auth.js](src/lib/auth.js)
- [Dockerfile](Dockerfile)
