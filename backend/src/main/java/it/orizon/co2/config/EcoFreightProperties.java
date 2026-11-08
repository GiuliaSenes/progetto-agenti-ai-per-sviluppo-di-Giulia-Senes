package it.orizon.co2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecofreight")
public record EcoFreightProperties(String baseUrl, String apiKey) {}
