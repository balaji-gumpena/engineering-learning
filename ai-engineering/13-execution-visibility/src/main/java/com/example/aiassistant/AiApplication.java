package com.example.aiassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
public class AiApplication {
    public static void main(String[] arguments) {
        SpringApplication.run(AiApplication.class, arguments);
    }
}
