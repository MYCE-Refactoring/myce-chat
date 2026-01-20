package com.myce.domain.document.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatRoomState {
    AI_ACTIVE("AI 상담 중", "Request Human", "request_handoff"),
    WAITING_FOR_ADMIN("상담원 대기 중", "Cancel Request", "cancel_handoff"),
    ADMIN_ACTIVE("상담원 상담 중", "Request AI", "request_ai");

    private final String description;
    private final String buttonText;
    private final String buttonAction;
}
