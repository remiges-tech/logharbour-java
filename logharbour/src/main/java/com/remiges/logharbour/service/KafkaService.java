package com.remiges.logharbour.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        kafkaTemplate.send(constants.getKafkaTopic(), msg);
    }

    @KafkaListener(topics = "${kafka.topic}")
    public void cunsume(String msg) {
        try {
            LogEntry logEntry = objectMapper.readValue(msg, LogEntry.class);
            logEntryRepository.save(logEntry);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
