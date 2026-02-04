package com.myce.api.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomWebSocketException extends RuntimeException{
    private CustomWebSocketError error;
}
