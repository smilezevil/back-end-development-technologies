# Лабораторна робота №10 — Resilience, idempotency, ShedLock

Три бізнес-дії з лаби 9 (ціна кімнати, вартість бронювання і кошторис у валюті гостя) тепер переживають ненадійний або вимкнений `exchange-rate-service`.
Клієнт, який повторює `POST /api/bookings` після втраченої відповіді, не створює дубль.
Запланований звіт виконується один раз, навіть коли `hotel-booking` запущено у двох екземплярах.

Попередні лабораторні роботи лежать у своїх гілках.

```
CurrencyPriceController → CurrencyPriceService
                              │  порти RateLookup, CurrencyConverter
                       ResilientRates   @Retry + @CircuitBreaker + fallback
                              │  RatesGateway
              ┌───────────────┴────────────────┐
     HttpRatesAdapter                   GrpcRatesAdapter
     rates.client=http                  rates.client=grpc
              └──────── exchange-rate-service ──┘
```

## 1. Resilience4j

### Декоратор

- `ResilientRates` реалізує ті самі порти `RateLookup` і `CurrencyConverter`, огортає `RatesGateway` (HTTP- чи gRPC-клієнт з лаби 9) і несе анотації Resilience4j. Він позначений `@Primary`, тому саме його отримує `CurrencyPriceService`.
- `CurrencyPriceService` і `CurrencyPriceController` не змінились і не знають жодного типу Resilience4j.
- `HttpRatesAdapter` і `GrpcRatesAdapter` тепер реалізують `RatesGateway` (він розширює обидва порти). Вибір між ними, як і раніше, робить `rates.client`.
- Retry огортає Circuit Breaker, тому кожна повторна спроба теж рахується у вікні вимикача.
- Анотації працюють через Spring AOP proxy: виклик приходить з іншого біна (`CurrencyPriceService`), тож проблеми self-invocation немає.

### Налаштування (`hotel-booking/src/main/resources/application.yml`)

```yaml
resilience4j:
  retry:
    instances:
      rates:
        max-attempts: 3
        wait-duration: 500ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - com.smilezevil.hotelbooking.exception.RatesUnavailableException
        ignore-exceptions:
          - com.smilezevil.hotelbooking.exception.RateNotFoundException
          - com.smilezevil.hotelbooking.exception.RatesCircuitOpenException
  circuitbreaker:
    instances:
      rates:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 6
        minimum-number-of-calls: 6
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 2
        record-exceptions:
          - com.smilezevil.hotelbooking.exception.RatesUnavailableException
        ignore-exceptions:
          - com.smilezevil.hotelbooking.exception.RateNotFoundException
```

### Поведінка

| Ситуація | Що відбувається | Відповідь |
|---|---|---|
| Валюти немає (`RateNotFoundException`) | Без повторів, вимикач цей випадок не рахує | 404 |
| B вимкнений, завис або повернув 5xx (`RatesUnavailableException`) | 3 спроби з паузами 0.5 с і 1 с, кожна рахується вимикачем | 503 |
| 6 збоїв з 6 останніх викликів | Вимикач розмикається: `CLOSED -> OPEN` | — |
| Вимикач розімкнений | `CallNotPermittedException` → fallback → `RatesCircuitOpenException`, до B запит не йде | 503 з поясненням |
| Минуло 10 с | `HALF_OPEN`, 2 пробні виклики: вдалі → `CLOSED`, ні → знову `OPEN` | 200 або 503 |

`RatesCircuitOpenException` — окремий власний виняток. Його відрізняє від `RatesUnavailableException` те, що виклик навіть не робився.

### Логи

`ResilienceEventLogger` підписаний на події `RetryRegistry` і `CircuitBreakerRegistry`:

```
RETRY rates: call failed with RatesUnavailableException, retry #1 in 500 ms
RETRY rates: call failed with RatesUnavailableException, retry #2 in 1000 ms
RETRY rates: gave up after 3 attempts, last error: Сервіс курсів валют недоступний (http): ConnectException
CIRCUIT rates: CLOSED -> OPEN
CIRCUIT OPEN: call to exchange-rate-service rejected without calling it
CIRCUIT rates: OPEN -> HALF_OPEN
CIRCUIT rates: HALF_OPEN -> CLOSED
```

Resilience4j створює бін з ім'ям `retryAspect`, і воно збігалося з аспектом `@RetryOnFailure` з лаби 4. Тому той аспект тепер має явне ім'я біна, `@Component("retryOnFailureAspect")`.

## 2. Idempotency-Key для `POST /api/bookings`

| Запит | Результат |
|---|---|
| Перший запит з ключем | Обробляється, відповідь (статус, тип, тіло) зберігається |
| Повтор з тим самим ключем від того самого користувача | Збережена відповідь із заголовком `Idempotent-Replay: true`, контролер не викликається, нового бронювання немає |
| Повтор, поки перший ще виконується | 409 Conflict |
| Той самий ключ від іншого користувача | Окремий запит |
| Перша спроба дала 5xx або впала з винятком | Ключ забувається, повтор обробляється заново |
| Запит без заголовка `Idempotency-Key` | Фільтр нічого не робить |

- `IdempotencyFilter` (`OncePerRequestFilter`) зареєстрований через `FilterRegistrationBean` лише на `/api/bookings` і стоїть після Spring Security, тому користувач уже відомий.
- Ключ у сховищі має вигляд `<користувач>:<Idempotency-Key>`.
- `IdempotencyStore` побудований на `ConcurrentHashMap.putIfAbsent`: перевірка і запис ключа — одна атомарна дія. Тому з двох одночасних запитів рівно один отримує `STARTED`.
- Відповідь контролера перехоплює `ContentCachingResponseWrapper`.
- Обмеження: сховище в пам'яті одного екземпляра, без терміну життя. У реальній системі ключі тримають у базі або Redis з TTL.

## 3. ShedLock

- `BookingStatsReportJob` — `@Scheduled` кожні 10 с, `@SchedulerLock(name = "bookingStatsReport")`. Звіт пише в лог кількість бронювань, порт і PID екземпляра.
- `SchedulingConfig`: `@EnableScheduling`, `@EnableSchedulerLock`, `JdbcTemplateLockProvider` з `usingDbTime()`.
- Таблицю `shedlock` створює Liquibase: `db/changelog/migrations/003-shedlock-schema.xml`, changeset `006`.
- Екземпляр, що першим узяв блокування, виконує звіт. Інші пропускають запуск, і в лозі (`DEBUG`) пишуть `Not executing 'bookingStatsReport'. It's locked.`

```yaml
reports:
  booking-stats:
    interval: PT10S
    lock-at-most-for: PT30S
    lock-at-least-for: PT9S

logging:
  level:
    net.javacrumbs.shedlock: DEBUG
```

`lock-at-most-for` звільняє блокування, якщо екземпляр упав. `lock-at-least-for` не дає другому екземпляру повторити звіт у межах того самого 10-секундного інтервалу.

## Відповідність завданню

| № | Вимога | Де реалізовано |
|---|---|---|
| 1a | Декоратор порту з анотаціями Resilience4j, сервіси без змін | `rates/ResilientRates`, `rates/RatesGateway` |
| 1b | Retry лише для тимчасових помилок | `@Retry`, `retry-exceptions` / `ignore-exceptions` |
| 1c | Circuit breaker: розмикається, відхиляє, пробні виклики, «не знайдено» не збій | `@CircuitBreaker`, `resilience4j.circuitbreaker.instances.rates` |
| 1d | Fallback → власний виняток → 503 з поясненням | `rateCircuitOpen` / `convertCircuitOpen`, `RatesCircuitOpenException`, `GlobalExceptionHandler` |
| 1e | Усі числа в YAML | блок `resilience4j` в `application.yml` |
| 1f | Лог на кожен повтор і зміну стану | `rates/ResilienceEventLogger` |
| 2a | POST з заголовком `Idempotency-Key` | `POST /api/bookings`, `idempotency/IdempotencyConfig` |
| 2b | Повтор отримує збережену відповідь, без дубля | `IdempotencyFilter`, `IdempotencyStore` |
| 2c | Ключ прив'язаний до користувача, 5xx не зберігається, без ключа як раніше | `IdempotencyFilter.owner`, `process`, `shouldNotFilter` |
| 3a | `@Scheduled`-метод | `job/BookingStatsReportJob` |
| 3b | ShedLock з блокуванням у базі | `config/SchedulingConfig`, таблиця `shedlock` |
| 3c | Два екземпляри, звіт виконує один | запуск на портах 8080 і 8083, логи і `select * from shedlock` |

## Запуск

Потрібно: Docker Desktop, JDK 21, PostgreSQL (`hotel_booking`) і Keycloak на порту 9090.

```
./mvnw -DskipTests package
docker compose up -d --build
java -jar hotel-booking/target/hotel-booking-1.0.0-SNAPSHOT.jar
```

**Resilience4j.** Вимкнути B, двічі викликати `GET /api/rooms/1/price?currency=EUR` і подивитися лог `hotel-booking`:

```
docker compose stop exchange-rate-service
docker compose start exchange-rate-service
```

Щоб устигнути показати, що розімкнений вимикач відхиляє запити навіть до вже запущеного B, час розімкнення можна подовжити при старті:

```
java -jar hotel-booking/target/hotel-booking-1.0.0-SNAPSHOT.jar --resilience4j.circuitbreaker.instances.rates.wait-duration-in-open-state=30s
```

**Idempotency.** Розділ «10. IDEMPOTENCY» у `hotel-booking/hotel-api.http`: 10.1 і 10.2 з одним ключем дають одне бронювання, 10.3 без ключа створює нове.

**ShedLock.** Два екземпляри в різних вікнах. Другий запускається без Quartz з лаби 6, бо той налаштований на один екземпляр:

```
java -jar hotel-booking/target/hotel-booking-1.0.0-SNAPSHOT.jar
java -jar hotel-booking/target/hotel-booking-1.0.0-SNAPSHOT.jar --server.port=8083 --spring.quartz.auto-startup=false
```

Вміст таблиці блокувань:

```
$(brew --prefix postgresql@16)/bin/psql -d hotel_booking -c "select * from shedlock;"
```

## Тести

```
./mvnw verify
```

| Тест | Що перевіряє |
|---|---|
| `ResilientRatesTest` | Справжні Resilience4j і AOP з налаштуваннями з YAML: «не знайдено» без повторів, тимчасовий збій повторюється до успіху, 6 збоїв розмикають вимикач, і наступний виклик не доходить до клієнта, після паузи `OPEN -> HALF_OPEN -> CLOSED`, записи в лозі |
| `CurrencyPriceControllerTest` | Серед іншого: розімкнений вимикач дає 503 з поясненням |
| `IdempotencyStoreTest` | Стани `STARTED` / `IN_PROGRESS` / `DONE`, забування ключа, 20 одночасних потоків з одним ключем дають рівно один `STARTED` |
| `IdempotencyFilterTest` | Повтор віддає збережену відповідь з `Idempotent-Replay`, ключі різних користувачів окремі, 5xx і винятки не зберігаються, без ключа все як раніше, повтор під час обробки дає 409 |
| `BookingStatsReportJobTest` | Справжній ShedLock: при вільному блокуванні звіт виконується з налаштуваннями з конфігурації, при зайнятому пропускається |

Тести лаб 2–9 також проходять.