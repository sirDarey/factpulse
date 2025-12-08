package com.sirdarey.factpulse.service;

import com.sirdarey.factpulse.ErrorCode;
import com.sirdarey.factpulse.config.AppConfig;
import com.sirdarey.factpulse.entity.User;
import com.sirdarey.factpulse.exception.CustomException;
import com.sirdarey.factpulse.model.WelcomeMessageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIOrchestrationService {

    private final UserService userService;
    private final OpenAIService openAIService;


    public String analyze(String fromPhoneNo, String userInput) {
        log.info("analyze[{}] :: Analyzing user prompt...", fromPhoneNo);
        try {
            User user = userService.getUserByPhoneNo(fromPhoneNo);
            return openAIService.analyzePrompt(userInput);

        } catch (CustomException ex) {
            log.error("analyze CustomException :: {}", ex.getMessage());
            return processPossibleFirstUser(ex, userInput, fromPhoneNo);
        } catch (Exception e) {
            log.error("analyze Exception :: {}", e.getMessage());
            return AppConfig.APOLOGY_RESPONSE;
        }
    }


    private String processPossibleFirstUser(CustomException ex, String userInput, String phoneNo) {
        log.info("processPossibleFirstUser for :: {}", phoneNo);
        if (!ex.getErrorCode().equals(ErrorCode.USER_NOT_FOUND)) {
            return ex.getMessage();
        }

        try {
            WelcomeMessageModel model = openAIService.welcomeUser(userInput);
            log.info("processPossibleFirstUser[{}] saving new user...", phoneNo);

            userService.saveUser(phoneNo, model.getName());

            return model.getWelcomeMessage();

        } catch (Exception e) {
            log.error("processPossibleFirstUser Exception :: {}", e.getMessage());
            return AppConfig.APOLOGY_RESPONSE;
        }
    }
}