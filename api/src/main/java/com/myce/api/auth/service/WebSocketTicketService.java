package com.myce.api.auth.service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 연결을 위한 1회용 티켓 관리 서비스
 * - 티켓 발급: Gateway JWT 인증 후 호출
 * - 티켓 검증: WebSocket 핸드셰이크 시 호출
 * - 티켓은 30초간 유효하며, 1회 사용 후 삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketTicketService {

    private static final String TICKET_KEY_PREFIX = "ws:ticket:";
    private static final Duration TICKET_TTL = Duration.ofSeconds(30);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * WebSocket 연결용 티켓 발급
     * @param memberId 사용자 ID
     * @param role 사용자 역할
     * @param loginType 로그인 타입
     * @return 발급된 티켓 문자열
     */
    public String issueTicket(Long memberId, String role, String loginType) {
        String ticket = UUID.randomUUID().toString();
        String key = TICKET_KEY_PREFIX + ticket;

        Map<String, Object> userInfo = Map.of(
                "memberId", memberId,
                "role", role,
                "loginType", loginType
        );

        redisTemplate.opsForValue().set(key, userInfo, TICKET_TTL);
        log.info("[WebSocketTicket] 티켓 발급. memberId={}, ticket={}", memberId, ticket);

        return ticket;
    }

    /**
     * 티켓 검증 및 사용자 정보 조회 (1회용 - 조회 후 삭제)
     * @param ticket 티켓 문자열
     * @return 사용자 정보 Map (memberId, role, loginType) 또는 null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> validateAndConsumeTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            log.warn("[WebSocketTicket] 티켓이 비어있음");
            return null;
        }

        String key = TICKET_KEY_PREFIX + ticket;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            log.warn("[WebSocketTicket] 유효하지 않거나 만료된 티켓. ticket={}", ticket);
            return null;
        }

        // 티켓 삭제 (1회용)
        redisTemplate.delete(key);
        log.info("[WebSocketTicket] 티켓 검증 성공 및 소비. ticket={}", ticket);

        return (Map<String, Object>) value;
    }
}
