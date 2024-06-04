package com.remiges.logharbour.util;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.kafka.core.KafkaTemplate;

import com.remiges.logharbour.model.LogEntry.LogPriority;
import com.remiges.logharbour.service.LoggerContext;

/**
 * Interface for Logharbour, providing methods to interact with Kafka,
 * manage file writers, and obtain logging context based on priority levels.
 */
public interface Logharbour {

    /**
     * Gets a Kafka connection as a KafkaTemplate.
     *
     * @return KafkaTemplate for interacting with Kafka.
     */
    public KafkaTemplate<String, String> getKafkaConnection();

    public String getKafkaTopic();

    /**
     * Gets a PrintWriter for writing logs to a file.
     *
     * @param fileName The name of the file to write logs to.
     * @return PrintWriter for writing to the specified file.
     * @throws IOException If an I/O error occurs while creating the PrintWriter.
     */
    public PrintWriter getFileWriter(String fileName) throws IOException;

    /**
     * Gets the LogHarbourContext based on the specified minimum log priority level.
     *
     * @param minLogPriority The minimum priority level for logging.
     * @return LogHarbourContext configured with the specified minimum log priority.
     */
    public LoggerContext getLoggerContext(LogPriority minLogPriority);

}
