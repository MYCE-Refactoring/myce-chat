package com.myce.api.config;

import com.myce.api.auth.dto.CustomUserDetails;
import com.myce.common.type.LoginType;
import java.security.Principal;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * WebSocket 핸드셰이크 시 Principal 설정
 * - HandshakeInterceptor에서 검증된 사용자 정보를 세션 속성에서 읽어옴
 * - CustomUserDetails로 변환하여 Principal로 반환
 */
@Slf4j
@Component
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {

        log.debug("[WebSocket-Handshake-Handler] Principal 설정 시작. uri={}", request.getURI());

        // HandshakeInterceptor에서 저장한 사용자 정보 조회
        Long memberId = (Long) attributes.get(WebSocketTicketHandshakeInterceptor.ATTR_MEMBER_ID);
        String role = (String) attributes.get(WebSocketTicketHandshakeInterceptor.ATTR_ROLE);
        String loginTypeStr = (String) attributes.get(WebSocketTicketHandshakeInterceptor.ATTR_LOGIN_TYPE);

        if (memberId == null || role == null) {
            log.warn("[WebSocket-Handshake-Handler] 사용자 정보 없음. 비인증 연결");
            return super.determineUser(request, wsHandler, attributes);
        }

        // CustomUserDetails 생성
        LoginType loginType = LoginType.fromString(loginTypeStr);
        CustomUserDetails userDetails = CustomUserDetails.builder()
                .memberId(memberId)
                .role(role)
                .loginType(loginType)
                .build();

        log.info("[WebSocket-Handshake-Handler] Principal 설정 완료. memberId={}, role={}",
                memberId, role);

        // UsernamePasswordAuthenticationToken을 Principal로 반환
        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }
}
