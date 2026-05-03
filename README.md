# n11 Bootcamp E-Commerce Microservice

## Jib

Bu repodaki runnable Spring servislerinde `jib-maven-plugin` tanımlıdır. Image build almak için ilgili modülde `package` fazını da çalıştırmak gerekir.

Ortak kullanım:

```bash
./mvnw -f <service>/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
```

Örnekler:

```bash
./mvnw -f product-service/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f inventory-service/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f cart-service/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f order-service/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f payment-service/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f notification-service/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f api-gateway/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f config-server/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
./mvnw -f discovery-server/pom.xml -Dmaven.test.skip=true package jib:dockerBuild
```

Üretilen local image isimleri:

- `java-ecommerce/product-service:local`
- `java-ecommerce/inventory-service:local`
- `java-ecommerce/cart-service:local`
- `java-ecommerce/order-service:local`
- `java-ecommerce/payment-service:local`
- `java-ecommerce/notification-service:local`
- `java-ecommerce/api-gateway:local`
- `java-ecommerce/config-server:local`
- `java-ecommerce/discovery-server:local`

Varsayılan base image:

- `eclipse-temurin:21-jre-jammy`

Notlar:

- `service-common-web` ve `service-common-events` library modülleridir; doğrudan Jib image üretmezler.
- Root `pom.xml` aggregator olduğu için Jib komutunu modül `pom.xml` üstünden çalıştırmak daha nettir.

## GitHub Actions

Repo içinde iki workflow tanımlıdır:

- `.github/workflows/ci.yml`
  PR ve branch push'larında backend testlerini, frontend build'ini, Docker Compose config doğrulamasını ve Jib smoke build'ini çalıştırır.
- `.github/workflows/docker-images.yml`
  `main` push'unda ve manuel tetiklemede runnable servis image'larını GHCR'a yollar.

GHCR image isimleri şu formatta üretilir:

- `ghcr.io/<github-owner>/java-ecommerce-product-service`
- `ghcr.io/<github-owner>/java-ecommerce-inventory-service`
- `ghcr.io/<github-owner>/java-ecommerce-cart-service`
- `ghcr.io/<github-owner>/java-ecommerce-order-service`
- `ghcr.io/<github-owner>/java-ecommerce-payment-service`
- `ghcr.io/<github-owner>/java-ecommerce-notification-service`
- `ghcr.io/<github-owner>/java-ecommerce-api-gateway`
- `ghcr.io/<github-owner>/java-ecommerce-config-server`
- `ghcr.io/<github-owner>/java-ecommerce-discovery-server`

## Jenkins

Repo kökünde bir `Jenkinsfile` vardır. Pipeline şu adımları içerir:

- checkout
- backend için `./mvnw test`
- frontend için `npm ci` ve `npm run build`
- production env ile `docker compose config`
- `product-service` için Jib smoke build

Opsiyonel GHCR publish stage'i de vardır:

- sadece `main` branch'te
- `PUBLISH_IMAGES=true` parametresi verilirse
- Jenkins credentials içinde `ghcr-credentials` adında `usernamePassword` credential tanımlıysa

Beklenen agent araçları:

- Java 21
- Node.js 22
- Docker / Docker Compose
