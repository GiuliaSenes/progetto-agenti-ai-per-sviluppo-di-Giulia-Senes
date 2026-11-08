package it.orizon.co2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(String baseUrl, String apiKey, String model, int historyLimit) {}
