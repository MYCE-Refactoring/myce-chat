package com.myce.api.dto.message;

import com.myce.api.dto.message.type.BroadcastType;
import lombok.Getter;

@Getter
public class WebSocketChatMessage extends WebSocketBaseMessage {
    private final ChatRoomStateInfo chatRoomStateInfo;

    public WebSocketChatMessage(BroadcastType type, ChatPayload payload, ChatRoomStateInfo roomStateInfo) {
        super(type, payload);
        this.chatRoomStateInfo = roomStateInfo;
    }

    public WebSocketChatMessage(BroadcastType type, HandoffNotificationInfo payload, ChatRoomStateInfo roomStateInfo) {
        super(type, payload);
        this.chatRoomStateInfo = roomStateInfo;
    }

    public WebSocketChatMessage(BroadcastType type, ButtonStatePayload payload, ChatRoomStateInfo roomStateInfo) {
        super(type, payload);
        this.chatRoomStateInfo = roomStateInfo;
    }
}
