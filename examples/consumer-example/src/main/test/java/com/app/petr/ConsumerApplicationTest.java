package com.app.petr;

import app.petr.Message;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class ConsumerApplicationTest {

    private static final String TOPIC = "consumer_test_data";

    @Container
    private static final KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    @BeforeAll
    static void setup() {
        kafkaContainer.start();
    }

    @BeforeEach
    void clearTopic() {
        Properties adminProps = new Properties();
        adminProps.put("bootstrap.servers", kafkaContainer.getBootstrapServers());
        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            adminClient.deleteTopics(Collections.singleton(TOPIC)).all().get();
            adminClient.createTopics(Collections.singleton(new NewTopic(TOPIC, 1, (short) 1))).all().get();
        } catch (Exception e) {
            // Игнорируем ошибки, если топик не существовал
        }
    }

    private KafkaProducer<String, byte[]> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        return new KafkaProducer<>(props);
    }

    private KafkaConsumer<String, byte[]> createConsumer(Properties props) {
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        return new KafkaConsumer<>(props);
    }

    private byte[] serializeMessage(Message message) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        SpecificDatumWriter<Message> writer = new SpecificDatumWriter<>(Message.class);
        writer.write(message, encoder);
        encoder.flush();
        return out.toByteArray();
    }

    private Message deserializeMessage(byte[] data) throws Exception {
        Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        SpecificDatumReader<Message> reader = new SpecificDatumReader<>(Message.class);
        return reader.read(null, decoder);
    }

    private void sendMessages(int count) throws Exception {
        KafkaProducer<String, byte[]> producer = createProducer();
        for (int i = 0; i < count; i++) {
            Message message = Message.newBuilder()
                    .setId("id-" + i)
                    .setContent("Message " + i)
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            byte[] serializedMessage = serializeMessage(message);
            producer.send(new ProducerRecord<>(TOPIC, message.getId().toString(), serializedMessage)).get();
        }
        producer.flush();
        producer.close();
    }

    @Test
    public void testConsumerWithDefaultConfig() throws Exception {
        Properties props = new Properties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "default-group-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String, byte[]> consumer = createConsumer(props);

        consumer.subscribe(Collections.singleton(TOPIC));
        sendMessages(5);

        ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isEqualTo(5);

        consumer.close();
    }

    @Test
    public void testConsumerWithMaxPollRecords() throws Exception {
        Properties props = new Properties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "max-poll-group-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "2");
        KafkaConsumer<String, byte[]> consumer = createConsumer(props);

        consumer.subscribe(Collections.singleton(TOPIC));
        sendMessages(5);

        ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isLessThanOrEqualTo(2);

        consumer.close();
    }

    @Test
    public void testConsumerWithAutoOffsetResetLatest() throws Exception {
        Properties props = new Properties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "latest-group-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        KafkaConsumer<String, byte[]> consumer = createConsumer(props);

        // Подписываемся и вызываем poll для завершения регистрации
        consumer.subscribe(Collections.singleton(TOPIC));
        consumer.poll(Duration.ofSeconds(20)); // Даём Kafka время зарегистрировать консюмера

        // Убеждаемся, что до отправки сообщений ничего не читается
        ConsumerRecords<String, byte[]> recordsBefore = consumer.poll(Duration.ofSeconds(1));
        assertThat(recordsBefore.isEmpty()).isTrue();

        // Отправляем 3 сообщения после полной регистрации
        sendMessages(3);

        // Читаем новые сообщения
        ConsumerRecords<String, byte[]> recordsAfter = consumer.poll(Duration.ofSeconds(5));
        assertThat(recordsAfter.count()).isEqualTo(3);

        consumer.close();
    }

    @Test
    public void testConsumerWithDisableAutoCommit() throws Exception {
        Properties props = new Properties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "no-auto-commit-group-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        KafkaConsumer<String, byte[]> consumer = createConsumer(props);

        consumer.subscribe(Collections.singleton(TOPIC));
        sendMessages(5);

        ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isEqualTo(5);

        consumer.close();
        consumer = createConsumer(props);
        consumer.subscribe(Collections.singleton(TOPIC));
        ConsumerRecords<String, byte[]> recordsAgain = consumer.poll(Duration.ofSeconds(5));
        assertThat(recordsAgain.count()).isEqualTo(5);

        consumer.close();
    }

    @Test
    public void testConsumerWithShortSessionTimeout() throws Exception {
        Properties props = new Properties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "short-session-group-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "6000");
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "2000");
        KafkaConsumer<String, byte[]> consumer = createConsumer(props);

        consumer.subscribe(Collections.singleton(TOPIC));
        sendMessages(3);

        ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isEqualTo(3);

        consumer.close();
    }
}