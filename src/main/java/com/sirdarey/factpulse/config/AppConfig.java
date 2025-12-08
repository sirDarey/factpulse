package com.sirdarey.factpulse.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirdarey.factpulse.service.OpenAIService;
import com.sirdarey.factpulse.util.PromptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
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
    public CommandLineRunner commandLineRunner(OpenAIService openAIService) {
        return args -> {
            log.info("\n\nANALYZING SYSTEM PROMPT....");
            openAIService.analyzePrompt(PromptUtil.SYSTEM_PROMPT);
            log.info("\n\nDONE ANALYZING SYSTEM PROMPT\n\n READY TO GO :)");
        };
    }
}