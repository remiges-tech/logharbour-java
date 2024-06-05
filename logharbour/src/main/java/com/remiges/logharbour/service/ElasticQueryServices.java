package com.remiges.logharbour.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import com.remiges.logharbour.constant.LogharbourConstants;
import com.remiges.logharbour.exception.InvalidTimestampRangeException;
import com.remiges.logharbour.model.LogEntry;
import com.remiges.logharbour.model.request.LogharbourRequestBo;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;

@Service
public class ElasticQueryServices {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    /**
     * Retrieves log entries from Elasticsearch based on the provided query
     * parameters.
     *
     * @param queryToken       The query token for authentication or identification.
     * @param app              The application name to filter logs.
     * @param who              The user identifier to filter logs.
     * @param className        The class name to filter logs.
     * @param instance         The instance identifier to filter logs.
     * @param op               The operation type to filter logs.
     * @param fromtsStr        The start timestamp for the time range filter.
     * @param totsStr          The end timestamp for the time range filter.
     * @param ndays            The number of days to filter logs from the current
     *                         time.
     * @param logType          The log type (e.g., "A" for activity, "C" for
     *                         change).
     * @param remoteIP         The remote IP address to filter logs.
     * @param pri              The log priority to filter logs.
     * @param searchAfterTs    The timestamp for pagination to fetch logs after the
     *                         specified time.
     * @param searchAfterDocId The document ID for pagination to fetch logs after
     *                         the specified document.
     * @return A {@link SearchHits} object containing the search results for log
     *         entries.
     * @throws IllegalArgumentException If the end timestamp is before the start
     *                                  timestamp.
     */
    public SearchHits<LogEntry> getQueryForLogs(String queryToken, String app, String who,
            String className, String instance,
            String op, String fromtsStr, String totsStr, int ndays, String logType,
            String remoteIP, LogEntry.LogPriority pri, String searchAfterTs,
            String searchAfterDocId) {

        boolean totsReverse = false;

        BoolQuery.Builder boBuilder = new BoolQuery.Builder();

        addMatchPhraseQuery(boBuilder, LogharbourConstants.APP, app);
        addMatchPhraseQuery(boBuilder, LogharbourConstants.WHO, who);
        addMatchPhraseQuery(boBuilder, LogharbourConstants.CLASS_NAME, className);
        addMatchPhraseQuery(boBuilder, LogharbourConstants.INSTANCE_ID, instance);
        addMatchPhraseQuery(boBuilder, LogharbourConstants.OP, op);
        addMatchPhraseQuery(boBuilder, LogharbourConstants.REMOTE_IP, remoteIP);

        if (logType != null && !logType.isEmpty()) {
            if (logType.equals("A")) {

                boBuilder.must(
                        MatchPhraseQuery.of(m -> m.field(LogharbourConstants.LOG_TYPE).query("ACTIVITY"))._toQuery());
            } else if (logType.equals("C")) {
                boBuilder.must(
                        MatchPhraseQuery.of(m -> m.field(LogharbourConstants.LOG_TYPE).query("CHANGE"))._toQuery());
            } else if (logType.equals("D")) {
                boBuilder.must(
                        MatchPhraseQuery.of(m -> m.field(LogharbourConstants.LOG_TYPE).query("DEBUG"))._toQuery());

            } else {
                return null;
            }
        }

        if (pri != null) {
            boBuilder.must(
                    MatchPhraseQuery.of(m -> m.field(LogharbourConstants.PRIORITY).query(pri.toString()))._toQuery());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime fromts = fromtsStr != null && !fromtsStr.isEmpty() ? LocalDateTime.parse(fromtsStr, formatter)
                : null;
        LocalDateTime tots = totsStr != null && !totsStr.isEmpty() ? LocalDateTime.parse(totsStr, formatter) : null;

        addTimestampRangeQuery(boBuilder, fromtsStr, totsStr, ndays);
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
            totsReverse = true;
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
        if (totsReverse) {
            return elasticsearchOperations.search(
                NativeQuery.builder().withQuery(boBuilder.build()._toQuery())
                        .withSort(s -> s.field(f -> f.field(LogharbourConstants.WHEN).order(SortOrder.Desc))).build(),
                LogEntry.class);

        }
        return elasticsearchOperations.search(
                NativeQuery.builder().withQuery(boBuilder.build()._toQuery())
                        .withSort(s -> s.field(f -> f.field(LogharbourConstants.WHEN).order(SortOrder.Asc))).build(),
                LogEntry.class);
    }

    /**
     * Generates and executes an Elasticsearch query to retrieve log entries based
     * on
     * various criteria such as application, class, instance, and optional filters
     * like user, operation, time range, and specific fields.
     *
     * @param queryToken Query token for the realm (currently not used in the
     *                   query).
     * @param app        The application name (mandatory).
     * @param className  The class name of the object (mandatory).
     * @param instance   The instance ID of the object (mandatory).
     * @param who        The user who made the changes (optional).
     * @param op         The operation performed (optional).
     * @param fromtsStr  Start of the time range (ISO 8601 format, optional).
     * @param totsStr    End of the time range (ISO 8601 format, optional).
     * @param ndays      Number of days back from the current time to consider
     *                   (optional).
     * @param field      Specific field to filter changes (optional).
     * @param remoteIP   Remote IP address (optional).
     * @throws IllegalArgumentException If the provided timestamps are in an invalid
     *                                  format or
     *                                  if fromts is after tots.
     */

    public SearchHits<LogEntry> getQueryForChangeLogs(String queryToken, String app,
            String className, String instance, String who,
            String op, String fromtsStr, String totsStr, int ndays, String field,
            String remoteIP) {

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field(LogharbourConstants.APP).query(app))._toQuery());
        boolQueryBuilder
                .must(MatchPhraseQuery.of(m -> m.field(LogharbourConstants.CLASS_NAME).query(className))._toQuery());
        boolQueryBuilder
                .must(MatchPhraseQuery.of(m -> m.field(LogharbourConstants.INSTANCE_ID).query(instance))._toQuery());
        boolQueryBuilder
                .must(MatchPhraseQuery.of(m -> m.field(LogharbourConstants.LOG_TYPE).query("CHANGE"))._toQuery());

        addMatchPhraseQuery(boolQueryBuilder, LogharbourConstants.WHO, who);
        addMatchPhraseQuery(boolQueryBuilder, LogharbourConstants.OP, op);
        addMatchPhraseQuery(boolQueryBuilder, LogharbourConstants.REMOTE_IP, remoteIP);
        addMatchPhraseQuery(boolQueryBuilder, "data.changeData.changes.field", field);

        addTimestampRangeQuery(boolQueryBuilder, fromtsStr, totsStr, ndays);

        return elasticsearchOperations.search(
                NativeQuery.builder().withQuery(boolQueryBuilder.build()._toQuery())
                        .withSort(s -> s.field(f -> f.field(LogharbourConstants.WHEN).order(SortOrder.Asc))).build(),
                LogEntry.class);

    }

    public SearchHits<LogEntry> getQueryForGetSetLogs(LogharbourRequestBo logharbourRequestBo) {

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        this.appendQuery(boolQueryBuilder, LogharbourConstants.APP, logharbourRequestBo.getApp());
        this.appendQuery(boolQueryBuilder, LogharbourConstants.WHO, logharbourRequestBo.getWho());
        this.appendQuery(boolQueryBuilder, LogharbourConstants.CLASS_NAME, logharbourRequestBo.getClassName());
        this.appendQuery(boolQueryBuilder, LogharbourConstants.OP, logharbourRequestBo.getOp());
        this.appendQuery(boolQueryBuilder, LogharbourConstants.REMOTE_IP, logharbourRequestBo.getRemoteIP());
        this.appendQuery(boolQueryBuilder, LogharbourConstants.INSTANCE_ID, logharbourRequestBo.getInstance());
        this.appendQuery(boolQueryBuilder, LogharbourConstants.LOG_TYPE, logharbourRequestBo.getType());

        this.addRangeQuery(boolQueryBuilder, LogharbourConstants.WHEN, logharbourRequestBo.getFromTs(),
                logharbourRequestBo.getToTs());

        Query query = NativeQuery.builder().withQuery(boolQueryBuilder.build()._toQuery()).build();
        return elasticsearchOperations.search(query, LogEntry.class);

    }

    /**
     * Appends a query clause to a BoolQuery builder based on the provided field and
     * value.
     *
     * @param boolQueryBuilder The BoolQuery builder to append the query to.
     * @param field            The field to query.
     * @param value            The value to match.
     */
    private void appendQuery(BoolQuery.Builder boolQueryBuilder, String field, String value) {
        if (value != null && field != null) {
            boolQueryBuilder.must(MatchQuery.of(m -> m.field(field).query(value))._toQuery());
        }
    }

    /**
     * Adds a range query clause to a BoolQuery builder based on the provided field,
     * from, and to values.
     *
     * @param boolQueryBuilder The BoolQuery builder to add the range query to.
     * @param field            The field to apply the range query on.
     * @param from             The lower bound of the range.
     * @param to               The upper bound of the range.
     */
    private void addRangeQuery(BoolQuery.Builder boolQueryBuilder, String field, Object from, Object to) {
        if (from != null) {
            boolQueryBuilder.must(RangeQuery.of(r -> r.field(field).gte(JsonData.of(from)))._toQuery());
        }
        if (to != null) {
            boolQueryBuilder.must(RangeQuery.of(r -> r.field(field).lte(JsonData.of(to)))._toQuery());
        }
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
