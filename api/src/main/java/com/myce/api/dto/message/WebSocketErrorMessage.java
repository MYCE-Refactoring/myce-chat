package com.myce.api.dto.message;

import com.myce.api.dto.message.type.BroadcastType;
import lombok.Getter;

@Getter
public class WebSocketErrorMessage extends WebSocketBaseMessage {
    public WebSocketErrorMessage(BroadcastType type, String payload) {
        super(type, payload);
    }
}
