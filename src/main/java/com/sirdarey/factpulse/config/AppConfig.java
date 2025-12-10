package com.sirdarey.factpulse.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class AppConfig {

    public static final String APOLOGY_RESPONSE =
            "❗I apologize, an error occurred while processing your request. Please try again later 😊";

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // Set pool size > 1 so multiple users can have tasks run at the exact same second
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("fact-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}