package com.sirdarey.factpulse.model;

import jakarta.annotation.Nullable;

import java.util.Map;

public record AIResponseModel (

        @Nullable
        String userIntent,

        @Nullable
        String actualMessage,

        @Nullable
        Map<String, String> data
){}