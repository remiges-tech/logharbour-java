package com.remiges.logharbour.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class Constants {

    @Value("${kafka.topic}")
    private String kafkaTopic;

}
