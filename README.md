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
