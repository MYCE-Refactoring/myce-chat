package com.myce.api.controller.websocket;

import com.myce.api.controller.supporter.SessionUserInfoSupporter;
import com.myce.api.dto.request.ChatReadStatusRequest;
import com.myce.api.dto.request.SendMessageRequest;
import com.myce.api.dto.WebSocketUserInfo;
import com.myce.api.service.SendMessageService;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/chats/rooms")
public class SendMessageController {

    private final SendMessageService sendMessageService;

    /**
     * 메시지 전송
     * /app/chat.send -> 메세지 저장 -> 채팅창 구독자들에게 실시간 브로드캐스트
     */
    @MessageMapping("/message-send")
    public ResponseEntity<Void> sendMessage(
            @Payload SendMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        WebSocketUserInfo userInfo = SessionUserInfoSupporter.getUserInfo(headerAccessor);
        Long memberId = userInfo.getMemberId();
        Role role = userInfo.getRole();
        LoginType loginType = userInfo.getLoginType();
        String sessionId = headerAccessor.getSessionId();
        sendMessageService.sendChatMessage(memberId, role, loginType, sessionId, request);

        return ResponseEntity.noContent().build();
    }

    /**
     * 관리자 채팅 메시지 전송
     * /app/admin/chat.send -> 관리자 권한 검증 -> 담당자 배정 -> 메시지 저장 및 브로드캐스트
     */
    @MessageMapping("/admin/message-send")
    public ResponseEntity<Object> sendAdminMessage(
            @Payload SendMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        WebSocketUserInfo userInfo = SessionUserInfoSupporter.getUserInfo(headerAccessor);
        Long memberId = userInfo.getMemberId();
        Role role = userInfo.getRole();
        LoginType loginType = userInfo.getLoginType();
        String sessionId = headerAccessor.getSessionId();
        sendMessageService.sendAdminChatMessage(memberId, role, loginType, sessionId, request);

        return ResponseEntity.noContent().build();
    }
}
