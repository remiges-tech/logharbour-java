package com.remiges.logharbour.util;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.remiges.logharbour.constant.LogharbourConstants;
import com.remiges.logharbour.exception.InvalidTimestampRangeException;
import com.remiges.logharbour.model.ChangeInfo;
import com.remiges.logharbour.model.DebugInfo;
import com.remiges.logharbour.model.GetLogsResponse;
import com.remiges.logharbour.model.LogData;
import com.remiges.logharbour.model.LogEntry;
import com.remiges.logharbour.model.LogEntry.LogPriority;
import com.remiges.logharbour.model.LogEntry.LogType;
import com.remiges.logharbour.model.LogEntry.Status;
import com.remiges.logharbour.model.LoggerContext;
import com.remiges.logharbour.model.LogharbourRequestBo;
import com.remiges.logharbour.repository.LogEntryRepository;

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
public class LHLogger implements Cloneable {

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

    private static final Logger logger = LoggerFactory.getLogger(LHLogger.class);

    @Autowired
    private LogEntryRepository logEntryRepository;

    // cloning method
    @Override
    public LHLogger clone() {
        try {
            return (LHLogger) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Cloning not supported for LogEntry", e);
        }
    }

    /**
     * Default constructor that initializes the writer for the log file.
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

    public LogEntry newLogEntry(String message, LogData data) {
        return new LogEntry(app, system, module, pri, who, op, Instant.now(), clazz, instanceId, status, error,
                remoteIP, message, data);
    }

    /**
     * Logs a message by pushing it to Kafka. If Kafka is unavailable, logs to a
     * file.
     *
     * @param logMessage The log message to be logged.
     */
    private void log(String logMessage) {
        if (shouldLog(pri)){
            try {

                this.kafkaTemplate.send(topic, logMessage);

            } catch (Exception e) {
                writer.println(logMessage);
                writer.flush();
                writer.close();
                e.printStackTrace();
            }
        }
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
            return priority.ordinal() <= loggerContext.getMinLogPriority().ordinal();
        }
    }

    /**
     * Logs an activity event with data.
     * 
     * @param message The log message.
     * @param data    The data associated with the activity.
     * @throws JsonProcessingException if an error occurs while processing the log
     *                                 entry.
     */
    public void logActivity(String message, Object data) throws JsonProcessingException {

        LogData logData = null;
        LogEntry entry;
        if (data != null) {
            // Convert the data to a JSON string
            String activityData = convertToString(data);
            logData = new LogData();
            logData.setActivityData(activityData);
            entry = newLogEntry(message, logData);
        } else {
            entry = newLogEntry(message, null);
        }
        entry.setLogType(LogType.ACTIVITY);
        log(objectMapper.writeValueAsString(entry));
    }

    /**
     * Logs a data change event.
     *
     * @param message The log message.
     * @param data    The change information.
     * @throws JsonProcessingException if an error occurs while processing the log
     *                                 entry.
     */
    public void logDataChange(String message, ChangeInfo data) throws JsonProcessingException {

        data.getChanges().forEach(change -> {
            change.setOldValue(convertToString(change.getOldValue()));
            change.setNewValue(convertToString(change.getNewValue()));
        });

        LogData logData = new LogData();
        logData.setChangeData(data);

        LogEntry entry = newLogEntry(message, logData);
        entry.setLogType(LogEntry.LogType.CHANGE);

        log(objectMapper.writeValueAsString(entry));

    }

    /**
     * Logs a debug event with data.
     *
     * @param message The log message.
     * @param data    The debug data.
     * @throws JsonProcessingException if an error occurs while processing the log
     *                                 entry.
     */
    public void logDebug(String message, Object data) throws JsonProcessingException {

        if (loggerContext.isDebugMode()) {
            LogEntry entry;
            DebugInfo debugInfo = new DebugInfo();
            debugInfo.setPid(getPid());
            debugInfo.setRuntime(System.getProperty("java.version"));
            debugInfo.setData(data.toString()); // Convert the entire data to a JSON string

            // Retrieve the current thread's stack trace elements.
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            /**
             * Check if the stack trace has more than two elements.
             * The first two elements typically represent `getStackTrace` and `getThread`
             * calls,
             * so the third element (index 2) will be the actual caller of this method.
             */
            if (stackTrace.length > 2) {
                StackTraceElement caller = stackTrace[2];
                debugInfo.setFileName(caller.getFileName());
                debugInfo.setLineNumber(caller.getLineNumber());
                debugInfo.setFunctionName(caller.getMethodName());
                debugInfo.setStackTrace(getStackTraceAsString(stackTrace));
            }

            LogData logData = new LogData();
            logData.setDebugData(debugInfo);
            entry = newLogEntry(message, logData);
            entry.setLogType(LogEntry.LogType.DEBUG);
            log(objectMapper.writeValueAsString(entry));
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
     * Retrieves the list of log entries based on specified parameters.
     *
     * @param querytoken The query token.
     * @param app        The application name.
     * @param who        The user identifier.
     * @param className  The class name.
     * @param instance   The instance identifier.
     * @param field      The field to filter by.
     * @param fromtsStr  The start timestamp in string format.
     * @param totsStr    The end timestamp in string format.
     * @param ndays      The number of days for fetching logs.
     * @return The list of log entries.
     * @throws Exception If an error occurs during processing.
     */

    public List<LogEntry> getChanges(String queryToken, String app, String who, String className, String instance,
            String field, String fromtsStr, String totsStr, int ndays) throws Exception {

        Instant fromts = null;
        Instant tots = null;

        try {
            fromts = (fromtsStr != null && !fromtsStr.isEmpty()) ? Instant.parse(fromtsStr) : null;
            tots = (totsStr != null && !totsStr.isEmpty()) ? Instant.parse(totsStr) : null;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid timestamp format. Please provide timestamps in ISO 8601 format.");
        }

        if (fromts != null && tots != null && fromts.isAfter(tots)) {
            throw new InvalidTimestampRangeException("fromts must be before tots");
        }

        List<LogEntry> logs = new ArrayList<>();
        LogEntry.LogType logType = LogEntry.LogType.CHANGE;

        if (fromts != null && tots != null) {
            logs = logEntryRepository.findLogEntries(app, className, instance, who, fromts.toString(), tots.toString());
        } else if (fromts != null) {
            logs = logEntryRepository.findLogEntries(app, className, instance, who, fromts.toString(),
                    Instant.now().toString());
        } else if (tots != null) {
            logs = logEntryRepository.findLogEntries(app, className, instance, who, Instant.EPOCH.toString(),
                    tots.toString());
        } else if (ndays > 0) {
            Instant end = Instant.now();
            Instant start = end.minusSeconds(ndays * 86400L);
            logs = logEntryRepository.findLogEntries(app, className, instance, who, start.toString(), end.toString());
        } else {
            logs = logEntryRepository.findByAppAndClassNameAndInstanceIdAndLogType(app, className, instance, logType);
        }

        if (field != null && !field.isEmpty()) {
            logs = logs.stream()
                    .filter(log -> {
                        Object data = log.getData();
                        return data instanceof Map && ((Map<?, ?>) data).containsKey(field);
                    })
                    .collect(Collectors.toList());
        }

        return logs;
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

    private static final int LOGHARBOUR_GETLOGS_MAXREC = 5;

    public GetLogsResponse getLogs(String queryToken, String app, String who,
            String className, String instance,
            String op, String fromtsStr, String totsStr, int ndays, String logType,
            String remoteIP, LogEntry.LogPriority pri, String searchAfterTs,
            String searchAfterDocId) throws Exception {

        Instant fromts = null;
        Instant tots = null;

        // Parse the timestamps
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

        // Validate timestamp range
        if (fromts != null && tots != null && fromts.isAfter(tots)) {
            throw new IllegalArgumentException("fromts must be before tots");
        }

        // Convert the priority to string
        String priStr = pri != null ? pri.toString() : null;

        // Fetch logs based on the constructed boolean query
        List<LogEntry> filteredLogs = logEntryRepository.findLogsByQuery(logType, fromtsStr, totsStr, who, priStr,
                remoteIP, op);

        int totalLogs = filteredLogs.size();

        // Apply pagination using searchAfterTS and searchAfterDocID
        if (searchAfterTs != null && !searchAfterTs.isEmpty() && searchAfterDocId != null
                && !searchAfterDocId.isEmpty()) {
            Instant searchAfterInstant = Instant.parse(searchAfterTs);
            filteredLogs = filteredLogs.stream()
                    .filter(log -> {
                        Instant logInstant = Instant.parse(log.getWhen());
                        return logInstant.isAfter(searchAfterInstant) ||
                                (logInstant.equals(searchAfterInstant) && log.getId().compareTo(searchAfterDocId) > 0);
                    })
                    .collect(Collectors.toList());
        }

        // Ensure we do not exceed the LOGHARBOUR_GETLOGS_MAXREC
        int end = Math.min(LOGHARBOUR_GETLOGS_MAXREC, filteredLogs.size());
        List<LogEntry> paginatedLogs = filteredLogs.subList(0, end);

        // Set next searchAfterTs and searchAfterDocId for the next batch
        String nextSearchAfterTs = null;
        String nextSearchAfterDocId = null;

        if (!paginatedLogs.isEmpty() && paginatedLogs.size() == end) {
            LogEntry lastLog = paginatedLogs.get(paginatedLogs.size() - 1);
            nextSearchAfterTs = lastLog.getWhen();
            nextSearchAfterDocId = lastLog.getId();
        }

        // Create and return the response
        return new GetLogsResponse(paginatedLogs, totalLogs, end, null, nextSearchAfterTs, nextSearchAfterDocId);
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