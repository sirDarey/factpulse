package com.sirdarey.factpulse.repo;

import com.sirdarey.factpulse.entity.FactHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FactHistoryRepo extends JpaRepository<FactHistory, Long> {

    @Query("SELECT f.content FROM FactHistory f WHERE f.userPreference.id=?1")
    List<String> getContentsByPreferenceId(Long id);
}