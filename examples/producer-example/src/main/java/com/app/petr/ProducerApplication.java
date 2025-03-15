package com.app.petr;

import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.ByteArrayOutputStream;

@SpringBootApplication
public class ProducerApplication implements ApplicationRunner {

    @Autowired
    private KafkaProducer<String, byte[]> kafkaProducer;

    @Autowired
    private KafkaProperties properties;

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        SpecificDatumWriter<Message> writer = new SpecificDatumWriter<>(Message.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);

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

            out.reset();
            writer.write(message, encoder);
            encoder.flush();

            ProducerRecord<String, byte[]> record = new ProducerRecord<>(
                    topic,
                    message.getId().toString(),
                    out.toByteArray()
            );

            kafkaProducer.send(record);
            Thread.sleep(1000);
        }
    }
}