package com.remiges.logharbour.util;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.common.KafkaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remiges.logharbour.config.Constants;
import com.remiges.logharbour.constant.LogharbourConstants;
import com.remiges.logharbour.exception.LogException;
import com.remiges.logharbour.exception.LogQueryException;
import com.remiges.logharbour.model.ChangeInfo;
import com.remiges.logharbour.model.DebugInfo;
import com.remiges.logharbour.model.LogData;
import com.remiges.logharbour.model.LogEntry;
import com.remiges.logharbour.model.LogEntry.LogPriority;
import com.remiges.logharbour.model.LogEntry.LogType;
import com.remiges.logharbour.model.LogEntry.Status;
import com.remiges.logharbour.model.request.LogharbourRequestBo;
import com.remiges.logharbour.model.response.GetLogsResponse;
import com.remiges.logharbour.service.ElasticQueryServices;
import com.remiges.logharbour.service.LoggerContext;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Service
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LHLogger {

    private String app;
    private String system;
    private String module;
    private LogPriority pri;
    private String who;
    private String op;
    private String clazz;
    private String instanceId;
    private Status status;
    private String error;
    private String remoteIP;

    private KafkaTemplate<String, String> kafkaTemplate;
    private String topic;
    private LoggerContext loggerContext;
    private PrintWriter writer;
    private ObjectMapper objectMapper;

    @Autowired
    private ElasticQueryServices elasticQueryServices;

    @Autowired
    private Constants constants;

    /**
     * Constructs an instance of LHLogger with the specified parameters.
     *
     * @param kafkaTemplate     the KafkaTemplate used for sending messages to Kafka
     * @param printWriter       the PrintWriter used for writing log messages
     * @param logHarbourContext the LoggerContext used for managing log
     *                          configurations
     * @param topic             the Kafka topic to which messages will be sent
     * @param objectMapper      the ObjectMapper used for JSON serialization and
     *                          deserialization
     */
    public LHLogger(KafkaTemplate<String, String> kafkaTemplate, PrintWriter printWriter,
            LoggerContext logHarbourContext, String topic, ObjectMapper objectMapper) {
        try {
            this.writer = printWriter;
            this.loggerContext = logHarbourContext;
            this.kafkaTemplate = kafkaTemplate;
            this.topic = topic;
            this.objectMapper = objectMapper;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the log details for the logger.
     *
     * @param app           Application name.
     * @param system        System name.
     * @param module        Module name.
     * @param pri           Log priority.
     * @param who           User or service performing the operation.
     * @param op            Operation being performed.
     * @param clazz         Class name.
     * @param instanceId    Instance ID.
     * @param status        Status of the operation.
     * @param error         Error message.
     * @param remoteIP      Remote IP address.
     * @param loggerContext Logger context.
     */
    public LHLogger setLogDetails(String app, String system, String module, LogPriority pri, String who, String op,
            String clazz, String instanceId, Status status, String error, String remoteIP) {
        this.app = app;
        this.system = system;
        this.module = module;
        this.pri = pri;
        this.who = who;
        this.op = op;
        this.clazz = clazz;
        this.instanceId = instanceId;
        this.status = status;
        this.error = error;
        this.remoteIP = remoteIP;

        return this;

    }

    /**
     * Logs a message to both the local log file and a Kafka topic, depending on the
     * specified priority.
     *
     * This method writes the provided log message to a local log file using a
     * PrintWriter and also sends
     * the log message to a Kafka topic if the logging priority meets the criteria.
     * If an error occurs
     * while writing to the log file or sending the message to Kafka, a LogException
     * is thrown.
     *
     * @param logMessage the log message to be written and potentially sent to Kafka
     * @param priority   the priority of the log message, used to determine if it
     *                   should be sent to Kafka
     * @throws LogException if an error occurs while writing to the log file or
     *                      sending the message to Kafka
     */
    private void log(String logMessage, LogPriority priority) throws LogException {
        try {

            writer.println(logMessage);
            writer.flush();

            if (writer.checkError()) {
                throw new LogException("Error occurred while writing to the log file.");
            }

        } catch (Exception e) {
            throw new LogException("Failed to write log message to file", e);
        }

        if (shouldLog(priority)) {
            try {
                this.kafkaTemplate.send(topic, logMessage).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LogException("Thread was interrupted while sending log message to Kafka", e);
            } catch (ExecutionException e) {
                throw new LogException("Failed to send log message to Kafka", e.getCause());
            } catch (KafkaException e) {
                throw new LogException("Kafka error occurred while sending log message", e);
            } catch (Exception e) {
                throw new LogException("Unexpected error occurred while sending log message to Kafka", e);
            }
        }
    }

    /**
     * Logs an activity message with optional data.
     *
     * This method creates a log entry with the given message and optional data.
     * If data is provided, it is converted to a string and included in the log
     * entry.
     * The log entry is then serialized to JSON and logged if it meets the logging
     * criteria.
     *
     * @param message the activity message to log
     * @param data    the additional data to log (can be null)
     * @throws JsonProcessingException if an error occurs during JSON serialization
     * @throws LogException            if an error occurs during logging
     */
    public void logActivity(String message, Object data) throws LogException {
        LogData logData = null;
        LogEntry entry;

        try {
            if (data != null) {
                String activityData = convertToString(data);
                logData = new LogData();
                logData.setActivityData(activityData);
                entry = newLogEntry(message, logData);
            } else {
                entry = newLogEntry(message, null);
            }

            entry.setLogType(LogType.ACTIVITY);
            String logMessage = objectMapper.writeValueAsString(entry);

            log(logMessage, entry.getPri());
        } catch (JsonProcessingException e) {
            throw new LogException("Failed to process JSON for logging activity", e);
        } catch (LogException e) {
            throw new LogException("Failed to log activity", e);
        } catch (Exception e) {
            throw new LogException("Unexpected error occurred during logging activity", e);
        }
    }

    /**
     * Logs a data change event.
     *
     * @param message The log message.
     * @param data    The change information.
     * @throws JsonProcessingException if an error occurs while processing the log
     *                                 entry.
     * @throws LogException
     */
    public void logDataChange(String message, ChangeInfo data) throws LogException {
        try {
            data.getChanges().forEach(change -> {
                change.setOldValue(convertToString(change.getOldValue()));
                change.setNewValue(convertToString(change.getNewValue()));
            });

            LogData logData = new LogData();
            logData.setChangeData(data);

            LogEntry entry = newLogEntry(message, logData);
            entry.setLogType(LogEntry.LogType.CHANGE);
            String logMessage = objectMapper.writeValueAsString(entry);
            log(logMessage, entry.getPri());
        } catch (JsonProcessingException e) {
            throw new LogException("Failed to process JSON while logging data change", e);
        } catch (LogException e) {
            throw new LogException("Failed to log data change", e);
        } catch (Exception e) {
            throw new LogException("Unexpected error occurred while logging data change", e);
        }
    }

    /**
     * Logs a debug message with additional debug information if the logger is in
     * debug mode.
     *
     * This method constructs a LogEntry with detailed debug information including
     * the process ID,
     * Java runtime version, the data provided, and the caller's stack trace
     * information. If the logger
     * is in debug mode, it serializes this LogEntry to JSON and logs it using the
     * `log` method.
     *
     * @param message the debug message to log
     * @param data    additional data to include in the debug information
     * @throws LogException if an error occurs during logging or JSON processing
     */
    public void logDebug(String message, Object data) throws LogException {
        try {
            if (loggerContext.isDebugMode()) {

                LogEntry entry;
                DebugInfo debugInfo = new DebugInfo();
                debugInfo.setPid(getPid());
                debugInfo.setRuntime(System.getProperty("java.version"));
                debugInfo.setData(data.toString());

                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                if (stackTrace.length > 2) {
                    StackTraceElement caller = stackTrace[2];
                    debugInfo.setFileName(caller.getFileName());
                    debugInfo.setLineNumber(caller.getLineNumber());
                    debugInfo.setFunctionName(caller.getMethodName());
                    debugInfo.setStackTrace(" ");
                }

                LogData logData = new LogData();
                logData.setDebugData(debugInfo);
                entry = newLogEntry(message, logData);
                entry.setLogType(LogEntry.LogType.DEBUG);
                String logMessage = objectMapper.writeValueAsString(entry);
                log(logMessage, entry.getPri());
            }
        } catch (JsonProcessingException e) {
            throw new LogException("Failed to process JSON while logging debug data", e);
        } catch (LogException e) {
            throw new LogException("Failed to log debug data", e);
        } catch (Exception e) {
            throw new LogException("Unexpected error occurred while logging debug data", e);
        }
    }

    /**
     * Creates a new LogEntry with the specified message and log data.
     *
     * @param message the log message
     * @param data    the log data containing additional information (can be null)
     * @return a new instance of LogEntry initialized with the current context and
     *         the provided message and data
     */
    public LogEntry newLogEntry(String message, LogData data) {
        return new LogEntry(app, system, module, pri, who, op, Instant.now(), clazz, instanceId, status, error,
                remoteIP, message, data);
    }

    /**
     * Checks if a message with the given priority should be logged based on the
     * minimum log priority set in the logger context.
     *
     * @param priority The priority of the log message.
     * @return True if the message should be logged, false otherwise.
     */
    public boolean shouldLog(LogPriority priority) {
        synchronized (loggerContext) {
            return priority.ordinal() >= loggerContext.getMinLogPriority().ordinal();
        }
    }

    /**
     * Gets the process ID of the current Java process.
     *
     * @return The process ID.
     */
    private int getPid() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(jvmName.split("@")[0]);
    }

    /**
     * Converts a stack trace to a string.
     *
     * @param stackTrace The stack trace elements.
     * @return The stack trace as a string.
     */
    private String getStackTraceAsString(StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Converts an object to a string.
     *
     * @param value The object to convert.
     * @return The string representation of the object.
     */
    private String convertToString(Object value) {
        return value != null ? value.toString() : null;
    }

    /**
     * Retrieves a list of change logs based on the specified query parameters.
     *
     * This method queries the Elasticsearch service for change logs that match the
     * given parameters.
     * The results are returned as a list of LogEntry objects.
     *
     * @param queryToken the token used for the query
     * @param app        the application name
     * @param className  the class name
     * @param instance   the instance ID
     * @param who        the user or service performing the operation
     * @param op         the operation being performed
     * @param fromtsStr  the start timestamp for the query (inclusive)
     * @param totsStr    the end timestamp for the query (inclusive)
     * @param ndays      the number of days for the query period
     * @param field      the specific field to query
     * @param remoteIP   the remote IP address
     * @return a list of LogEntry objects that match the query parameters
     * @throws LogQueryException if an error occurs while fetching the change logs
     */
    public List<LogEntry> getChangesLog(String queryToken, String app, String className, String instance, String who,
            String op, String fromtsStr, String totsStr, int ndays, String field,
            String remoteIP) {

        try {
            SearchHits<LogEntry> searchHits = elasticQueryServices.getQueryForChangeLogs(queryToken, app, className,
                    instance,
                    who, op, fromtsStr, totsStr, ndays,
                    field, remoteIP);

            return searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new LogQueryException("Error occurred while fetching change logs", e);
        }
    }

    /**
     * Fetches log entries based on various parameters and provides pagination
     * support.
     *
     * @param queryToken       The query token of the realm.
     * @param app              The application name to filter log entries. Can be
     *                         null.
     * @param who              The user whose actions are logged. Can be null.
     * @param className        The class name for filtering logs related to specific
     *                         objects. Can be null.
     * @param instance         The specific object instance for which logs are
     *                         requested. Must be null if className is not
     *                         specified.
     * @param op               The operation to filter log entries. Can be null.
     * @param fromtsStr        The starting timestamp for filtering logs. Can be
     *                         null.
     * @param totsStr          The ending timestamp for filtering logs. Can be null.
     * @param ndays            The number of days back in time the retrieval must
     *                         attempt. Ignored if fromts and tots are specified.
     * @param logType          The type of logs to retrieve. "A" for activity logs,
     *                         "C" for data-change logs, "D" for debug logs, or null
     *                         for all types.
     * @param remoteIP         The remote IP from where the operation was triggered.
     *                         Can be null.
     * @param pri              The log priority to filter logs. Can be null.
     * @param searchAfterTs    The timestamp to skip logs older than. Can be null.
     * @param searchAfterDocId The document ID to skip logs earlier than. Can be
     *                         null.
     * @return A GetLogsResponse containing paginated log entries and related
     *         information.
     * @throws Exception If an error occurs during log retrieval or processing.
     */
    public GetLogsResponse getLogs(String queryToken, String app, String who,
            String className, String instance,
            String op, String fromtsStr, String totsStr, int ndays, String logType,
            String remoteIP, LogEntry.LogPriority pri, String searchAfterTs,
            String searchAfterDocId) throws Exception {

        GetLogsResponse getLogsResponse = new GetLogsResponse();

        SearchHits<LogEntry> search = elasticQueryServices.getQueryForLogs(queryToken, app, who, className, instance,
                op, fromtsStr, totsStr, ndays, logType, remoteIP, pri, searchAfterTs, searchAfterDocId);

        long totalHits = search.getTotalHits();

        List<LogEntry> logEntries = search.getSearchHits().stream().map(SearchHit::getContent)
                .limit(constants.getLogharbourMaxRecord()).toList();
        if (constants.getLogharbourMaxRecord() <= totalHits) {
            getLogsResponse.setLogs(logEntries);
            getLogsResponse.setNrec(totalHits);
            return getLogsResponse;
        }
        getLogsResponse.setLogs(logEntries);
        getLogsResponse.setNrec(totalHits);
        return getLogsResponse;
    }

    public List<LogEntry> getSetlogs(LogharbourRequestBo logharbourRequestBo) throws Exception {

        try {
            System.out.println("my first request data here !!!!!!!!!");
            if (logharbourRequestBo.getQueryToken() == null && logharbourRequestBo.getQueryToken().isEmpty()) {
                throw new IllegalArgumentException("query token can not pass null or empty");
            }
            return this.processSearch(logharbourRequestBo, logharbourRequestBo.getSetAttr());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return this.processSearch(logharbourRequestBo, logharbourRequestBo.getSetAttr());
    }

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public List<LogEntry> processSearch(LogharbourRequestBo logharbourRequestBo, String getsetAttr) {

        try {
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

            if (logharbourRequestBo.getApp() != null && !logharbourRequestBo.getApp().isEmpty()) {
                boolQueryBuilder.must(MatchQuery.of(m -> m.field(LogharbourConstants.APP).query("Kra"))._toQuery());
            }
            if (logharbourRequestBo.getWho() != null && !logharbourRequestBo.getWho().isEmpty()) {
                boolQueryBuilder.must(MatchQuery.of(m -> m.field(LogharbourConstants.WHO).query("User3"))._toQuery());
            }
            if (logharbourRequestBo.getClassName() != null && !logharbourRequestBo.getClassName().isEmpty()) {
                boolQueryBuilder.must(MatchQuery
                        .of(m -> m.field(LogharbourConstants.CLASS_NAME).query(logharbourRequestBo.getClassName()))
                        ._toQuery());
            }
            if (logharbourRequestBo.getOp() != null && !logharbourRequestBo.getOp().isEmpty()) {
                boolQueryBuilder.must(MatchQuery
                        .of(m -> m.field(LogharbourConstants.OP).query(logharbourRequestBo.getOp()))._toQuery());
            }
            if (logharbourRequestBo.getRemoteIP() != null && !logharbourRequestBo.getRemoteIP().isEmpty()) {
                boolQueryBuilder.must(MatchQuery
                        .of(m -> m.field(LogharbourConstants.REMOTE_IP).query(logharbourRequestBo.getRemoteIP()))
                        ._toQuery());
            }
            if (logharbourRequestBo.getFromTs() != null) {
                boolQueryBuilder.must(MatchQuery
                        .of(m -> m.field(LogharbourConstants.WHEN).query(logharbourRequestBo.getFromTs().toString()))
                        ._toQuery());
            }
            if (logharbourRequestBo.getToTs() != null) {
                boolQueryBuilder.must(MatchQuery
                        .of(m -> m.field(LogharbourConstants.WHEN).query(logharbourRequestBo.getToTs().toString()))
                        ._toQuery());
            }
            // if (logharbourRequestBo.getNDays() == null) {
            // boolQueryBuilder.must(MatchQuery.of(m ->
            // m.field("who").query(logharbourRequestBo.getNDays()))._toQuery());
            // }
            if (logharbourRequestBo.getInstance() != null) {
                boolQueryBuilder.must(MatchQuery
                        .of(m -> m.field(LogharbourConstants.INSTANCE_ID).query(logharbourRequestBo.getInstance()))
                        ._toQuery());
            }
            if (logharbourRequestBo.getType() != null && !logharbourRequestBo.getType().isEmpty()) {
                boolQueryBuilder.must(
                        MatchQuery.of(m -> m.field(LogharbourConstants.LOG_TYPE).query(logharbourRequestBo.getType()))
                                ._toQuery());
            }
            /*********************************************************************************************************************/
            // Query query = NativeQuery.builder()
            // .withQuery(boolQueryBuilder.build()._toQuery())
            // .build();
            //
            // SearchHits<LogEntry> searchHits = elasticsearchOperations.search(query,
            // LogEntry.class);
            // List<LogEntry> loggerList = searchHits.getSearchHits()
            // .stream()
            // .map(SearchHit::getContent)
            // .toList();
            // return loggerList;

            /*********************************************************************************************************************/

            // Create aggregation based on getsetAttr with .keyword field
            Aggregation aggregation = Aggregation.of(a -> a
                    .terms(t -> t.field(getsetAttr + ".keyword").size(10)));

            Query query = NativeQuery.builder()
                    .withQuery(boolQueryBuilder.build()._toQuery())
                    .withAggregation("agg", aggregation)
                    .build();

            SearchHits<LogEntry> searchHits = elasticsearchOperations.search(query, LogEntry.class);

            // // Process aggregation results
            // Aggregations aggregations = searchHits.getAggregations();
            // if (aggregations != null && aggregations.get(getsetAttr) != null) {
            // // Process the aggregation results as needed
            // }

            List<LogEntry> loggerList = searchHits.getSearchHits()
                    .stream()
                    .map(SearchHit::getContent)
                    .toList();
            return loggerList;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;

    }

    // // Pattern for attribute validation
    // private static final Pattern PATTERN = Pattern.compile("^[a-z]{1,9}$");
    //
    // // Allowed attributes
    // private static final Map<String, Boolean> ALLOWED_ATTRIBUTES = new
    // HashMap<>();
    //
    // static {
    // ALLOWED_ATTRIBUTES.put("app", true);
    // ALLOWED_ATTRIBUTES.put("typeConst", true);
    // ALLOWED_ATTRIBUTES.put("op", true);
    // ALLOWED_ATTRIBUTES.put("instance", true);
    // ALLOWED_ATTRIBUTES.put("class", true);
    // ALLOWED_ATTRIBUTES.put("module", true);
    // ALLOWED_ATTRIBUTES.put("pri", true);
    // ALLOWED_ATTRIBUTES.put("status", true);
    // ALLOWED_ATTRIBUTES.put("remote_ip", true);
    // ALLOWED_ATTRIBUTES.put("system", true);
    // ALLOWED_ATTRIBUTES.put("who", true);
    // ALLOWED_ATTRIBUTES.put("field", true);
    // }

}