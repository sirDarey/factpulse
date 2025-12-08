package com.sirdarey.factpulse.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirdarey.factpulse.config.AppConfig;
import com.sirdarey.factpulse.model.WelcomeMessageModel;
import com.sirdarey.factpulse.util.PromptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final ObjectMapper objectMapper;
    private final OpenAiChatModel chatModel;

    private final String[] bannedWords = {
            "fuck", "shit", "bastard", "idiot", "asshole",
            "nigger", "hoe", "slut", "dick", "goat", "monkey"
    };


    /**
     * Takes in a natural-language prompt, sends it to the AI model,
     * and returns the model's response as plain text.
     */
    public String analyzePrompt(String prompt) {
        log.info("AI-analyze :: prompt :: {}", prompt);
        Prompt aiPrompt = new Prompt(prompt);

        ChatResponse chatResponse = chatModel.call(aiPrompt);
        String response;

        if (chatResponse == null
                || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null) {

            response = AppConfig.APOLOGY_RESPONSE;
        } else {
            response = chatResponse.getResult().getOutput().getText();
        }

        log.info("AI-analyze :: response :: {}", response);
        return response;
    }


    /**
     * Extracts the user's name from their message or generates a personalized welcome message when no name is provided.
     * Always returns a JSON response containing both the welcomeMessage and the sanitized name
     */
    public WelcomeMessageModel welcomeUser(String userMessage) throws JsonProcessingException {
        log.info("extractUserNameOrAsk for :: {}", userMessage);

        UserMessage message = new UserMessage(PromptUtil.WELCOME_USER_PROMPT + "\n\nUser message: " + userMessage);
        Prompt aiPrompt = new Prompt(message);

        String rawResponse = chatModel
                .call(aiPrompt)
                .getResult()
                .getOutput()
                .getText();

        WelcomeMessageModel model;
        try {
              model = objectMapper.readValue(rawResponse, WelcomeMessageModel.class);
        } catch (Exception ex) {
            log.error("extractUserNameOrAsk-Exception :: {}", ex.getMessage());
            throw ex;
        }

        model.setName(sanitizeName(model.getName()));
        log.info("extractUserNameOrAsk for :: {} :: RESPONSE :: {}", userMessage, model);

        return model;
    }


    /**
     * Ensures abusive or invalid names are replaced with "dear".
     */
    private String sanitizeName(String name) {
        if(name == null || name.isBlank()) return null;

        String lower = name.toLowerCase();
        for (String bad : bannedWords) {
            if (lower.contains(bad)) {
                return null;
            }
        }

        // Also: reject names that are too long, too short, or contain symbols
        if (name.length() > 30 || name.length() < 2 || !name.matches("[A-Za-z ]+")) {
            return "Dear";
        }

        return name;
    }
}