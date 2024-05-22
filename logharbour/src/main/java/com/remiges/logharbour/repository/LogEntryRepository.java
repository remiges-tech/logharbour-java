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

        // Finds log entries by application, class name, instance ID, log type, and a
        // time range.
        List<LogEntry> findByAppAndClassNameAndInstanceIdAndLogTypeAndWhenBetween(String app, String className,
                        String instanceId, LogEntry.LogType logType, String from, String to);

        @Query("{\"bool\":{\"must\":{\"match\":{\"logType\":\"ACTIVITY\"}}}}")
        List<LogEntry> findActivityLogs();

        @Query("{\"bool\":{\"must\":{\"match\":{\"logType\":\"CHANGE\"}}}}")
        // @Query("{\"bool\":{\"should\":[{\"match\":{\"logType\":\"CHANGE\"}},{\"match\":{\"logType\":\"ACTIVITY\"}}]}}")
        List<LogEntry> findChangeLogs();
}
