package com.myce.api.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * WebSocket 인증 요청 DTO
 */
@Getter
@NoArgsConstructor
public class AuthRequest {
    
    private String token;  // JWT 토큰
    
    @Builder
    public AuthRequest(String token) {
        this.token = token;
    }
}