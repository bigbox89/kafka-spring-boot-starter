package com.app.petr;

import app.petr.Message;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
public class ProducerApplicationTest {

    private static final String TOPIC = "test_data";

    @Container
    private static final KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    @BeforeAll
    static void setup() {
        kafkaContainer.start();
    }

    private KafkaProducer<String, byte[]> createProducer(Properties props) {
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        return new KafkaProducer<>(props);
    }

    private byte[] serializeMessage(Message message) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        SpecificDatumWriter<Message> writer = new SpecificDatumWriter<>(Message.class);
        writer.write(message, encoder);
        encoder.flush();
        return out.toByteArray();
    }

    @Test
    public void testProducerWithDefaultConfig() throws Exception {
        Properties props = new Properties();
        KafkaProducer<String, byte[]> producer = createProducer(props);

        Message message = Message.newBuilder()
                .setId("test-id-1")
                .setContent("Default config test")
                .setTimestamp(System.currentTimeMillis())
                .build();

        byte[] serializedMessage = serializeMessage(message);
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(TOPIC, message.getId().toString(), serializedMessage);

        Future<?> future = producer.send(record);
        producer.flush();

        assertThat(future.get()).isNotNull(); // Проверяем успешную отправку
        producer.close();
    }

    @Test
    public void testProducerWithIdempotenceEnabled() throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true"); // Включаем идемпотентность
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Требуется для идемпотентности
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5"); // Ограничение для идемпотентности
        KafkaProducer<String, byte[]> producer = createProducer(props);

        Message message = Message.newBuilder()
                .setId("test-id-2")
                .setContent("Idempotent test")
                .setTimestamp(System.currentTimeMillis())
                .build();

        byte[] serializedMessage = serializeMessage(message);
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(TOPIC, message.getId().toString(), serializedMessage);

        Future<?> future = producer.send(record);
        producer.send(record); // Отправляем тот же ключ ещё раз
        producer.flush();

        assertThat(future.get()).isNotNull(); // Успешная отправка с идемпотентностью
        producer.close();
    }

    @Test
    public void testProducerWithAcksZero() throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.ACKS_CONFIG, "0"); // Без подтверждений
        KafkaProducer<String, byte[]> producer = createProducer(props);

        Message message = Message.newBuilder()
                .setId("test-id-3")
                .setContent("Acks=0 test")
                .setTimestamp(System.currentTimeMillis())
                .build();

        byte[] serializedMessage = serializeMessage(message);
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(TOPIC, message.getId().toString(), serializedMessage);

        Future<?> future = producer.send(record);
        producer.flush();

        assertThat(future.get()).isNotNull(); // Отправка без ожидания подтверждения
        producer.close();
    }

    @Test
    public void testProducerWithRetries() throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.RETRIES_CONFIG, "3"); // 3 попытки
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, "100"); // Задержка между попытками 100 мс
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "500"); // Уменьшаем таймаут запроса до 500 мс
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "1000"); // Таймаут доставки 1000 мс (должен быть >= linger.ms + request.timeout.ms)
        props.put(ProducerConfig.LINGER_MS_CONFIG, "0"); // Явно задаём linger.ms для ясности
        KafkaProducer<String, byte[]> producer = createProducer(props);

        Message message = Message.newBuilder()
                .setId("test-id-4")
                .setContent("Retries test")
                .setTimestamp(System.currentTimeMillis())
                .build();

        byte[] serializedMessage = serializeMessage(message);
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(TOPIC, message.getId().toString(), serializedMessage);

        Future<?> future = producer.send(record);
        producer.flush();

        assertThat(future.get()).isNotNull(); // Успешная отправка с ретраями
        producer.close();
    }

    @Test
    public void testProducerWithShortTimeout() {
        Properties props = new Properties();
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "1"); // Очень короткий таймаут
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "1"); // Очень короткий таймаут доставки
        KafkaProducer<String, byte[]> producer = createProducer(props);

        Message message = Message.newBuilder()
                .setId("test-id-5")
                .setContent("Short timeout test")
                .setTimestamp(System.currentTimeMillis())
                .build();

        byte[] serializedMessage;
        try {
            serializedMessage = serializeMessage(message);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(TOPIC, message.getId().toString(), serializedMessage);

        Future<?> future = producer.send(record);

        // Ожидаем исключение из-за короткого таймаута
        assertThrows(ExecutionException.class, future::get, "Expected timeout exception due to short timeout");
        assertThat(future.isDone()).isTrue();

        producer.close();
    }
}