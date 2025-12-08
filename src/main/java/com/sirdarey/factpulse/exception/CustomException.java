package com.sirdarey.factpulse.exception;

import com.sirdarey.factpulse.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CustomException extends Exception {

    private ErrorCode errorCode;

    private String message;
}