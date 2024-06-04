package com.remiges.logharbour.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class Constants {

    @Value("${kafka.topic}")
    private String kafkaTopic;

    @Value("${elasticsearch.username}")
    private String elasticUsername;

    @Value("${elasticsearch.password}")
    private String elasticPassword;

    @Value("${elasticsearch.host}")
    private String elasticsearchHost;

    @Value("${elasticsearch.port}")
    private int elasticsearchPort;

    @Value("${elasticsearch.scheme}")
    private String elasticsearchScheme;

    @Value("${logharbour.max.record}")
    private Integer logharbourMaxRecord;

}
