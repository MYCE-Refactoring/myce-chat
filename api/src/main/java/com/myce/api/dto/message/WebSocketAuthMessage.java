package com.myce.api.dto.message;

import com.myce.api.dto.message.type.BroadcastType;
import lombok.Getter;

@Getter
public class WebSocketAuthMessage extends WebSocketBaseMessage {
    private final Long memberId;
    private final String sessionId;

    public WebSocketAuthMessage(
            BroadcastType type, Long memberId, String sessionId, String payload) {
        super(type, payload);
        this.memberId = memberId;
        this.sessionId = sessionId;
    }
}
