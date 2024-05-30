package com.remiges.logharbour;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@EnableElasticsearchRepositories
@SpringBootApplication(exclude = { ElasticsearchDataAutoConfiguration.class })
public class LogharbourApplication {

	public static void main(String[] args) {
		SpringApplication.run(LogharbourApplication.class, args);

	}

}
