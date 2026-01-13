package com.myce.domain.document.type;

import lombok.Getter;

@Getter
public enum ChatRoomState {
    AI_ACTIVE("AI 상담 중", "Request Human"),
    WAITING_FOR_ADMIN("상담원 대기 중", "Cancel Request"),
    ADMIN_ACTIVE("상담원 상담 중", "Request AI");

    private final String description;
    private final String buttonText;

    ChatRoomState(String description, String buttonText) {
        this.description = description;
        this.buttonText = buttonText;
    }

}
