package com.remiges.logharbour.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import com.remiges.logharbour.constant.LogharbourConstants;
import com.remiges.logharbour.exception.InvalidTimestampRangeException;
import com.remiges.logharbour.model.LogEntry;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;

@Service
public class ElasticQueryServices {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public SearchHits<LogEntry> getQueryForLogs(String queryToken, String app, String who,
            String className, String instance,
            String op, String fromtsStr, String totsStr, int ndays, String logType,
            String remoteIP, LogEntry.LogPriority pri, String searchAfterTs,
            String searchAfterDocId) {

        BoolQuery.Builder boBuilder = new BoolQuery.Builder();

        if (app != null && !app.isEmpty()) {
            boBuilder.must(MatchPhraseQuery.of(m -> m.field(LogharbourConstants.APP).query(app))._toQuery());
        }
        if (who != null && !who.isEmpty()) {
            boBuilder.must(MatchPhraseQuery.of(m -> m.field(LogharbourConstants.WHO).query(who))._toQuery());
        }
        if (className != null && !className.isEmpty()) {
            boBuilder.must(
                    MatchPhraseQuery.of(m -> m.field(LogharbourConstants.CLASS_NAME).query(className))._toQuery());
        }
        if (instance != null && !instance.isEmpty()) {
            boBuilder.must(
                    MatchPhraseQuery.of(m -> m.field(LogharbourConstants.INSTANCE_ID).query(instance))._toQuery());
        }
        if (op != null && !op.isEmpty()) {
            boBuilder.must(MatchPhraseQuery.of(m -> m.field(LogharbourConstants.OP).query(op))._toQuery());
        }

        if (remoteIP != null && !remoteIP.isEmpty()) {
            boBuilder.must(MatchPhraseQuery.of(m -> m.field(LogharbourConstants.REMOTE_IP).query(remoteIP))._toQuery());
        }
        if (logType != null && !logType.isEmpty()) {
            boBuilder.must(MatchPhraseQuery.of(m -> m.field(LogharbourConstants.LOG_TYPE).query(logType))._toQuery());
        }
        if (pri != null) {
            boBuilder.must(
                    MatchPhraseQuery.of(m -> m.field(LogharbourConstants.PRIORITY).query(pri.toString()))._toQuery());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime fromts = fromtsStr != null && !fromtsStr.isEmpty() ? LocalDateTime.parse(fromtsStr, formatter)
                : null;
        LocalDateTime tots = totsStr != null && !totsStr.isEmpty() ? LocalDateTime.parse(totsStr, formatter) : null;

        if (fromts != null && tots != null) {
            if (tots.isAfter(fromts)) {
                boBuilder.must(RangeQuery.of(
                        m -> m.field(LogharbourConstants.WHEN).gte(JsonData.of(fromtsStr)).lte(JsonData.of(totsStr)))
                        ._toQuery());
            } else {
                throw new IllegalArgumentException("tots must be after fromts");
            }
        } else if (fromts != null) {
            boBuilder.must(RangeQuery.of(
                    m -> m.field(LogharbourConstants.WHEN).gte(JsonData.of(fromtsStr)))
                    ._toQuery());
        } else if (tots != null) {
            boBuilder.must(RangeQuery.of(
                    m -> m.field(LogharbourConstants.WHEN).lte(JsonData.of(totsStr)))
                    ._toQuery());
        } else if (ndays > 0) {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusDays(ndays);

            boBuilder.must(RangeQuery.of(
                    m -> m.field(LogharbourConstants.WHEN).gte(JsonData.of(start.toString()))
                            .lte(JsonData.of(end.toString())))
                    ._toQuery());
        }

        if (searchAfterTs != null && !searchAfterTs.isEmpty()) {

            boBuilder.must(RangeQuery.of(
                    m -> m.field(LogharbourConstants.WHEN).gt(JsonData.of(searchAfterTs)))
                    ._toQuery());

        }
        Query query = NativeQuery.builder().withQuery(boBuilder.build()._toQuery())
                .withSort(s -> s.field(f -> f.field(LogharbourConstants.WHEN).order(SortOrder.Asc)))
                .build();

        return elasticsearchOperations.search(query, LogEntry.class);
    }

    /**
     * Generates and executes an Elasticsearch query to retrieve log entries based
     * on
     * various criteria such as application, class, instance, and optional filters
     * like user, operation, time range, and specific fields.
     *
     * @param queryToken       Query token for the realm (currently not used in the
     *                         query).
     * @param app              The application name (mandatory).
     * @param className        The class name of the object (mandatory).
     * @param instance         The instance ID of the object (mandatory).
     * @param who              The user who made the changes (optional).
     * @param op               The operation performed (optional).
     * @param fromtsStr        Start of the time range (ISO 8601 format, optional).
     * @param totsStr          End of the time range (ISO 8601 format, optional).
     * @param ndays            Number of days back from the current time to consider
     *                         (optional).
     * @param field            Specific field to filter changes (optional).
     * @param remoteIP         Remote IP address (optional).
     * @param pri              Log priority (currently not used in the query).
     * @param searchAfterTs    Timestamp for pagination (currently not used in the
     *                         query).
     * @param searchAfterDocId Document ID for pagination (currently not used in the
     *                         query).
     * @return SearchHits object containing the search results.
     * @throws IllegalArgumentException If the provided timestamps are in an invalid
     *                                  format or
     *                                  if fromts is after tots.
     */

    public SearchHits<LogEntry> getQueryForChangeLogs(String queryToken, String app,
            String className, String instance, String who,
            String op, String fromtsStr, String totsStr, int ndays, String field,
            String remoteIP) {

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // Mandatory fields
        boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field(LogharbourConstants.APP).query(app))._toQuery());
        boolQueryBuilder
                .must(MatchPhraseQuery.of(m -> m.field(LogharbourConstants.CLASS_NAME).query(className))._toQuery());
        boolQueryBuilder
                .must(MatchPhraseQuery.of(m -> m.field(LogharbourConstants.INSTANCE_ID).query(instance))._toQuery());
        // Ensure logType is "CHANGE"
        boolQueryBuilder
                .must(MatchPhraseQuery.of(m -> m.field(LogharbourConstants.LOG_TYPE).query("CHANGE"))._toQuery());

        // Optional fields
        addMatchPhraseQuery(boolQueryBuilder, LogharbourConstants.WHO, who);
        addMatchPhraseQuery(boolQueryBuilder, LogharbourConstants.OP, op);
        addMatchPhraseQuery(boolQueryBuilder, LogharbourConstants.REMOTE_IP, remoteIP);
        addMatchPhraseQuery(boolQueryBuilder, "data.changeData.changes.field", field);

        // Handle timestamp ranges
        addTimestampRangeQuery(boolQueryBuilder, fromtsStr, totsStr, ndays);

        Query query = NativeQuery.builder().withQuery(boolQueryBuilder.build()._toQuery())
                .withSort(s -> s.field(f -> f.field(LogharbourConstants.WHEN).order(SortOrder.Asc))).build();

        return elasticsearchOperations.search(query, LogEntry.class);

    }

    /**
     * Adds a match phrase query to the provided BoolQuery.Builder if the given
     * value is not null or empty.
     *
     * @param boolQueryBuilder the BoolQuery.Builder to add the match phrase query
     *                         to
     * @param field            the name of the field to match
     * @param value            the value to match against the specified field
     */
    private void addMatchPhraseQuery(BoolQuery.Builder boolQueryBuilder, String field, String value) {
        if (value != null && !value.isEmpty()) {
            boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field(field).query(value))._toQuery());
        }
    }

    /**
     * Adds a timestamp range query to the provided BoolQuery.Builder based on the
     * given timestamps or number of days.
     * 
     * @param boolQueryBuilder the BoolQuery.Builder to add the timestamp range
     *                         query to
     * @param fromtsStr        the start timestamp string (ISO 8601 format) or null
     *                         if not provided
     * @param totsStr          the end timestamp string (ISO 8601 format) or null if
     *                         not provided
     * @param ndays            the number of days to consider for the timestamp
     *                         range if both start and end timestamps are not
     *                         provided
     * @throws IllegalArgumentException       if the provided timestamps are not in
     *                                        the ISO 8601 format
     * @throws InvalidTimestampRangeException if the start timestamp is after the
     *                                        end timestamp
     */
    private void addTimestampRangeQuery(BoolQuery.Builder boolQueryBuilder, String fromtsStr, String totsStr,
            int ndays) {
        final Instant fromts;
        final Instant tots;

        try {
            fromts = (fromtsStr != null && !fromtsStr.isEmpty()) ? Instant.parse(fromtsStr) : null;
            tots = (totsStr != null && !totsStr.isEmpty()) ? Instant.parse(totsStr) : null;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid timestamp format. Please provide timestamps in ISO 8601 format.");
        }

        if (fromts != null && tots != null) {
            if (fromts.isAfter(tots)) {
                throw new InvalidTimestampRangeException("fromts must be before tots");
            }
            boolQueryBuilder.must(RangeQuery
                    .of(r -> r.field(LogharbourConstants.WHEN).gte(JsonData.of(fromts.toString()))
                            .lte(JsonData.of(tots.toString())))
                    ._toQuery());
        } else if (fromts != null) {
            boolQueryBuilder.must(RangeQuery
                    .of(r -> r.field(LogharbourConstants.WHEN).gte(JsonData.of(fromts.toString())))._toQuery());
        } else if (tots != null) {
            boolQueryBuilder.must(
                    RangeQuery.of(r -> r.field(LogharbourConstants.WHEN).lte(JsonData.of(tots.toString())))._toQuery());
        } else if (ndays > 0) {
            Instant end = Instant.now();
            Instant start = end.minusSeconds(ndays * 86400L);
            boolQueryBuilder.must(RangeQuery
                    .of(r -> r.field(LogharbourConstants.WHEN).gte(JsonData.of(start.toString()))
                            .lte(JsonData.of(end.toString())))
                    ._toQuery());
        }
    }

}
