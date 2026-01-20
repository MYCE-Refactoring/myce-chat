package com.myce.common.exception;


import org.springframework.http.HttpStatus;

public interface CustomError {
    HttpStatus getStatus();
    String getErrorCode();
    String getMessage();
}
