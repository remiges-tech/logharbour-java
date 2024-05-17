package com.remiges.logharbour.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remiges.logharbour.model.LogEntry;
import com.remiges.logharbour.service.KafkaService;

@Service
public class LHLogger {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaService kafkaService;

    // Writing a log in a text file will used in fallback writer for logs
    private String logFileName = "logharbour.txt";
    private PrintWriter writer;

    public LHLogger() {
        try {
            this.writer = new PrintWriter(new FileWriter(logFileName, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // method will push logs in kafka producer
    private void log(String logMessage) {

        // writer.println(logMessage); // function to write log in file
        try {

            kafkaService.producerLog(logMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
        // writer.flush();
    }

    public void logActivity(String message, LogEntry logEntry) throws JsonProcessingException {
        logEntry.setMsg(message);
        log(objectMapper.writeValueAsString(logEntry));
    }

    public void logDebug(LogEntry logEntry) throws JsonProcessingException {
        log(objectMapper.writeValueAsString(logEntry));
    }

    public void logChange(LogEntry logEntry) throws JsonProcessingException {
        log(objectMapper.writeValueAsString(logEntry));
    }

    public void close() {
        writer.close();
    }

}
