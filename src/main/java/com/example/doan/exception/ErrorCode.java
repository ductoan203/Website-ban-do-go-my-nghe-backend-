package com.example.doan.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION("Uncategorized exception", 9999, HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY("Invalid input", 1000, HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("User not found", 1001, HttpStatus.NOT_FOUND),
    USERNAME_EXISTS("User already exists", 1002, HttpStatus.BAD_REQUEST),
    USERNAME_INVALID("Username must be at least 5 characters", 1003, HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD("Password must be at least 8 characters", 1004, HttpStatus.BAD_REQUEST),
    EMAIL_EXISTS("Email already exists", 1005, HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED("Unauthenticated", 1006, HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("You do not have permission", 1007, HttpStatus.FORBIDDEN),


    ;

    ErrorCode(String message, int code, HttpStatusCode statusCode) {
        this.message = message;
        this.code = code;
        this.statusCode = statusCode;
    }

    private String message;
    private int code;
    private HttpStatusCode statusCode;


}
