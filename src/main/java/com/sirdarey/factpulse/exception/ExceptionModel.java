package com.sirdarey.factpulse.exception;

public record ExceptionModel(

        int httpStatusCode,

        String message
) {}