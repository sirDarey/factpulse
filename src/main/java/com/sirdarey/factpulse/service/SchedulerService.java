package com.sirdarey.factpulse.service;

import com.sirdarey.factpulse.entity.FactHistory;
import com.sirdarey.factpulse.entity.User;
import com.sirdarey.factpulse.entity.UserPreference;
import com.sirdarey.factpulse.repo.FactHistoryRepo;
import com.sirdarey.factpulse.repo.UserPreferenceRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final TaskScheduler taskScheduler;
    private final OpenAIService openAIService;
    private final FactHistoryRepo factHistoryRepo;
    private final WhatsappService whatsappService;
    private final UserPreferenceRepo userPreferenceRepo;

    // Map to keep track of running tasks: <PreferenceSchedulerId, FutureObject>
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();



    /**
     * Loads all active preferences from DB on startup/restart
     */
    @EventListener(ContextRefreshedEvent.class)
    private void initSchedules() {
        log.info("SchedulerService :: Initializing active schedules from Database...");
        List<UserPreference> activePreferences = userPreferenceRepo.findByActiveTrue();

        for (UserPreference pref : activePreferences) {
            scheduleTask(pref);
        }
        log.info("SchedulerService :: Restored {} schedules.", activePreferences.size());
    }


    /**
     * Schedules a new task based on the preference
     */
    public void scheduleTask(UserPreference preference) {
        try {
            if (preference.getFreqInSeconds() == null || preference.getFreqInSeconds() <= 0) {
                log.warn("Cannot schedule preference ID {} :: Invalid Frequency :: {}", preference.getId(), preference.getFreqInSeconds());
                return;
            }

            // define the task logic
            Runnable task = () -> executeFactGeneration(preference);

            // Calculate period
            PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofSeconds(preference.getFreqInSeconds()));

            // Calculate Initial Delay
            Duration initialDelay = Duration.ofSeconds(1); // Default fallback
            if(preference.getNextRun() != null) {
                ZonedDateTime now = ZonedDateTime.now();
                if (preference.getNextRun().isAfter(now)) {
                    // Calculate time remaining until the scheduled run
                    initialDelay = Duration.between(now, preference.getNextRun());
                }
            }
            trigger.setInitialDelay(initialDelay);

            // Schedule and store the future object so we can cancel it later
            ScheduledFuture<?> future = taskScheduler.schedule(task, trigger);
            scheduledTasks.put(preference.getSchedulerId(), future);

            log.info("Scheduled task for Preference ID: {} | Topic: {} | Freq: {}s",
                    preference.getId(), preference.getTopic(), preference.getFreqInSeconds());
        } catch (Exception e) {
            log.error("scheduleTask[{}] Exception :: {}", preference.getSchedulerId(), e.getMessage());
        }
    }


    /**
     * Updates an existing schedule (Cancels old -> Starts new)
     */
    public void updateSchedule(UserPreference preference) {
        try {
            stopSchedule(preference.getSchedulerId());
            if(preference.isActive()) {
                scheduleTask(preference);
            }
        } catch (Exception e) {
            log.error("updateSchedule[{}] Exception :: {}", preference.getSchedulerId(), e.getMessage());
        }
    }


    /**
     * Stops a schedule if it exists
     */
    void stopSchedule(String schedulerId) {
        ScheduledFuture<?> future = scheduledTasks.get(schedulerId);
        if (future != null) {
            future.cancel(false);
            scheduledTasks.remove(schedulerId);
            log.info("Stopped schedule for Preference ID: {}", schedulerId);
        }
    }


    /**
     * The logic that runs when the timer goes off
     */
    private void executeFactGeneration(UserPreference preference) {
        try {
            log.info("executeFactGeneration[{}] :: Processing topic: {}", preference.getSchedulerId(), preference.getTopic());

            List<String> history = factHistoryRepo.getContentsByPreferenceId(preference.getId());
            String historyContext = String.join(" | ", history);

            String prompt = String.format(
            """
            You are a fact generator.
            Topic: %s
            Tone: %s
            
            CONSTRAINT: Here is a list of facts you have ALREADY generated for this user:
            [%s]
            
            INSTRUCTIONS:
            1. Generate a NEW, UNIQUE fact that is NOT in or common to any of the history list above.
            2. If you have exhausted all interesting facts about this topic and cannot generate a new one, respond with exactly: 'NO_MORE_FACTS'
            3. Do not add introductions like "Here is a fact". Just the fact.
            4. You can start by saying: "Here's a new fact about #topic: ...."
            """,
                    preference.getTopic(),
                    (preference.getTone()==null)? "friendly" : preference.getTone(),
                    historyContext
            );

            User user = preference.getUser();
            String aiResponseText = openAIService.analyzePromptToSimpleText(prompt);

            preference.setUpdatedAt(ZonedDateTime.now());

            if ("NO_MORE_FACTS".equalsIgnoreCase(aiResponseText)) {
                log.info("executeFactGeneration :: Exhausted facts for topic '{}'. Deactivating.", preference.getTopic());

                // Deactivate in DB
                preference.setActive(false);
                preference.setNextRun(null);
                userPreferenceRepo.save(preference);

                // Stop the Schedule
                stopSchedule(preference.getSchedulerId());

                // Notify User
                String message = "I've run out of fresh facts for '" + preference.getTopic() + "'! 🛑 I have deactivated this schedule to avoid repeating myself.";
                whatsappService.sendWhatsAppReply(user.getPhoneNo(), message);
                return;
            }

            // Success Path: Send Fact and Save History
            whatsappService.sendWhatsAppReply(user.getPhoneNo(), aiResponseText);

            // Save to history so it's included in the exclusion list next time
            assert preference.getFreqInSeconds() != null;
            preference.setNextRun(ZonedDateTime.now().plusSeconds(preference.getFreqInSeconds()));
            userPreferenceRepo.save(preference);
            factHistoryRepo.save(new FactHistory(aiResponseText, preference));

            log.info("executeFactGeneration :: Sent new fact to {}", user.getPhoneNo());

        } catch (Exception e) {
            log.error("Error executing scheduled task for Preference ID {}", preference.getId(), e);
        }
    }
}