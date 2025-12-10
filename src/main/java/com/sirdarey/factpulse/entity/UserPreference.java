package com.sirdarey.factpulse.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "FK_user_id"),
            nullable = false
    )
    private User user;


    //For a new preference
    public UserPreference(String topic, @Nullable Integer freqInSeconds, @Nullable String tone, User user, @Nullable ZonedDateTime nextRun) {
        this.topic = topic;
        this.active = true;
        this.createdAt = ZonedDateTime.now();
        this.freqInSeconds = freqInSeconds;
        this.tone = tone;
        this.user = user;
        this.nextRun = nextRun;

        if(freqInSeconds != null) {
            this.schedulerId = UUID.randomUUID().toString();
        }
    }
}