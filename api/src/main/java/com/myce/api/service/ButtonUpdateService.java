package com.myce.api.service;

import com.myce.domain.document.type.ChatRoomState;
import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.domain.document.ChatMessage;

public interface ButtonUpdateService {
    void sendButtonStateUpdate(String roomCode, ChatRoomState newState);
    void sendButtonStateUpdate(String roomCode, ChatRoomState newState, ChatMessage message);
    void sendButtonStateUpdate(String roomCode, ChatRoomState newState, ChatMessageResponse message);
}
