package com.app.petr;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "apppetr.kafka")
public class KafkaProperties {
    private String bootstrapServers = "localhost:9092";
    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();
    private String avroSchemaPath;


    public static class Producer {
        private String topic = "default-topic";
        private boolean idempotenceEnabled = true; // Идемпотентность для надежности
        private int acks = 1; // Подтверждения: 0, 1, all
        private int retries = 3; // Количество попыток при сбоях
        private int retryBackoffMs = 1000; // Задержка между ретраями
        private int deliveryTimeoutMs = 120000; // Таймаут доставки
        private int requestTimeoutMs = 30000; // Таймаут запроса
        private int maxInFlightRequests = 5; // Макс. количество неподтвержденных запросов
        private Map<String, Object> config = new HashMap<>(); // Дополнительные настройки

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public boolean isIdempotenceEnabled() { return idempotenceEnabled; }
        public void setIdempotenceEnabled(boolean idempotenceEnabled) { this.idempotenceEnabled = idempotenceEnabled; }
        public int getAcks() { return acks; }
        public void setAcks(int acks) { this.acks = acks; }
        public int getRetries() { return retries; }
        public void setRetries(int retries) { this.retries = retries; }
        public int getRetryBackoffMs() { return retryBackoffMs; }
        public void setRetryBackoffMs(int retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }
        public int getDeliveryTimeoutMs() { return deliveryTimeoutMs; }
        public void setDeliveryTimeoutMs(int deliveryTimeoutMs) { this.deliveryTimeoutMs = deliveryTimeoutMs; }
        public int getRequestTimeoutMs() { return requestTimeoutMs; }
        public void setRequestTimeoutMs(int requestTimeoutMs) { this.requestTimeoutMs = requestTimeoutMs; }
        public int getMaxInFlightRequests() { return maxInFlightRequests; }
        public void setMaxInFlightRequests(int maxInFlightRequests) { this.maxInFlightRequests = maxInFlightRequests; }
        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
    }

    public static class Consumer {
        private String topic = "default-topic";
        private String groupId = "default-group";
        private int maxPollRecords = 500; // Макс. записей за один poll
        private int maxPollIntervalMs = 300000; // Макс. интервал между poll
        private int sessionTimeoutMs = 10000; // Таймаут сессии
        private int heartbeatIntervalMs = 3000; // Интервал heartbeat
        private int fetchMaxBytes = 52428800; // Макс. размер выборки (50MB)
        private boolean autoCommitEnabled = true; // Автокоммит оффсетов
        private int autoCommitIntervalMs = 5000; // Интервал автокоммита
        private String autoOffsetReset = "earliest"; // Сброс оффсета
        private int retryBackoffMs = 1000; // Задержка между ретраями
        private int maxRetries = 3; // Количество ретраев
        private Map<String, Object> config = new HashMap<>(); // Дополнительные настройки

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }
        public int getMaxPollRecords() { return maxPollRecords; }
        public void setMaxPollRecords(int maxPollRecords) { this.maxPollRecords = maxPollRecords; }
        public int getMaxPollIntervalMs() { return maxPollIntervalMs; }
        public void setMaxPollIntervalMs(int maxPollIntervalMs) { this.maxPollIntervalMs = maxPollIntervalMs; }
        public int getSessionTimeoutMs() { return sessionTimeoutMs; }
        public void setSessionTimeoutMs(int sessionTimeoutMs) { this.sessionTimeoutMs = sessionTimeoutMs; }
        public int getHeartbeatIntervalMs() { return heartbeatIntervalMs; }
        public void setHeartbeatIntervalMs(int heartbeatIntervalMs) { this.heartbeatIntervalMs = heartbeatIntervalMs; }
        public int getFetchMaxBytes() { return fetchMaxBytes; }
        public void setFetchMaxBytes(int fetchMaxBytes) { this.fetchMaxBytes = fetchMaxBytes; }
        public boolean isAutoCommitEnabled() { return autoCommitEnabled; }
        public void setAutoCommitEnabled(boolean autoCommitEnabled) { this.autoCommitEnabled = autoCommitEnabled; }
        public int getAutoCommitIntervalMs() { return autoCommitIntervalMs; }
        public void setAutoCommitIntervalMs(int autoCommitIntervalMs) { this.autoCommitIntervalMs = autoCommitIntervalMs; }
        public String getAutoOffsetReset() { return autoOffsetReset; }
        public void setAutoOffsetReset(String autoOffsetReset) { this.autoOffsetReset = autoOffsetReset; }
        public int getRetryBackoffMs() { return retryBackoffMs; }
        public void setRetryBackoffMs(int retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
    }

    public String getBootstrapServers() { return bootstrapServers; }
    public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }
    public Producer getProducer() { return producer; }
    public void setProducer(Producer producer) { this.producer = producer; }
    public Consumer getConsumer() { return consumer; }
    public void setConsumer(Consumer consumer) { this.consumer = consumer; }

    public String getAvroSchemaPath() {
        return avroSchemaPath;
    }

    public void setAvroSchemaPath(String avroSchemaPath) {
        this.avroSchemaPath = avroSchemaPath;
    }

}