package com.remiges.logharbour.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.remiges.logharbour.config.Constants;

@Service
public class KafkaService {

    @Autowired
    private Constants constants;

    private KafkaTemplate<String, String> kafkaTemplate;

    public KafkaService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void producerLog(String msg) {
        kafkaTemplate.send(constants.getKafkaTopic(), msg);
    }
}
