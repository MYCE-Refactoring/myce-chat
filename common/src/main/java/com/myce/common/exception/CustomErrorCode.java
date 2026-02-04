package com.myce.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CustomErrorCode implements CustomError{
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "U002", "유효하지 않은 토큰입니다."),
    INVALID_LOGIN_TYPE(HttpStatus.BAD_REQUEST, "U004", "잘못된 로그인 방식입니다."),

    MEMBER_NOT_EXIST(HttpStatus.NOT_FOUND, "M001", "회원정보가 존재하지 않습니다."),
    EXPO_NOT_EXIST(HttpStatus.NOT_FOUND, "E002", "박람회를 찾을 수 없습니다."),

    // 채팅 C
    CHAT_ROOM_NOT_EXIST(HttpStatus.NOT_FOUND, "C001", "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C002", "채팅방에 접근할 권한이 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "메시지를 찾을 수 없습니다."),
    CHAT_ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "C004", "이미 존재하는 채팅방입니다."),
    CHAT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "C005", "채팅 참여자 정보를 찾을 수 없습니다."),
    CHAT_SENDER_TYPE_INVALID(HttpStatus.BAD_REQUEST, "C006", "유효하지 않은 메시지 발송자 타입입니다."),
    CHAT_ROOM_NOT_CREATE(HttpStatus.BAD_REQUEST, "C007", "채팅방을 생성할 수 없습니다."),

    // AI
    ONLY_PLATFORM_ADMIN(HttpStatus.UNAUTHORIZED, "A001", "플랫폼 관리자 권한이 필요합니다."),
    ONLY_PLATFORM_CHAT_ROOM(HttpStatus.BAD_REQUEST, "A002", "플랫폼 채팅방만 요약 가능합니다."),
    ONLY_AI_ACTIVE_STATE(HttpStatus.BAD_REQUEST, "A003", "AI 활성 상태가 아닌 방에서는 사전 개입할 수 없습니다"),
    ONLY_ADMIN_WAITING_STATE(HttpStatus.BAD_REQUEST, "A004", "관리자 대기 상태가 아닙니다."),
    // WEBSOCKET
    INVALID_MEMBER(HttpStatus.UNAUTHORIZED, "W001", "인증되지 않은 사용자입니다."),

    // 일반 시스템 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S999", "내부 서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String errorCode;
    private final String message;
}