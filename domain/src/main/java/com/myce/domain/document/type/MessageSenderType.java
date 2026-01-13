package com.myce.domain.document.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 채팅 메시지 발송자 타입 Enum
 */
@Getter
@RequiredArgsConstructor
public enum MessageSenderType {
    
    /**
     * 일반 사용자
     * - 박람회 방문객, 예약자 등
     */
    USER("사용자"),
    
    /**
     * 관리자
     * - 박람회 관리자 (Super Admin, AdminCode)
     */
    ADMIN("관리자"),
    
    /**
     * 시스템
     * - 시스템에서 자동 생성하는 메시지용
     */
    SYSTEM("시스템"),
    
    /**
     * AI 상담사
     * - 찍찍봇 AI가 자동 응답하는 메시지용
     */
    AI("찍찍봇");
    
    private final String description;

}