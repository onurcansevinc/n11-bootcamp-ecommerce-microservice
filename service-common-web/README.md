# service-common-web

Standalone servis değildir. Tekrarlanan HTTP, güvenlik ve response altyapısını merkezi tutan paylaşılan kütüphanedir.

## İçerik

- `ClientCredentialsAccessTokenProvider`
- `CommonServletSecurityConfiguration`
- ortak `ApiResponse` ve `ResponseMeta` yapıları
- mevcut kullanıcı kimliği çözümleme yardımcıları

## Nerede Kullanılır

- Servisler arası client credentials token alma
- Ortak servlet güvenlik davranışı
- Standart API cevap gövdesi üretimi

## Neden Ayrı Modül

- Aynı güvenlik kurallarını her serviste tekrar etmemek
- Ortak DTO ve yardımcı sınıfları tek yerden yönetmek
- Yeni servislere aynı temel davranışı hızlıca taşımak

## Build

```bash
./mvnw -pl service-common-web test
```

## Not

Bu modül image üretmez; kütüphane olarak kullanılır.
