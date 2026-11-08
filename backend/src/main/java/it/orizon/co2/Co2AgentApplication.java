package it.orizon.co2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Co2AgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(Co2AgentApplication.class, args);
    }
}
