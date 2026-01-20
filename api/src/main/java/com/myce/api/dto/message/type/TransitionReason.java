package com.myce.api.dto.message.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TransitionReason {
    BUTTON_STATE_UPDATE("button_state_update"),

    ADMIN_MESSAGE("admin_message"),
    USER_MESSAGE("user_message"),
    MESSAGE_FLOW("message_flow"),
    HANDOFF_TO_OPERATOR("handoff_to_operator"),
    HANDOFF_REQUEST("handoff_requested"),
    HANDOFF_CANCELLED("handoff_cancelled"),
    HANDOFF_ACCEPTED("handoff_accepted"),
    HANDOFF_AI_REQUEST("ai_return_requested"),
    HANDOFF_TO_AI("handoff_to_ai"),
    ADMIN_TIMEOUT("admin_timeout"),
    UNKNOWN("unknown");

    private final String reason;
}
