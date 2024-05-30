package com.remiges.logharbour.service;


import java.util.ArrayList;
import java.util.List;

import javax.management.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;

@Service
public class ElasticQueryServices {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public BaseQueryBuilder getQueryForLogs() {

        BoolQuery.Builder boBuilder = new BoolQuery.Builder();
        List<Query> queries = new ArrayList<>();

       // MatchQueryBuilder appMatchQuery = QueryBuilders.matchQuery("app", appValue);

        MatchQuery.of(m -> m.field("app").query(""))._toQuery();
        MatchQuery.of(m -> m.field("app").query(""))._toQuery();
       // queries.add(MatchQuery.of(m -> m.field("app").query(""))._toQuery());

     // boBuilder.must(null);
        return null;
    }

}
