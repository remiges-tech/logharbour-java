package com.remiges.logharbour.config;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;

@Configuration
public class ElasticSearchConfig {

        @Autowired
        private Constants constants;

        @Bean
        public ElasticsearchClient elasticsearchClient() throws Exception {
                final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                                new UsernamePasswordCredentials(constants.getElasticUsername(),
                                                constants.getElasticPassword()));

                SSLContext sslContext = SSLContexts.custom()
                                .loadTrustMaterial((chain, authType) -> true) // Trust self-signed certificates
                                .build();

                RestClient restClient = RestClient.builder(
                                new HttpHost(constants.getElasticsearchHost(), constants.getElasticsearchPort(),
                                                constants.getElasticsearchScheme()))
                                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                                                .setDefaultCredentialsProvider(credentialsProvider)
                                                .setSSLContext(sslContext))
                                .build();

                RestClientTransport transport = new RestClientTransport(
                                restClient,
                                new co.elastic.clients.json.jackson.JacksonJsonpMapper());

                return new ElasticsearchClient(transport);
        }

        @Bean
        public ElasticsearchTemplate elasticsearchTemplate(ElasticsearchClient elasticsearchClient) {
                return new ElasticsearchTemplate(elasticsearchClient);
        }

}
