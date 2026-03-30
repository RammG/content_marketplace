package com.tianzige.marketplace.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.tianzige.marketplace.repository")
public class ElasticsearchConfig {
}
