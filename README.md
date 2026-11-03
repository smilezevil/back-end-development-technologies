# Лабораторна робота №8 — Message brokers (Kafka)

Два сервіси платформи **hotel-platform** взаємодіють через брокер повідомлень Apache Kafka.
Відправник не чекає на отримувача, і жодна подія не губиться, навіть якщо отримувач тимчасово недоступний.
Попередні лабораторні роботи лежать у своїх гілках (`lab2` … `lab7`).

```
hotel-booking (producer, :8080)  ──►  Kafka, topic hotel.booking-events  ──►  notification-service (consumer, :8082)
   запускається з IDE                     compose.yml, 3 partitions               запускається в Docker
   localhost:9094                          kafka:9092 / localhost:9094            kafka:9092
```

Прямих HTTP-викликів між `hotel-booking` і `notification-service` немає.
Спільний у них лише модуль `common` з контрактом події (`BookingEvent`, `BookingEventType`, `BookingTopics`).

## Бізнес-дії

| Бізнес-дія | Endpoint у `hotel-booking` | Подія | Результат у `notification-service` |
|---|---|---|---|
| Створення бронювання | `POST /api/bookings` | `CREATED` | `Booking #N confirmed: <дати>` у `GET /notifications` |
| Зміна бронювання | `PUT /api/bookings/{id}` | `UPDATED` | `Booking #N changed: <дати>` |
| Скасування бронювання | `DELETE /api/bookings/{id}` | `CANCELLED` | `Booking #N cancelled` |

Усі події йдуть в один topic `hotel.booking-events` з ключем `bookingId`,
тому події одного бронювання потрапляють в один partition і зберігають порядок.

## Як це працює

**Producer (`hotel-booking`):**

- `BookingService` (`@Transactional`) після успішної дії публікує Spring-подію `BookingEvent` через `ApplicationEventPublisher` і нічого не знає про Kafka.
- `BookingEventKafkaPublisher` слухає її через `@TransactionalEventListener(phase = AFTER_COMMIT)` і надсилає в Kafka **лише після commit**. Якщо дія впала або транзакцію відкотили, подія не надсилається.
- `acks: all`: повідомлення вважається відправленим, коли брокер його прийняв. `max.block.ms: 5000`: якщо брокер недоступний, запит не висить довше 5 секунд.

**Consumer (`notification-service`):**

- `BookingEventListener` — `@KafkaListener` у групі `notification-service`. Offset фіксується після успішної обробки.
- `ErrorHandlingDeserializer` + `JsonDeserializer`: тип береться з конфігурації (`BookingEvent`), биткі повідомлення не блокують partition.
- Ідемпотентність: `NotificationService.notifyAbout` пам'ятає оброблені `eventId`, тож повторна доставка тієї самої події нічого не змінює.
- `auto-offset-reset: earliest`: нова група читає topic з початку, тому події, надіслані до першого запуску consumer'а, теж обробляються.

**Адреса брокера** в обох сервісах: `${KAFKA_BOOTSTRAP_SERVERS:localhost:9094}`.
З IDE використовується `localhost:9094`, а в Docker (`compose.yml`) — `KAFKA_BOOTSTRAP_SERVERS=kafka:9092`, тобто ім'я сервісу з compose.

## Відповідність завданню

| № | Вимога | Де реалізовано |
|---|---|---|
| 1 | Брокер описаний у `compose.yml` | `compose.yml`: `apache/kafka:3.9.0` у режимі KRaft, listeners `INTERNAL://kafka:9092` і `EXTERNAL://localhost:9094`, healthcheck |
| 2 | Повідомлення передаються через брокер, сервіси не використовують класи один одного | `hotel-booking` → topic `hotel.booking-events` → `notification-service`; спільні лише DTO з `common.event` |
| 3 | Якщо отримувач не працює, дані не втрачаються | Kafka зберігає повідомлення, а група `notification-service` після запуску продовжує зі свого committed offset |

## Сценарії → як перевірити

| Сценарій | Перевірка |
|---|---|
| 1. Одна дія → одна подія → помітний результат | `hotel-api.http` 8.1–8.3 → у лозі `PUBLISHED` / `RECEIVED` → `GET http://localhost:8082/notifications` (8.5) |
| 2. Дія з помилкою не публікує подію; публікація після commit | Запит 8.4 (неіснуюча кімната) → немає `PUBLISHING`; тести `BookingEventAfterCommitTest` і `BookingServiceTest.*_throwsAndPublishesNothing` |
| 3. Немає HTTP між сервісами, consumer не залежить від класів producer'а | `notification-service/pom.xml` залежить лише від `common`; `dependency:tree -Dincludes=com.smilezevil` |
| 4. Consumer зупинено → producer відповідає успішно → після старту обидві події оброблено | `docker compose stop notification-service`, двічі 8.1 (`201`), `kafka-consumer-groups --describe` показує `LAG 2`, `docker compose start notification-service` → `RECEIVED` ×2, `LAG 0` |
| 5. Повторна подія не змінює результат (unit-тест) | `BookingEventListenerTest.onBookingEvent_sameEventDeliveredTwice_resultStaysAsAfterFirstDelivery` |
| 6. Адреса брокера зі змінної середовища; у Docker — ім'я сервісу | `application.yml` обох сервісів, `compose.yml`; у лозі consumer'а `bootstrap.servers = [kafka:9092]` |

## Запуск

Потрібно: Docker Desktop, JDK 21, а для `hotel-booking` ще PostgreSQL (`hotel_booking`) і Keycloak на порту 9090.

```
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw -DskipTests package
docker compose up -d --build
docker compose ps
```

Після цього `hotel-booking` запускається з IDE (`HotelBookingApplication`), а запити лежать у розділі «8. KAFKA» файлу `hotel-booking/hotel-api.http`.

Переглянути topic і consumer group:

```
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic hotel.booking-events --from-beginning --property print.key=true --property print.partition=true --timeout-ms 10000
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group notification-service
```

## Тести

```
./mvnw verify
```

| Тест | Що перевіряє |
|---|---|
| `BookingServiceTest` | Події `CREATED` / `UPDATED` / `CANCELLED` публікуються після успішних дій, а при помилках не публікуються |
| `BookingEventKafkaPublisherTest` | Подія йде в `hotel.booking-events` з ключем `bookingId` |
| `BookingEventAfterCommitTest` | Справжній `@TransactionalEventListener`: відправка лише після commit, нічого при rollback і без транзакції |
| `BookingEventListenerTest` | Повторна подія не змінює результат; три дії дають три сповіщення; невалідна адреса записується як `REJECTED` |
| `NotificationServiceApplicationTest` | Контекст сервісу піднімається з вимкненим Kafka-listener'ом, щоб тест не «з'їдав» реальні події |