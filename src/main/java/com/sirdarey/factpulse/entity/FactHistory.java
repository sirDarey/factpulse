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


    public FactHistory(String content) {
        this.content = content;
        this.deliveredAt = ZonedDateTime.now();
    }
}