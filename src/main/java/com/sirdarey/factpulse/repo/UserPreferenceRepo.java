package com.sirdarey.factpulse.repo;

import com.sirdarey.factpulse.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserPreferenceRepo extends JpaRepository<UserPreference, Long> {

    List<UserPreference> findByActiveTrue();

    @Query("SELECT p.topic FROM UserPreference p WHERE p.user.id=?1")
    List<String> getTopicsByUserId(UUID userID);

    @Query("SELECT u FROM UserPreference u WHERE u.topic=?1 AND u.user.id=?2")
    UserPreference findByTopicAndUserID(String topic, UUID userID);
}