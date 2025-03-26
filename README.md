Kafka Spring Boot Starter Documentation
Описание
kafka-spring-boot-starter — это стартер для Spring Boot, который упрощает интеграцию с Apache Kafka. Он предоставляет автоконфигурацию для продюсера и консюмера Kafka, включая поддержку enterprise-функций, таких как идемпотентность, ретраи и обработка ошибок. Стартер доступен в Maven Central под координатами io.github.bigbox89:kafka-spring-boot-starter.

Зависимости
Spring Boot 3.2.4
Spring Kafka 3.1.3
Apache Avro 1.11.3 (опционально, для сериализации)
Параметры конфигурации
Все параметры задаются через префикс apppetr.kafka в файлах application.yml или application.properties.

Общие параметры
Параметр	Описание	Тип	Значение по умолчанию
apppetr.kafka.bootstrap-servers	Список адресов брокеров Kafka	String	localhost:9092
Параметры продюсера (apppetr.kafka.producer)

### Общие параметры
| Параметр                     | Описание                          | Тип    | Значение по умолчанию |
|------------------------------|-----------------------------------|--------|-----------------------|
| `apppetr.kafka.bootstrap-servers` | Список адресов брокеров Kafka | `String` | `localhost:9092`      |
| `apppetr.kafka.avro-schema-path` | Путь к файлу схемы Avro (`.avdl`) | `String` | `null`            |

### Параметры продюсера (`apppetr.kafka.producer`)
| Параметр                     | Описание                          | Тип    | Значение по умолчанию |
|------------------------------|-----------------------------------|--------|-----------------------|
| `topic`                      | Имя топика для отправки сообщений | `String` | `default-topic`       |
| `idempotence-enabled`        | Включает идемпотентность          | `boolean` | `true`             |
| `acks`                       | Уровень подтверждений (0, 1, all) | `int`    | `1`                |
| `retries`                    | Количество ретраев                | `int`    | `3`                |
| `retry-backoff-ms`           | Задержка между ретраями (мс)      | `int`    | `1000`             |
| `delivery-timeout-ms`        | Таймаут доставки (мс)             | `int`    | `120000`           |
| `request-timeout-ms`         | Таймаут запроса (мс)              | `int`    | `30000`            |
| `max-in-flight-requests`     | Макс. неподтверждённых запросов   | `int`    | `5`                |
| `config`                     | Дополнительные настройки          | `Map<String, Object>` | `{}`         |

### Параметры консюмера (`apppetr.kafka.consumer`)
| Параметр                     | Описание                          | Тип    | Значение по умолчанию |
|------------------------------|-----------------------------------|--------|-----------------------|
| `topic`                      | Имя топика для чтения             | `String` | `default-topic`       |
| `group-id`                   | Идентификатор группы              | `String` | `default-group`       |
| `max-poll-records`           | Макс. записей за `poll`           | `int`    | `500`             |
| `max-poll-interval-ms`       | Макс. интервал между `poll` (мс)  | `int`    | `300000`          |
| `session-timeout-ms`         | Таймаут сессии (мс)               | `int`    | `10000`           |
| `heartbeat-interval-ms`      | Интервал heartbeat (мс)           | `int`    | `3000`            |
| `fetch-max-bytes`            | Макс. размер выборки (байт)       | `int`    | `52428800` (50MB) |
| `auto-commit-enabled`        | Включает автокоммит               | `boolean` | `true`            |
| `auto-commit-interval-ms`    | Интервал автокоммита (мс)         | `int`    | `5000`            |
| `auto-offset-reset`          | Политика сброса (`earliest`, `latest`, `none`) | `String` | `earliest` |
| `retry-backoff-ms`           | Задержка между ретраями (мс)      | `int`    | `1000`            |
| `max-retries`                | Количество ретраев                | `int`    | `3`               |
| `config`                     | Дополнительные настройки          | `Map<String, Object>` | `{}`         |