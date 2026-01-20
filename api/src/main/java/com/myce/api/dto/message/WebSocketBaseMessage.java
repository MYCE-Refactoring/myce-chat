package com.myce.api.dto.message;

import com.myce.api.dto.message.type.BroadcastType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WebSocketBaseMessage {
    private BroadcastType type;
    private Object payload;
}
