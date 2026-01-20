package com.myce.api.dto.message;

import com.myce.domain.document.type.ChatRoomState;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ButtonStatePayload {
    private final String roomCode;
    private final ChatRoomState state;
    private final String buttonText;
    private final String buttonAction;

    public ButtonStatePayload(String roomCode, ChatRoomState state) {
        this.roomCode = roomCode;
        this.state = state;
        this.buttonText = state.getButtonText();
        this.buttonAction = state.getButtonAction();
    }
}
