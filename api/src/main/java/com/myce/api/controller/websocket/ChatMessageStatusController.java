package com.myce.api.controller.websocket;

import com.myce.api.controller.supporter.SessionUserInfoSupporter;
import com.myce.api.dto.WebSocketUserInfo;
import com.myce.api.dto.request.ChatReadStatusRequest;
import com.myce.api.service.ChatReadStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageStatusController {

    private final ChatReadStatusService chatReadStatusService;

    /**
     * 사용자 읽음 상태 알림 처리
     * /app/read-status-notify -> 관리자에게 읽음 상태 알림 브로드캐스트
     */
    @MessageMapping("/read-status-notify")
    public ResponseEntity<Object> notifyReadStatus(
            @Payload ChatReadStatusRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        SessionUserInfoSupporter.validateUserInfo(headerAccessor);
        chatReadStatusService.updateChatReadStatus(request.getRoomCode(), request.getReaderType());
        return ResponseEntity.noContent().build();
    }

}
