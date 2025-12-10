package com.sirdarey.factpulse.service;

import com.sirdarey.factpulse.config.AppConfig;
import com.sirdarey.factpulse.entity.User;
import com.sirdarey.factpulse.entity.UserPreference;
import com.sirdarey.factpulse.enums.ErrorCode;
import com.sirdarey.factpulse.enums.UserIntent;
import com.sirdarey.factpulse.exception.CustomException;
import com.sirdarey.factpulse.model.AIResponseModel;
import com.sirdarey.factpulse.model.WelcomeMessageModel;
import com.sirdarey.factpulse.repo.UserPreferenceRepo;
import com.sirdarey.factpulse.repo.UserRepo;
import com.sirdarey.factpulse.util.PromptUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIOrchestrationService {

    private final UserRepo userRepo;
    private final UserService userService;
    private final OpenAIService openAIService;
    private final UserPreferenceRepo userPreferenceRepo;


    public String analyze(String fromPhoneNo, String userInput) {
        log.info("analyze[{}] :: Analyzing user prompt...", fromPhoneNo);
        try {
            User user = userService.getUserByPhoneNo(fromPhoneNo);

            if (user.getName() == null || user.getName().isBlank()) {
                log.info("analyze[{}] :: updating user's name...", fromPhoneNo);

                WelcomeMessageModel model = openAIService.welcomeUser(userInput);
                user.setName(model.getName());
                userRepo.save(user);
                log.info("analyze[{}] :: updated user's name :: {}", fromPhoneNo, model.getName());

                return model.getWelcomeMessage();
            }

            AIResponseModel aiResponse = openAIService.analyzePrompt(getPrompt(user, userInput));
            processAIResponse(aiResponse, user);

            return aiResponse.actualMessage();

        } catch (CustomException ex) {
            log.error("analyze CustomException :: {}", ex.getMessage());
            return processPossibleFirstUser(ex, userInput, fromPhoneNo);
        } catch (Exception e) {
            log.error("analyze Exception :: {}", e.getMessage());
            return AppConfig.APOLOGY_RESPONSE;
        }
    }


    private String getPrompt(User user, String userInput) {
        List<UserPreference> userPreferences = user.getPreferences();
        List<String> topics = userPreferences.stream().map(UserPreference::getTopic).toList();

        return String.format(PromptUtil.ANALYSIS_PROMPT, userInput, PromptUtil.SYSTEM_PROMPT, topics, user.getId());
    }

    private void processAIResponse(AIResponseModel aiResponse, User user) {
        if(aiResponse.userIntent()==null || aiResponse.data()==null
            || UserIntent.NONE.name().equals(aiResponse.userIntent())) return;

        if(UserIntent.UPDATE_NAME.name().equals(aiResponse.userIntent())) {
            updateName(user, aiResponse);
        } else if (UserIntent.NEW_PREFERENCE.name().equals(aiResponse.userIntent())) {
            addPreferenceToUser(user, aiResponse);
        } else if (UserIntent.UPDATE_PREFERENCE.name().equals(aiResponse.userIntent())) {
            updateUserPreference(user, aiResponse);
        }

        userRepo.save(user);
    }

    private void updateUserPreference(User user, AIResponseModel aiResponse) {
        assert aiResponse.data() != null;
        if(aiResponse.data().get("topic") == null) return;

        List<UserPreference> userPreferences = user.getPreferences();
        UserPreference preference = userPreferences
                .stream()
                .filter(p-> p.getTopic().equals(aiResponse.data().get("topic")))
                .findFirst()
                .orElse(null);

        if(preference != null) {
            if(aiResponse.data().get("freqInSeconds") != null) {
                preference.setFreqInSeconds(Integer.parseInt(aiResponse.data().get("freqInSeconds")));
            }
            if(aiResponse.data().get("tone") != null) {
                preference.setTone(aiResponse.data().get("tone"));
            }
            if(aiResponse.data().get("active") != null) {
                preference.setActive(Boolean.parseBoolean(aiResponse.data().get("active")));
            }
            preference.setUpdatedAt(ZonedDateTime.now());

            userPreferenceRepo.save(preference);
        }
    }

    private void addPreferenceToUser(User user, AIResponseModel aiResponse) {
        assert aiResponse.data() != null;
        if(aiResponse.data().get("topic") == null) return;

        List<UserPreference> userPreferences = user.getPreferences();
        Integer newFreqInSeconds = null;
        if(aiResponse.data().get("freqInSeconds") != null) {
            newFreqInSeconds = Integer.parseInt(aiResponse.data().get("freqInSeconds"));
        }

        UserPreference preference = userPreferenceRepo.save(new UserPreference(
                aiResponse.data().get("topic").toLowerCase(),
                newFreqInSeconds,
                aiResponse.data().get("tone")
        ));
        userPreferences.add(preference);

        if(newFreqInSeconds != null) {
            //TODO :: call scheduler for fresh schedule
        }
    }

    private void updateScheduling(@Nullable Integer newFreqInSeconds, UserPreference preference){
        if(newFreqInSeconds == null) return;
        if(preference.getFreqInSeconds() != null && !preference.getFreqInSeconds().equals(newFreqInSeconds)){
            //TODO :: Call scheduler service to make updates
        }
        preference.setFreqInSeconds(newFreqInSeconds);
    }

    private void updateName(User user, AIResponseModel aiResponse) {
        assert aiResponse.data() != null;
        if(aiResponse.data().get("name") != null){
            user.setName(aiResponse.data().get("name"));
            user.setUpdatedAt(ZonedDateTime.now());
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