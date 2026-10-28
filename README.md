# Лабораторна робота №7 — Multi-module Maven

Проєкт **hotel-platform**: один репозиторій і одна збірка, яка охоплює кілька незалежних сервісів.
Кожен сервіс можна окремо зібрати, протестувати, запустити й розгорнути.
Spring Boot 3.3.5, Java 21. Попередні лабораторні роботи лежать у своїх гілках (`lab2` … `lab6`).

## Структура

```
back-end-development-technologies/      кореневий POM hotel-platform (packaging pom)
├── pom.xml
├── mvnw, .mvn/
├── common/                  лише спільні DTO (records), без main і без Spring Boot plugin
├── hotel-booking/           застосунок із лаб 1–6
├── exchange-rate-service/   курси гривні та конвертація
└── notification-service/    сповіщення гостям, virtual threads
```

| Модуль | Тип | Порт | Main-клас | Призначення |
|---|---|---|---|---|
| `common` | бібліотека (jar) | — | — | DTO: `ApiError`, `ExchangeRateResponse`, `ConversionResponse`, `NotificationRequest`, `NotificationReceipt` |
| `hotel-booking` | сервіс | 8080 | `HotelBookingApplication` | Готелі, кімнати, гості, бронювання, Keycloak, Quartz |
| `exchange-rate-service` | сервіс | 8081 | `ExchangeRateServiceApplication` | `GET /rates/{currency}`, `GET /rates/{currency}/convert?amountUah=` |
| `notification-service` | сервіс | 8082 | `NotificationServiceApplication` | `POST /notifications`, `GET /notifications` |

Кожен сервіс має власні `main`, `application.yml`, порт, тести, `Dockerfile` і `.dockerignore`.

## Відповідність сценаріям

| Сценарій | Як виконано |
|---|---|
| 1. Кореневий POM з `packaging pom`; у `<modules>` усі модулі; `<parent>` кожного модуля вказує на кореневий POM; `mvn verify` з кореня зелений | `pom.xml` у корені; у кожному модулі `<parent>` = `com.smilezevil:hotel-platform` з `relativePath ../pom.xml`; `./mvnw verify` → `BUILD SUCCESS` для 5 модулів |
| 2. Спільна залежність оголошена в кореневому `<dependencies>`, а не в модулях | `spring-boot-starter-test` (scope `test`) лише в кореневому POM; у POM модулів його немає |
| 3. Кожен сервіс має свій main-клас, свій `application.yml` і свій порт; Spring Boot plugin у власному POM | Порти 8080 / 8081 / 8082; `spring-boot-maven-plugin` у `pom.xml` кожного сервісу; у кореневому POM і в `common` його немає |
| 4. Жоден сервіс не залежить від іншого; спільний лише модуль `common` із DTO | `dependency:tree`: `hotel-booking` не має наших залежностей, `exchange-rate-service` і `notification-service` залежать тільки від `common` |

## Збірка і тести

Збирати потрібно з JDK 21, бо Lombok у Spring Boot 3.3.5 не підтримує компілятор JDK 25:

```
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

Усі модулі разом:

```
./mvnw verify
```

Лише один сервіс (разом із `common`, від якого він залежить):

```
./mvnw -pl notification-service -am verify
```

Перевірка, що сервіси не залежать один від одного:

```
./mvnw -DskipTests package dependency:tree -Dincludes=com.smilezevil
```

## Запуск

Кожен сервіс запускається окремо, з IntelliJ або як fat jar:

```
java -jar exchange-rate-service/target/exchange-rate-service-1.0.0-SNAPSHOT.jar
java -jar notification-service/target/notification-service-1.0.0-SNAPSHOT.jar
```

`exchange-rate-service` і `notification-service` не потребують бази даних чи Keycloak.
`hotel-booking` потребує PostgreSQL (`hotel_booking`) і Keycloak на порту 9090.

Запити до нових сервісів лежать у `services-api.http`, до `hotel-booking` — у `hotel-booking/hotel-api.http`.

## Docker

```
./mvnw -DskipTests package
docker build -t notification-service ./notification-service
docker run --rm -p 8082:8082 notification-service
```

`Dockerfile` копіює вже зібраний jar в образ `eclipse-temurin:21-jre` і запускає його не від root.
`.dockerignore` пропускає в контекст збірки лише `target/*.jar`.

## Virtual threads

У `notification-service` увімкнено `spring.threads.virtual.enabled: true`:

- Tomcat обробляє кожен запит у virtual thread (у лозі: `handled on VirtualThread[...]`);
- `applicationTaskExecutor` стає `SimpleAsyncTaskExecutor` на virtual threads.

Тест `NotificationServiceApplicationTest.applicationTaskExecutor_virtualThreadsEnabled_runsTasksOnVirtualThreads` це перевіряє.