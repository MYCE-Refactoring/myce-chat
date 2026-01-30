package com.myce.api.controller.websocket;

import com.myce.api.controller.supporter.SessionUserInfoSupporter;
import com.myce.api.dto.WebSocketUserInfo;
import com.myce.api.dto.request.ChatRoomActionRequest;
import com.myce.api.service.ChatRoomStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatRoomStateController {

    private final ChatRoomStateService chatRoomStateService;

    /**
     * 관리자 연결 요청 (버튼 액션)
     * /app/request-handoff -> AI가 관리자 연결 대기 상태로 전환
     */
    @MessageMapping("/request-handoff")
    public ResponseEntity<Object> requestHandoff(
            @Payload ChatRoomActionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        WebSocketUserInfo userInfo = SessionUserInfoSupporter.getUserInfo(headerAccessor);
        String sessionId = headerAccessor.getSessionId();
        chatRoomStateService.adminHandoff(userInfo, request.getRoomCode(), sessionId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 관리자 연결 요청 취소 (버튼 액션)
     * /app/cancel-handoff -> AI가 일반 상태로 복귀
     */
    @MessageMapping("/cancel-handoff")
    public ResponseEntity<Object> cancelHandoff(
            @Payload ChatRoomActionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        WebSocketUserInfo userInfo = SessionUserInfoSupporter.getUserInfo(headerAccessor);
        String sessionId = headerAccessor.getSessionId();
        chatRoomStateService.cancelHandoff(userInfo, request.getRoomCode(), sessionId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 관리자 사전 개입 (AI_ACTIVE 상태에서 직접 관리자가 개입)
     * /app/proactive-intervention -> AI_ACTIVE에서 바로 HUMAN_ACTIVE로 전환
     */
    @MessageMapping("/proactive-intervention")
    public ResponseEntity<Object> proactiveIntervention(
            @Payload ChatRoomActionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        WebSocketUserInfo userInfo = SessionUserInfoSupporter.getUserInfo(headerAccessor);
        String sessionId = headerAccessor.getSessionId();
        chatRoomStateService.adminPreIntervention(userInfo, request.getRoomCode(), sessionId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 관리자 인계 수락 (WAITING_FOR_ADMIN → ADMIN_ACTIVE)
     * 사용자가 요청한 관리자 연결을 관리자가 수락
     */
    @MessageMapping("/accept-handoff")
    public ResponseEntity<Object> acceptHandoff(
            @Payload ChatRoomActionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        WebSocketUserInfo userInfo = SessionUserInfoSupporter.getUserInfo(headerAccessor);
        String sessionId = headerAccessor.getSessionId();
        chatRoomStateService.acceptHandoff(userInfo, request.getRoomCode(), sessionId);

        return ResponseEntity.noContent().build();
    }

    /**
     * AI 복귀 요청 (버튼 액션)
     * /app/request-ai -> 관리자에서 AI로 전환
     */
    @MessageMapping("/request-ai")
    public ResponseEntity<Object> requestAI(
            @Payload ChatRoomActionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        WebSocketUserInfo userInfo = SessionUserInfoSupporter.getUserInfo(headerAccessor);
        String sessionId = headerAccessor.getSessionId();
        chatRoomStateService.aiHandoff(userInfo, request.getRoomCode(), sessionId);

        return ResponseEntity.noContent().build();
    }


}
