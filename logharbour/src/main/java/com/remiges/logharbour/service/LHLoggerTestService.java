package com.remiges.logharbour.service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.remiges.logharbour.model.LogEntry.LogPriority;
import com.remiges.logharbour.util.Logharbour;

@Service
public class LHLoggerTestService implements Logharbour {

   
    private KafkaTemplate<String, String> kafkaTemplate;

    public LHLoggerTestService(KafkaTemplate<String, String> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public KafkaTemplate<String, String> getKafkaConnection() {
        return this.kafkaTemplate;
    }

    @Override
    public String getKafkaTopic() {
        return "logharbour";
    }

    @Override
    public PrintWriter getFileWriter(String fileName) throws IOException {
        PrintWriter writer;
        writer = new PrintWriter(new FileWriter(fileName, true));
        return writer;

    }

    @Override
    public LoggerContext getLoggerContext(LogPriority minLogPriority) {
        LoggerContext loggerContext = new LoggerContext(LogPriority.INFO);
        loggerContext.setDebugMode(true);
        return loggerContext;
    }

}
