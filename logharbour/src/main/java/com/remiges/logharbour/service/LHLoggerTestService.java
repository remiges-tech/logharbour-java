package com.remiges.logharbour.service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import com.remiges.logharbour.model.LogHarbourContext;
import com.remiges.logharbour.model.LogPriorityLevels;
import com.remiges.logharbour.util.Logharbour;

public class LHLoggerTestService implements Logharbour {

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

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
    public LogHarbourContext getLogharbourContext(LogPriorityLevels minLogPriority) {
        LogHarbourContext loggerContext = new LogHarbourContext(LogPriorityLevels.INFO);
        loggerContext.setDebugMode(true);
        return loggerContext;
    }

}
