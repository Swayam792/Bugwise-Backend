package com.swayam.bugwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.swayam.bugwise.repository.jpa")
@EnableElasticsearchRepositories(basePackages = "com.swayam.bugwise.repository.elasticsearch")
@EnableTransactionManagement
public class BugwiseApplication {

	public static void main(String[] args) {
		SpringApplication.run(BugwiseApplication.class, args);
	}

}
