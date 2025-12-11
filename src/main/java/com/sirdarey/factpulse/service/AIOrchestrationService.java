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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIOrchestrationService {

    private final UserRepo userRepo;
    private final UserService userService;
    private final OpenAIService openAIService;
    private final SchedulerService schedulerService;
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
        List<String> topics = userPreferenceRepo.getTopicsByUserId(user.getId());
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
            updateUserPreference(aiResponse, user.getId());
        }
    }

    private void updateUserPreference(AIResponseModel aiResponse, UUID userID) {
        assert aiResponse.data() != null;
        if(aiResponse.data().get("topic") == null) return;

//        List<UserPreference> userPreferences = user.getPreferences();
        UserPreference preference = userPreferenceRepo.findByTopicAndUserID(aiResponse.data().get("topic"), userID);

        if(preference != null) {
            Integer prevFreqInSeconds = preference.getFreqInSeconds();

            if(aiResponse.data().get("freqInSeconds") != null) {
                Integer newFreqInSeconds = Integer.parseInt(aiResponse.data().get("freqInSeconds"));
                preference.setFreqInSeconds(newFreqInSeconds);
            }
            if(aiResponse.data().get("tone") != null) {
                preference.setTone(aiResponse.data().get("tone"));
            }
            if(aiResponse.data().get("active") != null) {
                boolean isActive = Boolean.parseBoolean(aiResponse.data().get("active"));
                preference.setActive(isActive);

                if (!isActive) {
                    preference.setNextRun(null);
                }
            }
            preference.setUpdatedAt(ZonedDateTime.now());
            schedulerService.updateSchedule(preference, prevFreqInSeconds);


            userPreferenceRepo.save(preference);
        }
    }

    private void addPreferenceToUser(User user, AIResponseModel aiResponse) {
        assert aiResponse.data() != null;
        if(aiResponse.data().get("topic") == null) return;

        Integer newFreqInSeconds = null;
        if(aiResponse.data().get("freqInSeconds") != null) {
            newFreqInSeconds = Integer.parseInt(aiResponse.data().get("freqInSeconds"));
        }

        UserPreference preference = userPreferenceRepo.save(new UserPreference(
                aiResponse.data().get("topic").toLowerCase(),
                newFreqInSeconds,
                aiResponse.data().get("tone"),
                user,
                (newFreqInSeconds==null)? null : ZonedDateTime.now().plusSeconds(newFreqInSeconds)
        ));

        if(newFreqInSeconds != null) {
            schedulerService.scheduleTask(preference);
        }
    }


    private void updateName(User user, AIResponseModel aiResponse) {
        assert aiResponse.data() != null;
        if(aiResponse.data().get("name") != null){
            user.setName(aiResponse.data().get("name"));
            user.setUpdatedAt(ZonedDateTime.now());

            userRepo.save(user);
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