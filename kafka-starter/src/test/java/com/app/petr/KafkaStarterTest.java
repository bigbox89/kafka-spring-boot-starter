package com.app.petr;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = KafkaStarterTest.TestConfig.class)
class KafkaStarterTest {

    @MockBean
    private KafkaProducer<String, byte[]> producer;

    @MockBean
    private KafkaConsumer<String, byte[]> consumer;

    @Test
    void contextLoads() {
        assertThat(producer).isNotNull();
        assertThat(consumer).isNotNull();
    }

    @Configuration
    static class TestConfig {
        @Bean
        public KafkaProperties kafkaProperties() {
            KafkaProperties properties = new KafkaProperties();
            KafkaProperties.Producer producerConfig = properties.getProducer();
            producerConfig.setTopic("test-topic");
            KafkaProperties.Consumer consumerConfig = properties.getConsumer();
            consumerConfig.setTopic("test-topic");
            consumerConfig.setGroupId("test-group");
            properties.setBootstrapServers("localhost:9092");
            return properties;
        }

        @Bean
        public KafkaAutoConfiguration kafkaAutoConfiguration(KafkaProperties kafkaProperties) {
            return new KafkaAutoConfiguration(kafkaProperties);
        }
    }
}