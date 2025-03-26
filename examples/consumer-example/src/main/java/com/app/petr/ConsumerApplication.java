package com.app.petr;


import app.petr.Message;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;

import java.nio.ByteBuffer;

@SpringBootApplication
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

    @KafkaListener(topics = "#{kafkaProperties.consumer.topic}", groupId = "#{kafkaProperties.consumer.groupId}")
    public void listen(byte[] message) throws Exception {
        Message avroMessage = Message.fromByteBuffer(ByteBuffer.wrap(message));
        System.out.printf("Received message: id=%s, content=%s, timestamp=%d%n",
                avroMessage.getId(), avroMessage.getContent(), avroMessage.getTimestamp());
    }
}