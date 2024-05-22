package com.remiges.logharbour.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remiges.logharbour.model.LogEntry;
import com.remiges.logharbour.repository.LogEntryRepository;
import com.remiges.logharbour.service.KafkaService;

@Service
public class LHLogger {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private LogEntryRepository logEntryRepository;

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

    public void logDataChange(LogEntry logEntry) throws JsonProcessingException {
        log(objectMapper.writeValueAsString(logEntry));
    }

    public void close() {
        writer.close();
    }

    /**
     * Method to fetch log changes based on various parameters
     */

    public List<LogEntry> getChanges(String querytoken, String app, String who, String className, String instance,
            String field,
            String fromtsStr, String totsStr, int ndays) throws Exception {

        Instant fromts = null;
        Instant tots = null;
        /**
         * Parsing timestamps from strings to Instant objects
         * 
         */

        try {
            if (fromtsStr != null && !fromtsStr.isEmpty()) {
                fromts = Instant.parse(fromtsStr);
            }
            if (totsStr != null && !totsStr.isEmpty()) {
                tots = Instant.parse(totsStr);
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid timestamp format. Please provide timestamps in ISO 8601 format.");
        }
        /**
         * Validating that fromts is before tots if both are specified
         * 
         */

        if (fromts != null && tots != null && fromts.isAfter(tots)) {
            throw new IllegalArgumentException("fromts must be before tots");
        }

        final Instant finalFromts = fromts;
        final Instant finalTots = tots;

        List<LogEntry> logs;

        LogEntry.LogType logType = LogEntry.LogType.CHANGE;

        /**
         * Fetching logs based on specified time range and other parameters
         */

        /**
         * Check if both fromts and tots are specified
         * 
         */
        if (fromts != null && tots != null) {

            // Fetch logs and filter them based on the provided time range
            logs = logEntryRepository.findByAppAndClassNameAndInstanceIdAndLogType(app, className, instance, logType)
                    .stream()
                    .filter(log -> {
                        Instant logInstant = Instant.parse(log.getWhen());
                        return !logInstant.isBefore(finalFromts) && !logInstant.isAfter(finalTots);
                    })
                    .collect(Collectors.toList());
        } else if (fromts != null) {

            // If only fromts is specified, fetch logs and filter them to be after fromts
            logs = logEntryRepository.findByAppAndClassNameAndInstanceIdAndLogType(app, className, instance, logType)
                    .stream()
                    .filter(log -> !Instant.parse(log.getWhen()).isBefore(finalFromts))
                    .collect(Collectors.toList());
        } else if (tots != null) {

            // If only tots is specified, fetch logs and filter them to be before tots
            logs = logEntryRepository.findByAppAndClassNameAndInstanceIdAndLogType(app, className, instance, logType)
                    .stream()
                    .filter(log -> !Instant.parse(log.getWhen()).isAfter(finalTots))
                    .collect(Collectors.toList());
        } else if (ndays > 0) {

            // If ndays is specified, calculate the time range from now to ndays ago
            Instant end = Instant.now();
            Instant start = end.minusSeconds(ndays * 86400L);

            // Fetch logs and filter them based on the calculated time range
            logs = logEntryRepository.findByAppAndClassNameAndInstanceIdAndLogType(app, className, instance, logType)
                    .stream()
                    .filter(log -> {
                        Instant logInstant = Instant.parse(log.getWhen());
                        return !logInstant.isBefore(start) && !logInstant.isAfter(end);
                    })
                    .collect(Collectors.toList());
        } else {

            // If no time range or ndays is specified, fetch logs without any additional
            // filtering
            logs = logEntryRepository.findByAppAndClassNameAndInstanceIdAndLogType(app, className, instance, logType);
        }

        // Optional filtering by 'who' and 'field'
        if (who != null && !who.isEmpty()) {
            logs = logs.stream().filter(log -> who.equals(log.getWho())).toList();
        }
        if (field != null && !field.isEmpty()) {
            logs = logs.stream()
                    .filter(log -> {
                        Object data = log.getData();
                        return data instanceof Map && ((Map<?, ?>) data).containsKey(field);
                    })
                    .toList();
        }

        return logs;
    }

}
