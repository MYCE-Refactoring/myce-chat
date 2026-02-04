package com.myce.api.dto.message.type;

import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 메시지 타입 Enum
 */
@Getter
@RequiredArgsConstructor
public enum BroadcastType {
    AUTH("인증"),
    AUTH_ACK("인증응답"),
    JOIN_ROOM("방입장"),
    MESSAGE_SEND("메시지전송"),
    MESSAGE("메시지"),
    AI_MESSAGE("AI 메시지"),
    ADMIN_MESSAGE("관리자 메시지"),
    SYSTEM_MESSAGE("시스템 메시지"),
    PLATFORM_HANDOFF_REQUEST("플랫폼 상담 요청 전달"),

    ADMIN_HANDOFF_REQUEST("관리자 상담 요청"),
    AI_HANDOFF_REQUEST("AI 상담 복귀"),
    ERROR("에러"),
    ADMIN_RELEASED("담당자해제"),
    READ_STATUS_UPDATE("읽음 상태 변경"),
    UNREAD_COUNT_UPDATE("안읽음 개수 변경"),
    ROOM_PREVIEW_UPDATE("채팅방 미리보기 변경"),

    ADMIN_ASSIGNMENT_UPDATE("admin_assignment_update"),

    BUTTON_STATE_UPDATE("BUTTON_STATE_UPDATE");        ;
    
    private final String description;
    
    public static BroadcastType fromString(String type) {
        for (BroadcastType t : BroadcastType.values()) {
            if (t.name().equalsIgnoreCase(type)) return t;
        }
        throw new CustomException(CustomErrorCode.CHAT_SENDER_TYPE_INVALID);
    }
    
    public static BroadcastType fromDescription(String description) {
        for (BroadcastType t : BroadcastType.values()) {
            if (t.getDescription().equals(description)) return t;
        }
        throw new CustomException(CustomErrorCode.CHAT_SENDER_TYPE_INVALID);
    }
}
