package com.remiges.logharbour.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.remiges.logharbour.model.LoggerContext;
import com.remiges.logharbour.model.LogEntry.LogPriority;

@Configuration
public class LoggerContextConfig {

    @Bean
    public LoggerContext loggerContext() {
        return new LoggerContext(LogPriority.INFO);
    }

}
