package com.sirdarey.factpulse.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.List;

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
    private String deliveryChannels; //csv

    @Nullable
    private String tone;

    private boolean active;

    @Column(nullable = false)
    private ZonedDateTime createdAt;

    @Nullable
    private ZonedDateTime updatedAt;

    @Nullable
    private ZonedDateTime nextRun;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "fact_history_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "FK_fact_history_id"),
            nullable = false
    )
    private List<FactHistory> preferences;


    public UserPreference(String topic) {
        this.topic = topic;
        this.active = true;
        this.createdAt = ZonedDateTime.now();
    }
}