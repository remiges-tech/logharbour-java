package com.remiges.logharbour.service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import com.remiges.logharbour.model.LogEntry;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
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
            boBuilder.must(MatchPhraseQuery.of(m -> m.field("app").query(app))._toQuery());
        }
        if (who != null && !who.isEmpty()) {
            boBuilder.must(MatchPhraseQuery.of(m -> m.field("who").query(who))._toQuery());
        }
        if (className != null && !className.isEmpty()) {
            boBuilder.must(MatchPhraseQuery.of(m -> m.field("className").query(className))._toQuery());
        }
        if (instance != null && !instance.isEmpty()) {
            boBuilder.must(MatchPhraseQuery.of(m -> m.field("instanceId").query(instance))._toQuery());
        }
        if (op != null && !op.isEmpty()) {
            boBuilder.must(MatchPhraseQuery.of(m -> m.field("op").query(op))._toQuery());
        }

        // MatchQuery.of(m -> m.field("app").query(""))._toQuery();

        Query query = NativeQuery.builder().withQuery(boBuilder.build()._toQuery()).build();

        SearchHits<LogEntry> searchHits = elasticsearchOperations.search(query, LogEntry.class);

        return searchHits;
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
     * @param remoteIP         Remote IP address (currently not used in the query).
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
            String remoteIP, LogEntry.LogPriority pri, String searchAfterTs,
            String searchAfterDocId) {

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // Mandatory fields
        boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("app").query(app))._toQuery());
        boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("className").query(className))._toQuery());
        boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("instanceId").query(instance))._toQuery());
        // Ensure logType is "CHANGE"
        boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("logType").query("CHANGE"))._toQuery());

        // Optional fields
        if (who != null && !who.isEmpty()) {
            boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("who").query(who))._toQuery());
        }

        if (op != null && !op.isEmpty()) {
            boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("op").query(op))._toQuery());
        }

        if (remoteIP != null && !remoteIP.isEmpty()) {
            boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("remoteIP").query(remoteIP))._toQuery());
        }

        if (field != null && !field.isEmpty()) {
            boolQueryBuilder
                    .must(MatchPhraseQuery.of(m -> m.field("data.changeData.changes.field").query(field))._toQuery());
        }

        // Handle timestamp ranges
        final Instant fromts;
        final Instant tots;

        try {
            fromts = (fromtsStr != null && !fromtsStr.isEmpty()) ? Instant.parse(fromtsStr) : null;
            tots = (totsStr != null && !totsStr.isEmpty()) ? Instant.parse(totsStr) : null;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid timestamp format. Please provide timestamps in ISO 8601 format.");
        }

        // Inter-dependency logic
        if (fromts != null && tots != null) {
            if (fromts.isAfter(tots)) {
                throw new IllegalArgumentException("fromts must be before tots");
            }
            boolQueryBuilder.must(RangeQuery
                    .of(r -> r.field("when").gte(JsonData.of(fromts.toString())).lte(JsonData.of(tots.toString())))
                    ._toQuery());
        } else if (fromts != null) {
            boolQueryBuilder.must(RangeQuery.of(r -> r.field("when").gte(JsonData.of(fromts.toString())))._toQuery());
        } else if (tots != null) {
            boolQueryBuilder.must(RangeQuery.of(r -> r.field("when").lte(JsonData.of(tots.toString())))._toQuery());
        } else if (ndays > 0) {
            Instant end = Instant.now();
            Instant start = end.minusSeconds(ndays * 86400L);
            boolQueryBuilder.must(RangeQuery
                    .of(r -> r.field("when").gte(JsonData.of(start.toString())).lte(JsonData.of(end.toString())))
                    ._toQuery());
        }

        Query query = NativeQuery.builder().withQuery(boolQueryBuilder.build()._toQuery()).build();

        SearchHits<LogEntry> searchHits = elasticsearchOperations.search(query, LogEntry.class);

        return searchHits;
    }

}
