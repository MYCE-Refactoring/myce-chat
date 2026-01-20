package com.myce.api.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomWebSocketError {
    private String sessionId;
    private String message;
}
