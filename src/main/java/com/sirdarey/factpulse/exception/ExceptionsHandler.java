package com.sirdarey.factpulse.exception;

import com.sirdarey.factpulse.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionsHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionModel> handleAnyException(Exception e) {
        log.error("handleAnyException :: {}", e.getMessage());
        return ResponseEntity.status(500).body(new ExceptionModel(500, AppConfig.APOLOGY_RESPONSE));
    }
}