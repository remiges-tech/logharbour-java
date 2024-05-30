package com.remiges.logharbour.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.remiges.logharbour.model.LogPriorityLevels;
import com.remiges.logharbour.model.LoggerContext;

@Configuration
public class LoggerContextConfig {

    @Bean
    public LoggerContext loggerContext() {
        return new LoggerContext(LogPriorityLevels.INFO);
    }

}
