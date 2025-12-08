package com.sirdarey.factpulse.model;

import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeMessageModel{

    private String welcomeMessage;

    @Setter
    private String name;
}