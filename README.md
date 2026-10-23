# Лабораторна робота №6 — Quartz Scheduler

Проєкт **hotel-booking**: Spring Boot 3.3.5, Java 21, PostgreSQL 16, Liquibase, Keycloak, Quartz 2.3.2.

## Що реалізовано

Регулярні завдання виконуються за розкладом, який зберігається в PostgreSQL (таблиці `QRTZ_*`).
Після перезапуску чи розгортання нової версії завдання не зникають і не дублюються.

### Завдання (Jobs)

| Завдання | Ключ (group.name) | Що робить | Стратегія у разі збою |
|---|---|---|---|
| `BookingCleanupJob` | `maintenance.bookingCleanupJob` | `PENDING`-бронювання, дата заїзду яких минула, переводить у `CANCELLED` | Обмежений повтор: `refireImmediately`, поки спроб менше за `maxRetries`, далі чекає наступного запуску |
| `StayCompletionJob` | `lifecycle.stayCompletionJob` | `CONFIRMED`-бронювання, дата виїзду яких минула, переводить у `COMPLETED` | Помилка фіксується в лозі, завдання чекає наступного запланованого запуску |

Обидва завдання позначені `@DisallowConcurrentExecution`, тож наступний запуск чекає завершення попереднього.
Завдання «легкі»: вони лише читають параметри з `JobDataMap` і роблять один виклик `BookingMaintenanceService`.
Межа транзакції — `@Transactional` на методах сервісу.

### Тригери

| Тригер | Тип | Розклад | Часовий пояс | Перевизначає | Misfire |
|---|---|---|---|---|---|
| `maintenance.bookingCleanupNightlyTrigger` | Cron | `0 0 3 * * ?` — щоночі о 03:00 | `Europe/Kyiv` | `maxRetries = 5` | `DO_NOTHING` |
| `maintenance.bookingCleanupEveryMinuteTrigger` | Simple | щохвилини | — | `graceDays = 0` | `RESCHEDULE_NEXT_WITH_REMAINING_COUNT` |
| `lifecycle.stayCompletionTrigger` | Cron | `0 */2 * * * ?` — кожні 2 хв (для демонстрації) | `Europe/Kyiv` | — | `FIRE_ONCE_NOW` |

### Параметри (JobDataMap)

| Завдання | Значення за замовчуванням (у `JobDetail`) |
|---|---|
| `BookingCleanupJob` | `graceDays = 1`, `maxRetries = 2` |
| `StayCompletionJob` | `graceDays = 0` |

Quartz об'єднує параметри завдання й тригера (`getMergedJobDataMap()`), і значення тригера має пріоритет.
Якщо ключа немає, він написаний з помилкою, має не той тип або від'ємне значення, `JobParameters` кидає `JobExecutionException` з поясненням, і сервіс не викликається.

## Відповідність пунктам завдання

| № | Вимога | Де реалізовано |
|---|---|---|
| 1 | Схема таблиць Quartz для своєї БД | Офіційний `org/quartz/impl/jdbcjobstore/tables_postgres.sql` з `quartz-2.3.2.jar` → `db/changelog/migrations/002-quartz-tables.sql` (без `DROP TABLE` і `COMMIT`) |
| 2 | Створення схеми через Liquibase | `002-quartz-schema.xml`, changeset `005-create-quartz-tables` із `rollback`; підключено в `db.changelog-master.xml` |
| 3 | Quartz JDBC job store | `application.properties`: `spring.quartz.job-store-type=jdbc`, `initialize-schema=never`, `PostgreSQLDelegate`, `overwrite-existing-jobs=true` |
| 4 | Два Job, `JobDetail` і `Trigger` в одному `@Configuration`; один Job має два тригери | `config/QuartzConfig.java`; `BookingCleanupJob` має нічний і щохвилинний тригери |
| 5 | Параметри через `JobDataMap` | Значення за замовчуванням у `JobDetail`, перевизначення в тригерах, читання через `JobParameters` |
| 6 | Ідемпотентність | `BookingMaintenanceService`: один `UPDATE` з умовою на поточний статус; повторний запуск змінює 0 рядків |
| 7 | Стратегія у разі збою | `BookingCleanupJob` — обмежений повтор, `StayCompletionJob` — фіксація помилки й очікування наступного запуску |

## Сценарії → тести

| Сценарій | Тести |
|---|---|
| Jobs і тригери оголошуються в одному місці | `QuartzConfigTest`: `triggers_createdByConfig_pointToTheirJobByJobKey`, `jobsAndTriggers_createdByConfig_haveExplicitNameAndGroup` |
| Параметри тригера мають пріоритет | `QuartzConfigTest.mergedJobDataMap_triggerOverridesOneKey_triggerWinsAndOtherKeysKeepJobDefaults` (злиття через `JobExecutionContextImpl`, як у Quartz) |
| Jobs «легкі» | `BookingCleanupJobTest`: `execute_triggerOverridesGraceDays_callsServiceOnceWithTriggerValue`, `transactionBoundary_isOnServiceMethodNotOnJob`; `StayCompletionJobTest.execute_defaultParameters_callsServiceOnce` |
| Явна помилка при невідповідності параметрів | `BookingCleanupJobTest`: `execute_requiredKeyMissing_…`, `execute_keyMisspelledInTrigger_…`, `execute_negativeGraceDays_…`; `StayCompletionJobTest.execute_keyFromAnotherJob_…` |
| Паралельне виконання заборонено | `DisallowConcurrentExecutionTest` — справжній планувальник: з анотацією запуски йдуть по черзі, без неї накладаються |
| Кожен тригер має свою misfire-інструкцію | `QuartzConfigTest.triggers_createdByConfig_haveExplicitMisfireInstruction` |
| Cron не залежить від часового поясу сервера | `QuartzConfigTest`: `cronTriggers_createdByConfig_useKyivTimeZone`, `nightlyTrigger_winterDate_firesAt3amKyivTimeRegardlessOfServerZone`, `timeZone_unknownId_silentlyFallsBackToGmt` |
| Повторний запуск безпечний (IT) | `BookingMaintenanceServiceIT` — два запуски поспіль на справжній PostgreSQL, другий змінює 0 рядків |
| Невдале виконання обробляється цілеспрямовано | `BookingCleanupJobTest`: `execute_serviceFailsAndRetriesLeft_…`, `execute_serviceKeepsFailing_givesUpAfterMaxRetriesWithoutInfiniteLoop`; `StayCompletionJobTest.execute_serviceFails_recordsErrorAndWaitsForNextScheduledRun` |
| Розклад зберігається в БД через Liquibase | `002-quartz-schema.xml` + `spring.quartz.jdbc.initialize-schema=never` |

## Нові файли лаби 6

```
hotel-booking/src/main/java/com/smilezevil/hotelbooking/
  config/QuartzConfig.java
  job/BookingCleanupJob.java
  job/StayCompletionJob.java
  job/JobParameters.java
  service/BookingMaintenanceService.java
  repository/BookingRepository.java
hotel-booking/src/main/resources/
  application.properties
  db/changelog/db.changelog-master.xml
  db/changelog/migrations/002-quartz-schema.xml
  db/changelog/migrations/002-quartz-tables.sql
hotel-booking/src/test/java/com/smilezevil/hotelbooking/
  config/QuartzConfigTest.java
  job/BookingCleanupJobTest.java
  job/StayCompletionJobTest.java
  job/DisallowConcurrentExecutionTest.java
  job/JobContexts.java
  service/BookingMaintenanceServiceIT.java
```

## Запуск

Потрібно: Java 21, PostgreSQL 16 з базою `hotel_booking`, Keycloak 26 на порту 9090 (realm `course-realm`, client `backend-app`).

```
brew services start postgresql@16
cd <папка-keycloak>/bin && ./kc.sh start-dev --http-port 9090
cd hotel-booking && ./mvnw spring-boot:run
```

Під час старту Liquibase створює таблиці `QRTZ_*`, а в лозі з'являється
`Using job-store 'org.springframework.scheduling.quartz.LocalDataSourceJobStore' - which supports persistence`.

## Тести

Unit-тести (класи `*IT` Maven за замовчуванням пропускає):

```
./mvnw test
```

Інтеграційний тест ідемпотентності (потрібна запущена PostgreSQL, дані після тесту відкочуються):

```
./mvnw test -Dtest=BookingMaintenanceServiceIT
```

## Перевірка розкладу в базі

```sql
select job_name, job_group, job_class_name, is_nonconcurrent from qrtz_job_details;
select trigger_name, trigger_group, trigger_type, misfire_instr,
       to_timestamp(next_fire_time / 1000) as next_fire
from qrtz_triggers order by trigger_name;
select trigger_name, cron_expression, time_zone_id from qrtz_cron_triggers;
```

Очікувано: 2 завдання з `is_nonconcurrent = true`, 3 тригери, `time_zone_id = Europe/Kyiv`.
Після перезапуску застосунку тригерів так само 3, дублікатів немає.

## Демонстрація

Файл `hotel-booking/hotel-api.http`, розділ «6. QUARTZ»:

1. **6.1** створює `PENDING`-бронювання з датою заїзду в минулому. Протягом хвилини лог показує `Cancelled 1 …`, а наступного запуску `Cancelled 0 …`.
2. **6.2** показує, що статус став `CANCELLED`.
3. **6.3** і **6.4** роблять те саме для завершення проживання: статус стає `COMPLETED`.
