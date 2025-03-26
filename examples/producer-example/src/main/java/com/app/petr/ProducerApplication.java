package com.app.petr;

import app.petr.Message;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.ByteBuffer;

@SpringBootApplication
public class ProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }

    @Bean
    public ApplicationRunner runner(KafkaTemplate<String, byte[]> kafkaTemplate, KafkaProperties properties) {
        return args -> {
            String topic = properties.getProducer().getTopic();
            if (topic == null) {
                throw new IllegalStateException("Producer topic is not configured");
            }

            for (int i = 0; i < 100; i++) {
                Message message = Message.newBuilder()
                        .setId("id-" + i)
                        .setContent("Message " + i)
                        .setTimestamp(System.currentTimeMillis())
                        .build();

                ByteBuffer buffer = message.toByteBuffer();
                byte[] messageBytes = new byte[buffer.remaining()];
                buffer.get(messageBytes);

                kafkaTemplate.send(topic, message.getId().toString(), messageBytes);
                System.out.println("Sent message: " + message.getId());
                Thread.sleep(1000);
            }
        };
    }
}