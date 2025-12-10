package com.sirdarey.factpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "facts_history")
@Getter @Setter
@NoArgsConstructor
public class FactHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private ZonedDateTime deliveredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_preference_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "FK_user_preference_id"),
            nullable = false
    )
    private UserPreference userPreference;


    public FactHistory(String content, UserPreference userPreference) {
        this.content = content;
        this.deliveredAt = ZonedDateTime.now();
        this.userPreference = userPreference;
    }
}