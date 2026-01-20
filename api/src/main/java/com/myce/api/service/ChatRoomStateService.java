package com.myce.api.service;

import com.myce.api.dto.WebSocketUserInfo;

public interface ChatRoomStateService {
    void adminHandoff(WebSocketUserInfo userInfo, String roomCode, String sessionId);
    void cancelHandoff(WebSocketUserInfo userInfo, String roomCode, String sessionId);
    void adminPreIntervention(WebSocketUserInfo userInfo, String roomCode, String sessionId);
    void acceptHandoff(WebSocketUserInfo userInfo, String roomCode, String sessionId);
    void aiHandoff(WebSocketUserInfo userInfo, String roomCode, String sessionId);
}
