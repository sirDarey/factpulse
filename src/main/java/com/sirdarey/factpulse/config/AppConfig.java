package com.sirdarey.factpulse.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    public static final String APOLOGY_RESPONSE =
            "I apologize, but I encountered an error processing your request. Please try again later.";

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}