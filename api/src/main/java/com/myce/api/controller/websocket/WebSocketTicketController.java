package com.myce.api.controller.websocket;

import com.myce.api.auth.filter.InternalHeaderKey;
import com.myce.api.auth.service.WebSocketTicketService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WebSocket 연결을 위한 티켓 발급 API
 * - Gateway에서 JWT 인증 후 내부 헤더가 설정된 상태로 호출됨
 * - 30초간 유효한 1회용 티켓 발급
 */
@Slf4j
@RestController
@RequestMapping("/api/chats/ws")
@RequiredArgsConstructor
public class WebSocketTicketController {

    private final WebSocketTicketService ticketService;

    /**
     * WebSocket 연결용 티켓 발급
     * @return 티켓 문자열
     */
    @PostMapping("/ticket")
    public ResponseEntity<Map<String, String>> issueTicket(
            @RequestHeader(InternalHeaderKey.INTERNAL_MEMBER_ID) Long memberId,
            @RequestHeader(InternalHeaderKey.INTERNAL_ROLE) String role,
            @RequestHeader(value = InternalHeaderKey.INTERNAL_LOGIN_TYPE, required = false) String loginType
    ) {
        log.info("[WebSocketTicket] Request for issue ticket. memberId={}, role={}", memberId, role);

        String ticket = ticketService.issueTicket(memberId, role, loginType);

        return ResponseEntity.ok(Map.of("ticket", ticket));
    }
}
