package com.remiges.logharbour.service;

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

    public SearchHits<LogEntry> getQueryForChangeLogs(String queryToken, String app,
            String className, String instance, String who,
            String op, String fromtsStr, String totsStr, int ndays, String field, String logType,
            String remoteIP, LogEntry.LogPriority pri, String searchAfterTs,
            String searchAfterDocId) {

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // Mandatory fields
        boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("app").query(app))._toQuery());
        boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("className").query(className))._toQuery());
        boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("instanceId").query(instance))._toQuery());

        // Optional fields
        if (who != null && !who.isEmpty()) {
            boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("who").query(who))._toQuery());
        }
        if (op != null && !op.isEmpty()) {
            boolQueryBuilder.must(MatchPhraseQuery.of(m -> m.field("op").query(op))._toQuery());
        }

        Query query = NativeQuery.builder().withQuery(boolQueryBuilder.build()._toQuery()).build();

        SearchHits<LogEntry> searchHits = elasticsearchOperations.search(query, LogEntry.class);

        return searchHits;
    }

}
