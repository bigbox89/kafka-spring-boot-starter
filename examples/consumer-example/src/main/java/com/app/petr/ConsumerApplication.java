package com.app.petr;

import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Duration;
import java.util.Collections;

@SpringBootApplication
public class ConsumerApplication implements ApplicationRunner {

    @Autowired
    private KafkaConsumer<String, byte[]> kafkaConsumer;

    @Autowired
    private KafkaProperties properties;

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String topic = properties.getConsumer().getTopic();
        if (topic == null) {
            throw new IllegalStateException("Consumer topic is not configured");
        }

        kafkaConsumer.subscribe(Collections.singleton(topic));
        SpecificDatumReader<Message> reader = new SpecificDatumReader<>(Message.class);

        while (true) {
            ConsumerRecords<String, byte[]> records = kafkaConsumer.poll(Duration.ofMillis(100));
            records.forEach(record -> {
                try {
                    Decoder decoder = DecoderFactory.get().binaryDecoder(record.value(), null);
                    Message message = reader.read(null, decoder);
                    System.out.printf("Received message: id=%s, content=%s, timestamp=%d%n",
                            message.getId(), message.getContent(), message.getTimestamp());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}