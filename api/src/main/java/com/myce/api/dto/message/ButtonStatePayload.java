package com.myce.api.dto.message;

import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.type.ChatRoomState;
import com.myce.domain.document.type.MessageSenderType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ButtonStatePayload {
    private final String roomCode;
    private final ChatRoomState state;
    private final String buttonText;
    private final String buttonAction;
    private final String messageId;
    private final Long seq;
    private final MessageSenderType senderType;
    private final String senderName;
    private final String content;
    private final LocalDateTime sentAt;
    private final Integer unreadCount;
    private final String messageType;

    public ButtonStatePayload(String roomCode, ChatRoomState state) {
        this.roomCode = roomCode;
        this.state = state;
        this.buttonText = state.getButtonText();
        this.buttonAction = state.getButtonAction();
        this.messageId = null;
        this.seq = null;
        this.senderType = null;
        this.senderName = null;
        this.content = null;
        this.sentAt = null;
        this.unreadCount = null;
        this.messageType = null;
    }

    public static ButtonStatePayload of(String roomCode, ChatRoomState state, ChatMessage message) {
        if (message == null) {
            return new ButtonStatePayload(roomCode, state);
        }

        return new ButtonStatePayload(
                roomCode,
                state,
                state.getButtonText(),
                state.getButtonAction(),
                message.getId(),
                message.getSeq(),
                message.getSenderType(),
                message.getSenderName(),
                message.getContent(),
                message.getSentAt(),
                message.getUnreadCount(),
                message.getMessageType()
        );
    }

    public static ButtonStatePayload of(String roomCode, ChatRoomState state, ChatMessageResponse message) {
        if (message == null) {
            return new ButtonStatePayload(roomCode, state);
        }

        return new ButtonStatePayload(
                roomCode,
                state,
                state.getButtonText(),
                state.getButtonAction(),
                message.getMessageId(),
                message.getSeq(),
                message.getSenderType(),
                message.getSenderName(),
                message.getContent(),
                message.getSentAt(),
                message.getUnreadCount(),
                null
        );
    }
}
