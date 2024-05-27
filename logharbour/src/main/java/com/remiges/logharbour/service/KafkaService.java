package com.remiges.logharbour.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.remiges.logharbour.config.Constants;
import com.remiges.logharbour.model.LogEntry;
import com.remiges.logharbour.repository.LogEntryRepository;

@Service
public class KafkaService {

    @Autowired
    private Constants constants;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void producerLog(String msg) {
        System.out.println(" Sending data to Kafka Topic : " + msg);
        kafkaTemplate.send(constants.getKafkaTopic(), msg);
    }

    /**
     * Consumes messages from the specified Kafka topic, deserializes them into
     * LogEntry objects,
     * and saves them to the Elasticsearch repository.
     *
     * @param msg the message received from the Kafka topic
     */
    @KafkaListener(topics = "${kafka.topic}")
    public void consumer(String msg) {
        try {
            LogEntry logEntry = objectMapper.readValue(msg, LogEntry.class);
            System.out.println(" Data receive from Kafka Topic is: " + msg);

            logEntryRepository.save(logEntry);

            System.out.println("Data saved to Elastic search :" + msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
