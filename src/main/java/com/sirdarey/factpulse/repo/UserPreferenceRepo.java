package com.sirdarey.factpulse.repo;

import com.sirdarey.factpulse.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepo extends JpaRepository<UserPreference, Long> {
}