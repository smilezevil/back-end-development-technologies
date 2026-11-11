# Лабораторна робота №9 — Interservice communication (HTTP + gRPC)

`hotel-booking` (сервіс A) отримує курси валют від `exchange-rate-service` (сервіс B).
B надає ті самі дані двома способами: через REST і через gRPC.
A звертається до B лише через власні інтерфейси-порти, а спосіб виклику (HTTP чи gRPC) вибирається властивістю `rates.client`.
Повільний або недоступний B не підвішує A: виклики обмежені таймаутами і deadline, а помилки перетворюються на відповіді 404 і 503.

Попередні лабораторні роботи лежать у своїх гілках.

```
hotel-booking (A, :8080)
  CurrencyPriceController → CurrencyPriceService
                              │  порти RateLookup, CurrencyConverter
              ┌───────────────┴────────────────┐
     HttpRatesAdapter                   GrpcRatesAdapter
     rates.client=http                  rates.client=grpc
              │                                │
   REST http://localhost:8081         gRPC localhost:9081
              └──────── exchange-rate-service (B) ───────┘
          контракт: exchange-rate-api/src/main/proto/exchange_rates.proto
```

## Модулі

| Модуль | Роль |
|---|---|
| `common` | Спільні DTO (`ExchangeRateResponse`, `ConversionResponse`) і контракт Kafka-подій |
| `exchange-rate-api` | Модуль контрактів: `exchange_rates.proto`, Java-класи генеруються під час збірки |
| `exchange-rate-service` | Сервіс B: курси валют через REST (`:8081`) і gRPC (`:9081`) |
| `hotel-booking` | Сервіс A: готелі, бронювання і три бізнес-дії з курсами валют |
| `notification-service` | Споживач Kafka-подій з лаби 8 |

## Бізнес-дії

| Бізнес-дія | Endpoint у `hotel-booking` | Порт | Доступ |
|---|---|---|---|
| Ціна кімнати за ніч у валюті гостя | `GET /api/rooms/{id}/price?currency=EUR` | `CurrencyConverter` | без токена |
| Вартість бронювання у валюті гостя | `GET /api/bookings/{id}/price?currency=USD` | `CurrencyConverter` | з токеном |
| Кошторис проживання на N ночей | `GET /api/rooms/{id}/quote?nights=3&currency=PLN` | `RateLookup` | без токена |

У кожній відповіді є поле `via` (`http` або `grpc`), яке показує, через який адаптер A звернувся до B.

## Як це працює

### Контракт gRPC (`exchange-rate-api`)

- `exchange_rates.proto` описує сервіс `ExchangeRates` з методами `GetRate` і `Convert` та чотири повідомлення.
- Суми передаються рядками, щоб не втратити точність `BigDecimal`.
- `protobuf-maven-plugin` разом з `os-maven-plugin` генерує класи під час `./mvnw compile` у `target/generated-sources/protobuf`. Папка `target/` у `.gitignore`, тому згенерований код у репозиторій не потрапляє.
- Від цього модуля залежать обидва сервіси.

### Сервер (`exchange-rate-service`)

- `ExchangeRatesGrpcService` реалізує `ExchangeRates` поверх того самого `ExchangeRateService`, що й REST-контролер. Невідома валюта повертається як статус `NOT_FOUND`, від'ємна сума як `INVALID_ARGUMENT`.
- `GrpcServerLifecycle` (`SmartLifecycle`) запускає gRPC-сервер разом із застосунком і зупиняє разом із ним.
- Сервер вимкнений за замовчуванням (`grpc.server.enabled: false`). Вмикається властивістю, а в Docker змінною `GRPC_SERVER_ENABLED=true`. Порт береться з `grpc.server.port` (9081).
- `ProtoReflectionService` дозволяє викликати сервер через `grpcurl` без `.proto`-файлу.

### Клієнт (`hotel-booking`)

- **Порти** `RateLookup` і `CurrencyConverter` — власні інтерфейси, від яких залежить `CurrencyPriceService`. Сервіс не знає, як саме йде виклик.
- **HTTP-адаптер:** `ExchangeRateHttpClient` — декларативний HTTP interface client Spring (`@HttpExchange`, `@GetExchange`). Реалізацію створює `HttpServiceProxyFactory` поверх `RestClient` з `JdkClientHttpRequestFactory`, де явно задані connect-timeout і read-timeout. Spring Cloud OpenFeign не використовується: його версія прив'язана до версії Spring Boot, а HTTP interface вбудований у Spring 6.
- **gRPC-адаптер:** `ManagedChannel` створюється з адресою з конфігурації, а `GrpcRatesAdapter` перед кожним викликом blocking stub ставить `withDeadlineAfter(...)`.
- Обидва адаптери реалізують обидва порти.

### Вибір адаптера

`HttpRatesConfig` активна лише при `rates.client=http`, `GrpcRatesConfig` лише при `rates.client=grpc` (`@ConditionalOnProperty`).
За будь-якого іншого значення не створюється жоден адаптер, `CurrencyPriceService` не отримує своїх портів, і застосунок зупиняється на старті з `APPLICATION FAILED TO START`.

### Конфігурація (`hotel-booking/src/main/resources/application.yml`)

```yaml
rates:
  client: ${RATES_CLIENT:http}
  rest:
    url: ${RATES_REST_URL:http://localhost:8081}
    connect-timeout: 1s
    read-timeout: 2s
  grpc:
    target: ${RATES_GRPC_TARGET:localhost:9081}
    deadline: 2s
```

Значення після двокрапки — адреси для локального запуску. У Docker їх перевизначають змінні оточення, наприклад `RATES_REST_URL=http://exchange-rate-service:8081` і `RATES_GRPC_TARGET=exchange-rate-service:9081`.
`RatesProperties` (`@ConfigurationProperties` + `@Validated`) не дає застосунку стартувати без адрес і таймаутів.

### Помилки

| Ситуація | HTTP-адаптер | gRPC-адаптер | Виняток | Відповідь |
|---|---|---|---|---|
| Валюти немає | 404 від B | `NOT_FOUND` | `RateNotFoundException` | 404 |
| B не відповідає вчасно | read-timeout (`HttpTimeoutException`) | `DEADLINE_EXCEEDED` | `RatesUnavailableException` | 503 |
| B вимкнений | `ConnectException` | `UNAVAILABLE` | `RatesUnavailableException` | 503 |
| B повернув помилку | 5xx | будь-який інший статус | `RatesUnavailableException` | 503 |

Перетворення робить метод `call(...)` у кожному адаптері, а `GlobalExceptionHandler` віддає 404 і 503 з полем `message`.
`RestClientException` і `StatusRuntimeException` за межі адаптерів не виходять.
Неіснуюча кімната чи бронювання дають 404 (`ResourceNotFoundException`), неправильна кількість ночей дає 400.

## Відповідність завданню

| № | Вимога | Де реалізовано |
|---|---|---|
| 1 | Декларативний HTTP-клієнт | `rates/http/ExchangeRateHttpClient` (`@HttpExchange`), `HttpRatesConfig` |
| 2 | 2–3 власні порти | `rates/RateLookup`, `rates/CurrencyConverter`, їх використовує `CurrencyPriceService` |
| 3 | Один `.proto` у модулі контрактів, класи генеруються під час збірки | `exchange-rate-api/src/main/proto/exchange_rates.proto`, `protobuf-maven-plugin` |
| 4 | gRPC-сервер у B: живе разом із застосунком, вимкнений за замовчуванням, порт з налаштувань, `NOT_FOUND` | `GrpcServerLifecycle`, `ExchangeRatesGrpcService`, `grpc.server.*` |
| 5 | gRPC-клієнт як другий адаптер, канал з конфігурації, deadline на кожен виклик | `rates/grpc/GrpcRatesConfig`, `GrpcRatesAdapter` |
| 6 | Вибір адаптера властивістю, невідоме значення зупиняє старт | `rates.client` + `@ConditionalOnProperty`, `RatesClientConfigTest` |
| 7 | Адреси B як властивості з дефолтами і перевизначенням через env | `rates.rest.url`, `rates.grpc.target`, `compose.yml` (`GRPC_SERVER_ENABLED`) |
| 8 | Явні таймаути і deadline у конфігурації | `rates.rest.connect-timeout`, `rates.rest.read-timeout`, `rates.grpc.deadline` |
| 9 | Уніфіковані помилки → 404 / 503 | `RateNotFoundException`, `RatesUnavailableException`, `GlobalExceptionHandler` |

## Запуск

Потрібно: Docker Desktop, JDK 21, PostgreSQL (`hotel_booking`) і Keycloak на порту 9090.

```
./mvnw -DskipTests package
docker compose up -d --build
docker compose ps
```

`exchange-rate-service` у Docker стартує з увімкненим gRPC: у лозі з'являється `gRPC server started on port 9081`.

`hotel-booking` у режимі http (за замовчуванням):

```
java -jar hotel-booking/target/hotel-booking-1.0.0-SNAPSHOT.jar
```

`hotel-booking` у режимі gRPC:

```
RATES_CLIENT=grpc java -jar hotel-booking/target/hotel-booking-1.0.0-SNAPSHOT.jar
```

Запити лежать у розділі «9. INTERSERVICE» файлу `hotel-booking/hotel-api.http`. Перевірити B напряму:

```
curl http://localhost:8081/rates/EUR
grpcurl -plaintext -d '{"currency":"EUR"}' localhost:9081 hotelplatform.exchangerate.ExchangeRates/GetRate
grpcurl -plaintext -d '{"currency":"XYZ"}' localhost:9081 hotelplatform.exchangerate.ExchangeRates/GetRate
```

## Сценарії → як перевірити

| Сценарій | Перевірка | Очікування |
|---|---|---|
| Три дії через HTTP | запуск без змінних, запити 9.1, 9.2, 9.4 | `"via": "http"`, у лозі B `Conversion requested` |
| Три дії через gRPC | `RATES_CLIENT=grpc`, ті самі запити | `"via": "grpc"`, у лозі B `gRPC conversion requested` |
| Невідома валюта | запит 9.5 | 404 в обох режимах |
| B завис | `docker compose pause exchange-rate-service`, запит 9.1 | 503 приблизно через 2 с: `HttpTimeoutException` або `DEADLINE_EXCEEDED` |
| B вимкнений | `docker compose stop exchange-rate-service`, запит 9.1 | 503 одразу: `ConnectException` або `UNAVAILABLE` |
| Невідомий адаптер | `RATES_CLIENT=soap` | `APPLICATION FAILED TO START` |

Повернути B після перевірки: `docker compose unpause exchange-rate-service` або `docker compose start exchange-rate-service`.

## Тести

```
./mvnw verify
```

| Тест | Що перевіряє |
|---|---|
| `ExchangeRatesGrpcServiceTest` | gRPC-сервер B через in-process сервер: курс, конвертація, `NOT_FOUND`, `INVALID_ARGUMENT` |
| `GrpcServerLifecycleTest` | Сервер вимкнений за замовчуванням; при `enabled=true` стартує з контекстом, відповідає по мережі і зупиняється |
| `HttpRatesAdapterTest` | HTTP-адаптер проти WireMock: успіх, 404, 500, read-timeout, вимкнений сервер |
| `GrpcRatesAdapterTest` | gRPC-адаптер проти in-process сервера: успіх, `NOT_FOUND`, `INTERNAL`, deadline, вимкнений сервер |
| `RatesClientConfigTest` | `http` і `grpc` вибирають свій адаптер для обох портів, значення беруться з конфігурації, невідоме значення валить контекст |
| `CurrencyPriceServiceTest` | Бізнес-дії з моками портів: розрахунки, 404 для кімнати, неправильна кількість ночей |
| `CurrencyPriceControllerTest` | Відповіді 200, 404, 503 і 400 контролера разом із `GlobalExceptionHandler` |