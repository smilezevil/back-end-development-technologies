# Лабораторна робота №11 — Auditing, Spring Batch

`hotel-booking` тепер знає, хто і коли створив чи змінив готель, кімнату або бронювання, і зберігає всі версії цих сутностей.
Кімнати можна масово завантажити з CSV: поганий рядок пропускається з поясненням, а повторний імпорт не створює дублів.
Бонус: версіонування кімнат засобами PostgreSQL (функції і тригери).

Попередні лабораторні роботи лежать у своїх гілках.

```
HTTP-запит (токен Keycloak, bob) ──┐
                                   ├─ SecurityContext ─ CurrentAuditor ─┬─ AuditorAware          → created_by / updated_by
Spring Batch (batch:roomImport) ───┘                                    └─ AuditRevisionListener → revinfo.changed_by

POST /api/imports/rooms?file=…  → 202 + executionId, job у потоці room-import-N
     FlatFileItemReader → RoomImportProcessor → RepositoryItemWriter    chunk-size 5, skip-limit 10
GET  /api/imports/rooms/{id}    → статус, read / write / filter / skip
```

## 1. Audit fields

- `audit/AuditableEntity` (`@MappedSuperclass`) містить `createdAt`, `createdBy`, `updatedAt`, `updatedBy`. Її розширюють `Hotel`, `Room` і `Booking`.
- `audit/AuditingConfig` вмикає `@EnableJpaAuditing` і реєструє `AuditorAware`.
- `audit/CurrentAuditor` бере користувача з `SecurityContextHolder`: для токена Keycloak це `preferred_username`, для інших автентифікацій `getName()`.
- Якщо користувача немає (немає автентифікації, анонім, неавтентифікований токен), кидається `IllegalStateException` з поясненням. Транзакція відкочується, і рядок без автора не потрапляє в базу. Фоновий код має сам встановити свою identity, як це робить імпорт CSV.
- Колонки додає Liquibase (`004-audit-schema.xml`). Рядки, що існували до аудиту, отримали автора `migration`.

## 2. Історія змін (Hibernate Envers)

- `@Audited` на `Hotel`, `Room`, `Booking`. Таблиці `hotel_aud`, `room_aud`, `booking_aud` і `revinfo` створює Liquibase, Hibernate лише перевіряє їх (`ddl-auto: validate`).
- `audit/AuditRevision` — власна сутність ревізії з колонкою `changed_by`, яку заповнює `audit/AuditRevisionListener`. Одна транзакція дає одну ревізію.
- `@NotAudited` на `Hotel.description`. Це довгий маркетинговий текст, його часто правлять, а на ціни, бронювання й гроші він не впливає. Зміна лише опису не створює ревізії, хоча `updated_at` і `updated_by` у таблиці `hotel` оновлюються.
- `Booking.guest` позначено `RelationTargetAuditMode.NOT_AUDITED`. `Guest` з персональними даними свідомо не аудитується, тому в історії бронювання зберігається лише `guest_id`.
- `GET /api/history/hotels/{id}`, `/rooms/{id}`, `/bookings/{id}` доступні лише ADMIN. Відповідь містить кожну версію: номер ревізії, час, автора, тип зміни (`ADD`, `MOD`, `DEL`) і знімок сутності.
- Сутності використовують `@Getter @Setter`, а не `@Data`. Згенерований Lombok `hashCode` чіпав лінивий зв'язок `hotel` і ламав читання історії для готелю, який існував до ввімкнення аудиту.

## 3. Імпорт кімнат з CSV (Spring Batch)

Формат файлу:

```
hotelId,roomNumber,type,pricePerNight,capacity
1,701,STANDARD,1200.00,2
```

- **Reader.** `FlatFileItemReader` пропускає заголовок і передає кожен рядок як `RoomCsvRow` разом з номером рядка у файлі.
- **Processor.** `RoomImportProcessor` перевіряє правила:
    - готель існує;
    - номер кімнати не порожній і до 20 символів;
    - тип: STANDARD, DELUXE, LUXURY або SUITE;
    - ціна від 0 до 100000, не більше двох знаків після коми;
    - місткість від 1 до 10;
    - кімната не повторюється у файлі.

  Нову кімнату processor створює, змінену оновлює, а для незмінної повертає `null`, тобто фільтрує її.
- **Writer.** `RepositoryItemWriter` викликає `saveAll`. Кожен chunk — окрема транзакція і одна ревізія Envers.
- **Skip.** Пропускаються `FlatFileParseException` (рядок не розібрано) і `InvalidRoomRowException` (порушено правило), не більше `skip-limit` разів. Кожен пропуск пишеться в лог: `ROOM IMPORT skip, line 6: ціна має бути більшою за 0, а не -100`. Якщо пропусків більше за ліміт, job завершується зі статусом `FAILED`, а вже закомічені chunks лишаються.
- **Identity.** `ImportIdentityListener` перед стартом кладе в `SecurityContext` користувача `batch:roomImport`, а після завершення прибирає його. Тому в `created_by`, `updated_by` і `revinfo.changed_by` видно, що зміну зробив імпорт, а не людина.
- **REST.** `POST /api/imports/rooms?file=…` повертає `202`, `executionId` і заголовок `Location`, а job виконується у фоновому потоці. `GET /api/imports/rooms/{id}` повертає статус, час, лічильники `readCount`, `writeCount`, `filterCount`, `skipCount` і першу помилку. Обидва ендпоінти доступні лише ADMIN.
- **Повторний імпорт.** `JobInstance` визначається параметром `file`. Якщо той самий файл уже успішно імпортовано, сервіс повертає `409` з підказкою. Те саме буде, якщо імпорт цього файлу ще виконується. `rerun=true` додає унікальний параметр і запускає новий імпорт; дублів при цьому немає, бо наявні кімнати оновлюються або фільтруються. Імпорт зі статусом `FAILED` той самий запит продовжує з місця падіння.
- **Path traversal.** `ImportFileResolver` дозволяє лише `.csv` безпосередньо в папці імпорту. Ім'я нормалізується, батьківська папка має бути саме папкою імпорту, а реальний шлях після розкриття symlink має лежати всередині неї. `../../etc/passwd`, `/etc/hosts` і підпапки дають `400`.
- Таблиці `batch_*` створює Liquibase (`005-batch-schema.xml`), а Spring Boot їх не створює (`initialize-schema: never`).

Налаштування:

```yaml
spring:
  batch:
    job:
      enabled: false
    jdbc:
      initialize-schema: never

import:
  rooms:
    directory: ${IMPORT_DIR:imports}
    chunk-size: 5
    skip-limit: 10
```

## 4. Бонус: версіонування засобами PostgreSQL

`006-room-versioning.xml` додає:

- колонку `room.row_version`;
- таблицю `room_version` з повним знімком рядка в JSONB, операцією, користувачем БД і програмою (`application_name`);
- функції `room_next_row_version()` (BEFORE-тригер, збільшує версію, лише якщо рядок справді змінився) і `room_save_version()` (AFTER-тригер, записує версію, у тому числі для `DELETE`);
- функцію `room_restore_version(room_id, version)`, яка повертає кімнату до старої версії і записує це як нову версію;
- базову версію `BASELINE` для всіх кімнат, що вже існували.

| | Envers | Тригер у PostgreSQL |
|---|---|---|
| Що бачить | зміни через Hibernate | будь-які зміни: застосунок, psql, інші сервіси |
| Автор | користувач застосунку | користувач БД і програма |
| Ревізія | одна на транзакцію | своя версія для кожного рядка |

## Відповідність завданню

| № | Вимога | Де реалізовано |
|---|---|---|
| 1 | Audit fields у трьох сутностях через Spring Data auditing і `AuditorAware` | `audit/AuditableEntity`, `audit/AuditingConfig`, `Hotel`, `Room`, `Booking` |
| 2 | Користувач із `SecurityContext` | `audit/CurrentAuditor` |
| 3 | Користувача немає | `IllegalStateException` з поясненням, `CurrentAuditorTest` |
| 4 | Історія Envers з автором і часом | `@Audited`, `audit/AuditRevision`, `audit/AuditRevisionListener` |
| 4.1 | `@NotAudited` з поясненням | `Hotel.description` |
| 5 | Історія для ADMIN | `HistoryController`, `HistoryService` |
| 6 | Chunk-oriented job, chunk size з конфігурації | `batch/RoomImportJobConfig`, `import.rooms.chunk-size` |
| 7 | Створити, оновити, відфільтрувати (`null`) | `batch/RoomImportProcessor` |
| 8 | Skip з лімітом і логом рядка та причини | `faultTolerant().skip(...).skipLimit(...)`, `batch/RoomImportSkipListener` |
| 9 | Власна identity імпорту | `batch/ImportIdentityListener` (`batch:roomImport`) |
| 10 | REST: 202 та id, статус з лічильниками | `RoomImportController`, `RoomImportService`, `ImportStatusDTO` |
| 11 | Повтор того самого файлу дає 409, параметр для свідомого повтору | `ImportConflictException`, `rerun=true` |
| 12 | Захист від path traversal | `batch/ImportFileResolver` |
| 13 | Схеми Batch і аудиту через Liquibase | `004-audit-schema.xml`, `005-batch-schema.xml` |
| 14* | Версіонування функціями і тригерами БД | `006-room-versioning.xml` |

## Запуск

Потрібно: JDK 21, PostgreSQL (база `hotel_booking`) і Keycloak на порту 9090. Docker для цієї лаби не потрібен.

```
./mvnw -DskipTests package
java -jar hotel-booking/target/hotel-booking-1.0.0-SNAPSHOT.jar
```

Застосунок треба запускати з кореня проєкту: папка `imports` шукається відносно поточної папки (або задається через `IMPORT_DIR`). У `imports` лежать три приклади:

- `rooms-2027.csv` — нові кімнати і одна без змін;
- `rooms-broken.csv` — оновлення, фільтр і 7 поганих рядків;
- `rooms-too-many-errors.csv` — перевищення `skip-limit`.

Запити лежать у `hotel-booking/hotel-api.http`, у розділах «11. AUDIT / ENVERS» і «12. ІМПОРТ КІМНАТ З CSV». Користувачі Keycloak:

- `bob` — ADMIN;
- `carol` — лише USER, для перевірки 403. Її створено вручну в `course-realm`.

Перевірка в базі:

```
$(brew --prefix postgresql@16)/bin/psql -d hotel_booking -c "select id, room_number, created_by, updated_by from room order by id;"
$(brew --prefix postgresql@16)/bin/psql -d hotel_booking -c "select rev, changed_by from revinfo order by rev;"
$(brew --prefix postgresql@16)/bin/psql -d hotel_booking -c "select room_id, version, operation, client_app from room_version order by room_id, version;"
```

## Тести

```
./mvnw verify
```

| Тест | Що перевіряє |
|---|---|
| `CurrentAuditorTest` | Немає користувача, анонім, неавтентифікований токен дають помилку з поясненням; `preferred_username` з токена Keycloak; `sub`, якщо його немає; identity фонової задачі |
| `AuditRevisionListenerTest` | Автор потрапляє в ревізію; без користувача ревізія не створюється |
| `HistoryControllerTest` | Справжній `SecurityConfig`: ADMIN отримує 200, USER 403, без токена 401, немає історії 404 |
| `RoomImportProcessorTest` | Нова, змінена і незмінена (`null`) кімната, кожне правило з номером рядка, дублікати у файлі, повторна обробка після відкату chunk |
| `RoomCsvReaderTest` | Заголовок пропускається, номери рядків як у файлі, рядок з неправильною кількістю колонок дає `FlatFileParseException` |
| `ImportFileResolverTest` | `../`, абсолютний шлях, підпапка, symlink назовні, не `.csv`, порожнє ім'я, файлу немає |
| `ImportIdentityListenerTest` | Під час імпорту автор `batch:roomImport`, після нього identity прибрано |
| `RoomImportSkipListenerTest` | Лог пропуску з номером рядка, причиною і вмістом рядка |
| `RoomImportServiceTest` | Параметри job, `rerun`, 409 для завершеного і запущеного імпорту, статус з лічильниками |
| `RoomImportControllerTest` | 202 з `Location`, 409, 400, 403, статус |