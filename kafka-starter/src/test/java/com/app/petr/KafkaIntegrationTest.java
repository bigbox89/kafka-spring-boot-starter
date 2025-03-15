package com.app.petr;

import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
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
public class KafkaIntegrationTest {

    private static final String TOPIC = "rest_data";

    @Container
    private static final KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.3.0"));

    private KafkaProducer<String, byte[]> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        return new KafkaProducer<>(props);
    }

    private KafkaConsumer<String, byte[]> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }

    @Test
    public void testProducerAndConsumer() throws Exception {

        KafkaProducer<String, byte[]> producer = createProducer();
        KafkaConsumer<String, byte[]> consumer = createConsumer();

        consumer.subscribe(Collections.singleton(TOPIC));

        Message message = Message.newBuilder()
                .setId("test-id")
                .setContent("Test content")
                .setTimestamp(System.currentTimeMillis())
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        SpecificDatumWriter<Message> writer = new SpecificDatumWriter<>(Message.class);
        writer.write(message, encoder);
        encoder.flush();
        byte[] serializedMessage = out.toByteArray();

        ProducerRecord<String, byte[]> record = new ProducerRecord<>(TOPIC, message.getId().toString(), serializedMessage);
        producer.send(record).get();
        producer.flush();

        SpecificDatumReader<Message> reader = new SpecificDatumReader<>(Message.class);
        ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.isEmpty()).isFalse();

        records.forEach(consumerRecord -> {
            try {
                Decoder decoder = DecoderFactory.get().binaryDecoder(consumerRecord.value(), null);
                Message receivedMessage = reader.read(null, decoder);
                assertThat(receivedMessage.getId().toString()).isEqualTo(message.getId());
                assertThat(receivedMessage.getContent().toString()).isEqualTo(message.getContent());
                assertThat(receivedMessage.getTimestamp()).isEqualTo(message.getTimestamp());
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize message", e);
            }
        });

        producer.close();
        consumer.close();
    }
}