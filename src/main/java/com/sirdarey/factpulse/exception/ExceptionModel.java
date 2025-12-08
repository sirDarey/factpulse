package com.sirdarey.factpulse.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Generic Exception Model")
public record ExceptionModel(int httpStatusCode, String status, String message, Map<String, String> errors) {

    public ExceptionModel(int httpStatusCode, String message) {
        this(httpStatusCode, "failed", message, null);
    }

    public ExceptionModel(int httpStatusCode, String message, Map<String, String> errors) {
        this(httpStatusCode, "failed", message, errors);
    }
}
