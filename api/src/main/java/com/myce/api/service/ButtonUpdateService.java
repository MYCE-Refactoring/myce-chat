package com.myce.api.service;

import com.myce.domain.document.type.ChatRoomState;

public interface ButtonUpdateService {
    void sendButtonStateUpdate(String roomId, ChatRoomState newState);
}
