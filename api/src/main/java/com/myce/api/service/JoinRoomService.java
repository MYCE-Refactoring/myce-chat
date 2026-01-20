package com.myce.api.service;

import com.myce.api.dto.WebSocketUserInfo;

public interface JoinRoomService {
    /**
     * 채팅방 입장 권한 검증 및 처리
     */
    void joinRoom(WebSocketUserInfo userInfo, String roomId, String sessionId);
}
