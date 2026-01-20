package com.myce.api.dto.message.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WebSocketMessagePayload {
    AUTH_SUCCESS("Authentication successful"),
    AUTH_FAIL("Authentication failed"),

    SEND_MESSAGE_FAIL("Send message fail."),
    JOIN_MESSAGE_FAIL("Join room failed.");

    private final String message;
}
