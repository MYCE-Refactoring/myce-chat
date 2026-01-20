package com.myce.api.dto.message.type;

import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageReaderType {
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
     * AI 상담사
     * - 찍찍봇 AI가 자동 응답하는 메시지용
     */
    AI("찍찍봇");

    private final String description;

    public static MessageReaderType fromString(String type) {
        for (MessageReaderType t : MessageReaderType.values()) {
            if (t.name().equalsIgnoreCase(type)) return t;
        }
        throw new CustomException(CustomErrorCode.CHAT_SENDER_TYPE_INVALID);
    }
}
