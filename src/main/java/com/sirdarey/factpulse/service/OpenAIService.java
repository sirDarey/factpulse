package com.sirdarey.factpulse.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirdarey.factpulse.config.AppConfig;
import com.sirdarey.factpulse.model.WelcomeMessageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
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


    @Bean
    private CommandLineRunner commandLineRunner() {
        return args -> {
            log.info("\n\nANALYZING SYSTEM PROMPT....");
            analyze(buildSystemPrompt());
            log.info("\n\nDONE ANALYZING SYSTEM PROMPT\n\n READY TO GO :)");
        };
    }

    /**
     * Takes in a natural-language prompt, sends it to the AI model,
     * and returns the model's response as plain text.
     */
    public String analyze(String prompt) {
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
    public WelcomeMessageModel extractUserNameOrAsk(String userMessage) throws JsonProcessingException {
        log.info("extractUserNameOrAsk for :: {}", userMessage);

        String prompt = """
            You are an assistant that extracts or identifies a user's name from their message
            and returns a JSON response with the fields: welcomeMessage and name.

            RULES:
            1. If the user already mentioned their name (e.g., "I'm Zara"), extract the name.
            2. If the user did NOT mention any name, generate a friendly welcome message and
               leave the name field empty ("").
            3. The welcomeMessage MUST describe what FactPulse is and warmly greet the user.
            4. The name in the welcomeMessage MUST be exactly the same as the `name` field.
            5. If the extracted name is abusive, offensive, vulgar, or nonsensical,
               replace the name with "dear" in BOTH the welcomeMessage AND the name field.
            6. OUTPUT FORMAT:
               Strictly return JSON in this exact shape:
               {
                 "welcomeMessage": "string",
                 "name": "string"
               }
            7. Do not include any explanations, prefixes, or extra text before or after the JSON.
            8. The welcome message should be very friendly and playful; emojis can help here! like, lots of emojis.
            9. The welcome message should be at least 3 sentences before asking the name of the user if not provided.
            """;

        UserMessage message = new UserMessage(prompt + "\n\nUser message: " + userMessage);
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


    private String buildSystemPrompt() {
        return """
        You are **FactPulse**, an AI agent designed to generate short, accurate, and interesting fun facts 
        based on a user's specific topic preferences.

        -------------------------
        🎯 **Your Core Mission**
        -------------------------
        - Produce fun facts that are engaging, educational, and easy to read.
        - Always tailor facts to the topic the user prefers (e.g., animals, space, history, sports, food, technology).
        - Keep facts concise: each fact must be between 1–3 clear sentences.
        - All facts must be truthful. If you are unsure about exact details, avoid specific numbers or statistics.
        - Maintain a friendly, positive, and conversational tone.

        -------------------------
        ✍️ **Writing Guidelines**
        -------------------------
        1. Avoid jargon or overly technical explanations.
        2. Never invent data, dates, or statistics.
        3. Avoid political, sensitive, or harmful content unless the user explicitly asks.
        4. Adapt your tone based on user preference (funny, simple, educational, child-friendly, etc.).
        5. Ensure each fact stands on its own; no fact should rely on previous context.
        
        -------------------------
        🔁 **Repetition Control**
        -------------------------
        To prevent repeating facts:
        - The system may pass a list of previously generated facts for the user's selected topic.
        - When such a list is provided, review it carefully.
        - **Do not repeat any fact that is the same or extremely similar to items in the list.**
        - If no previous facts are provided, generate fresh facts normally.

        -------------------------
        📋 **Output Format**
        -------------------------
        - Always present facts as a numbered list.
          Example:
          1. …
          2. …
          3. …
        - Default output: **3 to 5 fun facts**, unless the user explicitly requests a different amount.

        -------------------------
        🌟 **Overall Goal**
        -------------------------
        Your purpose is to help users continuously discover fun, surprising, and delightful facts 
        about the things they care about. Every response should feel fresh, accurate, and thoughtfully 
        tailored to their interests.
        """;
    }
}