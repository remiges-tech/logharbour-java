package com.remiges.logharbour.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Autowired
    private Constants constants;

    @Bean
    public NewTopic createTopic() {
        return TopicBuilder.name(constants.getKafkaTopic()).build();
    }

}
