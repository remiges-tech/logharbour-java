package com.remiges.logharbour.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remiges.logharbour.model.ChangeDetails;
import com.remiges.logharbour.model.ChangeInfo;
import com.remiges.logharbour.constant.LogharbourConstants;
import com.remiges.logharbour.model.GetLogsResponse;
import com.remiges.logharbour.model.LogData;
import com.remiges.logharbour.model.LogEntry;
import com.remiges.logharbour.model.LogharbourRequestBo;
import com.remiges.logharbour.repository.LogEntryRepository;
import com.remiges.logharbour.service.KafkaService;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;

@Service
public class LHLogger {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private LogEntryRepository logEntryRepository;

    private static final Logger logger = LoggerFactory.getLogger(LHLogger.class);

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

    // Method to create a new log entry
    public LogEntry newLogEntry(String message, LogData data) {
        return new LogEntry(message, message, message, null, message, message, message, message,
                message, null, message, message, message, null, data);
    }

    public void logActivity(String message, LogEntry logEntry) throws JsonProcessingException {
        logEntry.setMsg(message);
        log(objectMapper.writeValueAsString(logEntry));
    }

    public void logDebug(LogEntry logEntry) throws JsonProcessingException {
        log(objectMapper.writeValueAsString(logEntry));
    }

    // LogDataChange method logs a data change event.
    public void logDataChange(String message, ChangeInfo data) {
        for (ChangeDetails change : data.getChanges()) {
            change.setOldValue(convertToString(change.getOldValue()));
            change.setNewValue(convertToString(change.getNewValue()));
        }

        LogData logData = new LogData();
        logData.setChangeData(data);

        LogEntry entry = newLogEntry(message, logData);
        entry.setLogType(LogEntry.LogType.CHANGE);

        log(entry.toString());

    }

    private String convertToString(Object value) {
        return value != null ? value.toString() : null;
    }

    public void close() {
        writer.close();
    }

    /**
     * Method to fetch log changes based on various parameters
     * 
     * 
     * @param querytoken Query token (not used in this method).
     * @param app        Application identifier.
     * @param who        User identifier.
     * @param className  Class name.
     * @param instance   Instance identifier.
     * @param field      Field identifier.
     * @param fromtsStr  Start timestamp in ISO 8601 format.
     * @param totsStr    End timestamp in ISO 8601 format.
     * @param ndays      Number of days for which logs are required.
     * @return List of log entries matching the specified parameters.
     * @throws Exception If an error occurs during log retrieval or parameter
     *                   parsing.
     */

    public List<LogEntry> getChanges(String querytoken, String app, String who, String className, String instance,
            String field,
            String fromtsStr, String totsStr, int ndays) throws Exception {
        logger.debug(
                "Entering getChanges method with parameters: querytoken={}, app={}, who={}, className={}, instance={}, field={}, fromtsStr={}, totsStr={}, ndays={}",
                querytoken, app, who, className, instance, field, fromtsStr, totsStr, ndays);

        Instant fromts = null;
        Instant tots = null;
        /**
         * Parsing timestamps from strings to Instant objects
         * 
         */

        try {
            if (fromtsStr != null && !fromtsStr.isEmpty()) {
                fromts = Instant.parse(fromtsStr);
                logger.debug("Parsed fromts: {}", fromts);

            }
            if (totsStr != null && !totsStr.isEmpty()) {
                tots = Instant.parse(totsStr);
                logger.debug("Parsed tots: {}", tots);
            }
        } catch (DateTimeParseException e) {
            logger.error("Invalid timestamp format: fromtsStr={}, totsStr={}", fromtsStr, totsStr, e);

            throw new IllegalArgumentException(
                    "Invalid timestamp format. Please provide timestamps in ISO 8601 format.");
        }
        /**
         * Validating that fromts is before tots if both are specified
         * 
         */

        if (fromts != null && tots != null && fromts.isAfter(tots)) {
            logger.error("fromts must be before tots: fromts={}, tots={}", fromts, tots);

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

            logger.debug("Fetching logs between {} and {}", finalFromts, finalTots);

            // Fetch logs and filter them based on the provided time range
            logs = logEntryRepository.findByAppAndClassNameAndInstanceIdAndLogType(app, className, instance, logType)
                    .stream()
                    .filter(log -> {
                        Instant logInstant = Instant.parse(log.getWhen());
                        return !logInstant.isBefore(finalFromts) && !logInstant.isAfter(finalTots);
                    })
                    .collect(Collectors.toList());
        } else if (fromts != null) {

            logger.debug("Fetching logs after {}", finalFromts);

            // If only fromts is specified, fetch logs and filter them to be after fromts
            logs = logEntryRepository.findByAppAndClassNameAndInstanceIdAndLogType(app, className, instance, logType)
                    .stream()
                    .filter(log -> !Instant.parse(log.getWhen()).isBefore(finalFromts))
                    .collect(Collectors.toList());
        } else if (tots != null) {

            logger.debug("Fetching logs before {}", finalTots);
            // If only tots is specified, fetch logs and filter them to be before tots
            logs = logEntryRepository.findByAppAndClassNameAndInstanceIdAndLogType(app, className, instance, logType)
                    .stream()
                    .filter(log -> !Instant.parse(log.getWhen()).isAfter(finalTots))
                    .collect(Collectors.toList());
        } else if (ndays > 0) {

            // If ndays is specified, calculate the time range from now to ndays ago
            Instant end = Instant.now();
            Instant start = end.minusSeconds(ndays * 86400L);

            logger.debug("Fetching logs for the past {} days ({} to {})", ndays, start, end);

            // Fetch logs and filter them based on the calculated time range
            logs = logEntryRepository.findByAppAndClassNameAndInstanceIdAndLogType(app, className, instance, logType)
                    .stream()
                    .filter(log -> {
                        Instant logInstant = Instant.parse(log.getWhen());
                        return !logInstant.isBefore(start) && !logInstant.isAfter(end);
                    })
                    .collect(Collectors.toList());
        } else {

            logger.debug("Fetching all logs without time range filter");

            // If no time range or ndays is specified, fetch logs without any additional
            // filtering
            logs = logEntryRepository.findByAppAndClassNameAndInstanceIdAndLogType(app, className, instance, logType);
        }

        // Optional filtering by 'who' and 'field'
        if (who != null && !who.isEmpty()) {

            logger.debug("Filtering logs by who: {}", who);
            logs = logs.stream().filter(log -> who.equals(log.getWho())).toList();
        }
        if (field != null && !field.isEmpty()) {

            logger.debug("Filtering logs by field: {}", field);
            logs = logs.stream()
                    .filter(log -> {
                        Object data = log.getData();
                        return data instanceof Map && ((Map<?, ?>) data).containsKey(field);
                    })
                    .toList();
        }

        logger.debug("Returning {} log entries", logs.size());
        return logs;
    }

    /**
     * Retrieves log entries based on the provided filters.
     *
     * @param queryToken a token for querying logs (not currently used in
     *                   filtering).
     * @param app        the application name to filter logs.
     * @param who        the user identifier to filter logs.
     * @param className  the class name to filter logs.
     * @param instance   the instance identifier to filter logs.
     * @param fromtsStr  the start timestamp in ISO 8601 format.
     * @param totsStr    the end timestamp in ISO 8601 format.
     * @param ndays      the number of days to look back from the current time.
     * @param logType    the type of logs to retrieve ("A" for activity logs, "C"
     *                   for change logs).
     * @param pri        the priority of logs to filter.
     * @return a GetLogsResponse object containing the filtered log entries, the
     *         number of records, and any error messages.
     * @throws Exception if there is an error parsing the timestamps or applying the
     *                   filters.
     */
    public GetLogsResponse getLogs(String queryToken, String app, String who, String className, String instance,
            String op,
            String fromtsStr, String totsStr, int ndays, String logType, String remoteIP, LogEntry.LogPriority pri,
            String searchAfterTs, String searchAfterDocId)
            throws Exception {
        Instant fromts = null;
        Instant tots = null;
        // Instant searchAfterInstant =null;

        // Parse the timestamps

        int maxRecord = 5;
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

        final Instant finalFromts = fromts;
        final Instant finalTots = tots;

        List<LogEntry> changeLogs = new ArrayList<>();
        List<LogEntry> activityLogs = new ArrayList<>();

        // Fetch logs based on logType
        if (logType == null) {
            // No logType provided, fetch both types of logs
            changeLogs = logEntryRepository.findChangeLogs();
            activityLogs = logEntryRepository.findActivityLogs();
        } else {
            // LogType provided, fetch accordingly
            switch (logType) {
                case "A":
                    activityLogs = logEntryRepository.findActivityLogs();
                    break;
                case "C":
                    changeLogs = logEntryRepository.findChangeLogs();
                    break;
                default:
                    changeLogs = logEntryRepository.findChangeLogs();
                    activityLogs = logEntryRepository.findActivityLogs();
                    break;
            }
        }

        // Apply additional filters
        if ((app == null || app.isEmpty()) && (who == null || who.isEmpty()) &&
                (className == null || className.isEmpty()) && (instance == null || instance.isEmpty()) &&
                (fromts == null) && (tots == null) && ndays == 0 && pri == null) {
            // Logs are already fetched above
        } else if (fromts != null && tots != null) {
            changeLogs = changeLogs.stream()
                    .filter(log -> {
                        Instant logInstant = Instant.parse(log.getWhen());
                        return !logInstant.isBefore(finalFromts) && !logInstant.isAfter(finalTots);
                    })
                    .collect(Collectors.toList());
            activityLogs = activityLogs.stream()
                    .filter(log -> {
                        Instant logInstant = Instant.parse(log.getWhen());
                        return !logInstant.isBefore(finalFromts) && !logInstant.isAfter(finalTots);
                    })
                    .collect(Collectors.toList());
        } else if (fromts != null) {
            changeLogs = changeLogs.stream()
                    .filter(log -> !Instant.parse(log.getWhen()).isBefore(finalFromts))
                    .collect(Collectors.toList());
            activityLogs = activityLogs.stream()
                    .filter(log -> !Instant.parse(log.getWhen()).isBefore(finalFromts))
                    .collect(Collectors.toList());
        } else if (tots != null) {
            changeLogs = changeLogs.stream()
                    .filter(log -> !Instant.parse(log.getWhen()).isAfter(finalTots))
                    .collect(Collectors.toList());
            activityLogs = activityLogs.stream()
                    .filter(log -> !Instant.parse(log.getWhen()).isAfter(finalTots))
                    .collect(Collectors.toList());
        } else if (ndays > 0) {
            Instant end = Instant.now();
            Instant start = end.minusSeconds(ndays * 86400L);
            changeLogs = changeLogs.stream()
                    .filter(log -> {
                        Instant logInstant = Instant.parse(log.getWhen());
                        return !logInstant.isBefore(start) && !logInstant.isAfter(end);
                    })
                    .collect(Collectors.toList());
            activityLogs = activityLogs.stream()
                    .filter(log -> {
                        Instant logInstant = Instant.parse(log.getWhen());
                        return !logInstant.isBefore(start) && !logInstant.isAfter(end);
                    })
                    .collect(Collectors.toList());
        }

        // Combine logs
        List<LogEntry> combinedLogs = new ArrayList<>();
        combinedLogs.addAll(changeLogs);
        combinedLogs.addAll(activityLogs);

        // optional Filter by who if provided
        if (who != null && !who.isEmpty()) {
            combinedLogs = combinedLogs.stream().filter(log -> who.equals(log.getWho())).collect(Collectors.toList());
        }

        // optional Filter by log priority if provided
        if (pri != null) {
            combinedLogs = combinedLogs.stream().filter(log -> pri.equals(log.getPri())).collect(Collectors.toList());
        }
        // optional Filter by remoteIP
        if (remoteIP != null && !remoteIP.isEmpty()) {
            combinedLogs = combinedLogs.stream().filter(log -> remoteIP.equals(log.getRemoteIP()))
                    .collect(Collectors.toList());
        }
        // optional filter by operation type
        if (op != null && !op.isEmpty()) {
            combinedLogs = combinedLogs.stream().filter(log -> op.equals(log.getOp())).collect(Collectors.toList());
        }

        int totalLogs = combinedLogs.size();
        int start = 0 * maxRecord;
        int end = Math.min(start + maxRecord, totalLogs);

        List<LogEntry> paginatedLogs = combinedLogs.subList(start, end);
        // Create and return the response
        GetLogsResponse response = new GetLogsResponse(paginatedLogs, combinedLogs.size(), null);
        return response;

    }

    public List<LogEntry> getSetlogs(LogharbourRequestBo logharbourRequestBo) throws Exception {

		try {
			System.out.println("my first request data here !!!!!!!!!");
			if (logharbourRequestBo.getQueryToken() == null && logharbourRequestBo.getQueryToken().isEmpty()) {
				throw new IllegalArgumentException("query token can not pass null or empty");
			}
			return this.processSearch(logharbourRequestBo,logharbourRequestBo.getSetAttr());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return this.processSearch(logharbourRequestBo,logharbourRequestBo.getSetAttr());
	}	

	@Autowired
	  private ElasticsearchOperations elasticsearchOperations;	
		  public List<LogEntry> processSearch(LogharbourRequestBo logharbourRequestBo,String getsetAttr) {

			  
			  try {
				  BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
				  
			        if (logharbourRequestBo.getApp() != null && !logharbourRequestBo.getApp().isEmpty()) {
			            boolQueryBuilder.must(MatchQuery.of(m -> m.field(LogharbourConstants.APP).query("Kra"))._toQuery());
			        }
			        if (logharbourRequestBo.getWho() != null && !logharbourRequestBo.getWho().isEmpty()) {
			            boolQueryBuilder.must(MatchQuery.of(m -> m.field(LogharbourConstants.WHO).query("User3"))._toQuery());
			        }
			        if (logharbourRequestBo.getClassName() != null && !logharbourRequestBo.getClassName().isEmpty()) {
			            boolQueryBuilder.must(MatchQuery.of(m -> m.field(LogharbourConstants.CLASS_NAME).query(logharbourRequestBo.getClassName()))._toQuery());
			        }
			        if (logharbourRequestBo.getOp() != null && !logharbourRequestBo.getOp().isEmpty()) {
			            boolQueryBuilder.must(MatchQuery.of(m -> m.field(LogharbourConstants.OP).query(logharbourRequestBo.getOp()))._toQuery());
			        }
			        if (logharbourRequestBo.getRemoteIP() != null && !logharbourRequestBo.getRemoteIP().isEmpty()) {
			            boolQueryBuilder.must(MatchQuery.of(m -> m.field(LogharbourConstants.REMOTE_IP).query(logharbourRequestBo.getRemoteIP()))._toQuery());
			        } 
			        if (logharbourRequestBo.getFromTs() != null) {
			            boolQueryBuilder.must(MatchQuery.of(m -> m.field(LogharbourConstants.WHEN).query(logharbourRequestBo.getFromTs().toString()))._toQuery());
			        }
			        if (logharbourRequestBo.getToTs() != null) {
			            boolQueryBuilder.must(MatchQuery.of(m -> m.field(LogharbourConstants.WHEN).query(logharbourRequestBo.getToTs().toString()))._toQuery());
			        }
//			        if (logharbourRequestBo.getNDays() == null) {
//			            boolQueryBuilder.must(MatchQuery.of(m -> m.field("who").query(logharbourRequestBo.getNDays()))._toQuery());
//			        }
				    if (logharbourRequestBo.getInstance() != null) {
			            boolQueryBuilder.must(MatchQuery.of(m -> m.field(LogharbourConstants.INSTANCE_ID).query(logharbourRequestBo.getInstance()))._toQuery());
			        } 
			        if (logharbourRequestBo.getType() != null && !logharbourRequestBo.getType().isEmpty()) {
			            boolQueryBuilder.must(MatchQuery.of(m -> m.field(LogharbourConstants.LOG_TYPE).query(logharbourRequestBo.getType()))._toQuery());
			        }
		/*********************************************************************************************************************/
//				  Query query = NativeQuery.builder()
//			                .withQuery(boolQueryBuilder.build()._toQuery())
//			                .build();
//				  			  
//						SearchHits<LogEntry> searchHits = elasticsearchOperations.search(query, LogEntry.class);
//						List<LogEntry> loggerList = searchHits.getSearchHits()
//						    .stream()
//						    .map(SearchHit::getContent)
//						    .toList();
//				  return loggerList;
				  
		/*********************************************************************************************************************/
			        
			        // Create aggregation based on getsetAttr with .keyword field
			        Aggregation aggregation = Aggregation.of(a -> a
			            .terms(t -> t.field(getsetAttr + ".keyword").size(10))
			        );

			       
			        Query query = NativeQuery.builder()
			                .withQuery(boolQueryBuilder.build()._toQuery())
			                .withAggregation("agg", aggregation)
			                .build();

			        SearchHits<LogEntry> searchHits = elasticsearchOperations.search(query, LogEntry.class);

//			        // Process aggregation results
//			        Aggregations aggregations = searchHits.getAggregations();
//			        if (aggregations != null && aggregations.get(getsetAttr) != null) {
//			            // Process the aggregation results as needed
//			        }

			        List<LogEntry> loggerList = searchHits.getSearchHits()
			                .stream()
			                .map(SearchHit::getContent)
			                .toList();
			        return loggerList;
			        
			        
				  
				  }catch (Exception ex) {
					ex.printStackTrace();
				}
				  return null;
			  
		  }
	
//	// Pattern for attribute validation
//    private static final Pattern PATTERN = Pattern.compile("^[a-z]{1,9}$");
//
//    // Allowed attributes
//    private static final Map<String, Boolean> ALLOWED_ATTRIBUTES = new HashMap<>();
//
//    static {
//        ALLOWED_ATTRIBUTES.put("app", true);
//        ALLOWED_ATTRIBUTES.put("typeConst", true);
//        ALLOWED_ATTRIBUTES.put("op", true);
//        ALLOWED_ATTRIBUTES.put("instance", true);
//        ALLOWED_ATTRIBUTES.put("class", true);
//        ALLOWED_ATTRIBUTES.put("module", true);
//        ALLOWED_ATTRIBUTES.put("pri", true);
//        ALLOWED_ATTRIBUTES.put("status", true);
//        ALLOWED_ATTRIBUTES.put("remote_ip", true);
//        ALLOWED_ATTRIBUTES.put("system", true);
//        ALLOWED_ATTRIBUTES.put("who", true);
//        ALLOWED_ATTRIBUTES.put("field", true);
//    }
    


}
