package com.myce.api.controller.websocket;

import com.myce.api.controller.supporter.SessionUserInfoSupporter;
import com.myce.api.dto.WebSocketUserInfo;
import com.myce.api.service.JoinRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/chats/rooms")
public class ConnectChatRoomController {

    private final JoinRoomService joinRoomService;

    /**
     * 채팅방 입장
     * /app/join -> 권한 검증 -> 채팅방 입장 -> 새션에 현재 방 저장
     */
    @MessageMapping("/join/{room-code}")
    public ResponseEntity<Object> joinRoom(
            @DestinationVariable("room-code") String roomCode,
            SimpMessageHeaderAccessor headerAccessor) {

        WebSocketUserInfo userInfo = SessionUserInfoSupporter.getUserInfo(headerAccessor);
        String sessionId = headerAccessor.getSessionId();
        joinRoomService.joinRoom(userInfo, roomCode, sessionId);

        return ResponseEntity.noContent().build();
    }

}
