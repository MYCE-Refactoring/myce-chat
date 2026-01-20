package com.myce.api.config;

import com.myce.api.auth.service.WebSocketTicketService;
import com.myce.api.controller.supporter.SessionUserInfoSupporter;
import com.myce.api.dto.WebSocketUserInfo;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * WebSocket 핸드셰이크 시 티켓 검증 인터셉터
 * - 쿼리 파라미터에서 티켓 추출
 * - Redis에서 티켓 검증 및 사용자 정보 조회
 * - 검증 실패 시 401 에러 반환
 * - 검증 성공 시 세션 속성에 임시 저장 (HandshakeHandler에서 Principal로 변환)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketTicketHandshakeInterceptor implements HandshakeInterceptor {

    private static final String TICKET_PARAM = "ticket";
    public static final String ATTR_MEMBER_ID = "memberId";
    public static final String ATTR_ROLE = "role";
    public static final String ATTR_LOGIN_TYPE = "loginType";

    private final WebSocketTicketService ticketService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String uri = request.getURI().toString();
        log.debug("[WebSocket-Handshake] 핸드셰이크 시작. uri={}", uri);

        // 쿼리 파라미터에서 티켓 추출
        String ticket = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst(TICKET_PARAM);

        if (ticket == null || ticket.isBlank()) {
            log.warn("[WebSocket-Handshake] 티켓 없음. 연결 거부");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 티켓 검증
        Map<String, Object> userInfo = ticketService.validateAndConsumeTicket(ticket);

        if (userInfo == null) {
            log.warn("[WebSocket-Handshake] 유효하지 않은 티켓. 연결 거부");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 세션 속성에 사용자 정보 임시 저장 (HandshakeHandler에서 Principal로 변환)
        Object memberIdObj = userInfo.get("memberId");
        Long memberId = memberIdObj instanceof Integer
                ? ((Integer) memberIdObj).longValue()
                : (Long) memberIdObj;

        String roleStr = (String) userInfo.get("role");
        String loginTypeStr = (String) userInfo.get("loginType");
        Role role = Role.fromName(roleStr);
        if (role == null || loginTypeStr == null) {
            log.warn("[WebSocket-Handshake] 사용자 정보 누락. memberId={}, role={}, loginType={}",
                    memberId, roleStr, loginTypeStr);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        LoginType loginType = LoginType.fromString(loginTypeStr);

        attributes.put(ATTR_MEMBER_ID, memberId);
        attributes.put(ATTR_ROLE, roleStr);
        attributes.put(ATTR_LOGIN_TYPE, loginTypeStr);
        attributes.put(
                SessionUserInfoSupporter.SESSION_USER_INFO_ID,
                new WebSocketUserInfo(role, "", memberId, loginType)
        );

        log.info("[WebSocket-Handshake] 티켓 검증 성공. memberId={}, role={}",
                memberId, roleStr);

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        if (exception != null) {
            log.error("[WebSocket-Handshake] 핸드셰이크 실패", exception);
        }
    }
}
