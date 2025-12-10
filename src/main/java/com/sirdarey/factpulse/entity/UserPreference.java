package com.sirdarey.factpulse.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
@Getter @Setter
@NoArgsConstructor
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Nullable
    private Integer freqInSeconds; //How often to get facts on this topic

    @Nullable
    private String tone;

    private boolean active;

    @Column(nullable = false)
    private ZonedDateTime createdAt;

    @Nullable
    private ZonedDateTime updatedAt;

    @Nullable
    private ZonedDateTime nextRun;

    @Nullable
    private String schedulerId;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "fact_history_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "FK_fact_history_id"),
            nullable = false
    )
    private List<FactHistory> history;


    //For a new preference
    public UserPreference(String topic, @Nullable Integer freqInSeconds, @Nullable String tone) {
        this.topic = topic;
        this.active = true;
        this.createdAt = ZonedDateTime.now();
        this.freqInSeconds = freqInSeconds;
        this.tone = tone;

        if(freqInSeconds != null) {
            this.schedulerId = UUID.randomUUID().toString();
        }
    }
}