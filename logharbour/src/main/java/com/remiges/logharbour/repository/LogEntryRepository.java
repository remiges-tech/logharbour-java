package com.remiges.logharbour.repository;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.remiges.logharbour.model.LogEntry;

@Repository
public interface LogEntryRepository extends ElasticsearchRepository<LogEntry, String> {

        // Finds log entries by application, class name, instance ID, and log type.
        List<LogEntry> findByAppAndClassNameAndInstanceIdAndLogType(String app, String className, String instanceId,
                        LogEntry.LogType logType);

        @Query("{\"bool\":{\"must\":{\"match\":{\"logType\":\"ACTIVITY\"}}}}")
        List<LogEntry> findActivityLogs();

        @Query("{\"bool\":{\"must\":{\"match\":{\"logType\":\"CHANGE\"}}}}")
        // @Query("{\"bool\":{\"should\":[{\"match\":{\"logType\":\"CHANGE\"}},{\"match\":{\"logType\":\"ACTIVITY\"}}]}}")
        List<LogEntry> findChangeLogs();

        // Custom query using BoolQueryBuilder logic
        @Query("{\"bool\": {\"must\": [" +
                        "{\"match\": {\"logType\": \"CHANGE\"}}," +
                        "{\"match\": {\"app\": \"?0\"}}," +
                        "{\"match\": {\"className\": \"?1\"}}," +
                        "{\"match\": {\"instanceId\": \"?2\"}}," +
                        "{\"match\": {\"who\": \"?3\"}}," +
                        "{\"range\": {\"when\": {\"gte\": \"?4\", \"lte\": \"?5\"}}}" +
                        "]}}")
        List<LogEntry> findLogEntries(String app, String className, String instanceId, String who, String from,
        String to);


        @Query("{\"bool\": {\"must\": [" +
        "{\"match\": {\"logType\": \"?0\"}}," +
        "{\"range\": {\"when\": {\"gte\": \"?1\", \"lte\": \"?2\"}}}," +
        "{\"match\": {\"who\": \"?3\"}}," +
        "{\"match\": {\"pri\": \"?4\"}}," +
        "{\"match\": {\"remoteIP\": \"?5\"}}," +
        "{\"match\": {\"op\": \"?6\"}}" +
        "]}}")
    List<LogEntry> findLogsByQuery(String logType, String fromtsStr, String totsStr, String who, String pri, String remoteIP, String op);
    }


