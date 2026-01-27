package com.myce.api.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 입장 요청 DTO
 */
@Getter
@NoArgsConstructor
public class JoinRoomRequest {
    
    private String roomCode;  // "admin-{expoId}-{userId}" 형식
    
    @Builder
    public JoinRoomRequest(String roomId) {
        this.roomCode = roomId;
    }
}