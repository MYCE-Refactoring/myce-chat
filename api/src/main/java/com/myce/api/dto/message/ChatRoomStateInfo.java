package com.myce.api.dto.message;

import com.myce.api.dto.message.type.TransitionReason;
import com.myce.domain.document.type.ChatRoomState;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ChatRoomStateInfo {
    private final ChatRoomState state;
    private final LocalDateTime time;
    private final TransitionReason transitionReason;
    private boolean isAdmin;
    private AdminInfo adminInfo;
    private boolean isHandOff;
    private HandOffInfo handOffInfo;

    public ChatRoomStateInfo(ChatRoomState state, LocalDateTime time, TransitionReason transitionReason) {
        this.state = state;
        this.time = time;
        this.transitionReason = transitionReason;
    }

    public void addAdminInfo(String adminCode, String displayName, LocalDateTime lastActivity) {
        this.isAdmin = true;
        this.adminInfo = new AdminInfo(adminCode, displayName, lastActivity);
    }

    public void addHandOffInfo(LocalDateTime requestedAt, boolean isAiSummaryGenerated) {
        this.isHandOff = true;
        this.handOffInfo = new HandOffInfo(requestedAt, isAiSummaryGenerated);
    }

    private record AdminInfo(String adminCode, String displayName, LocalDateTime lastActivity) { }
    private record HandOffInfo(LocalDateTime requestedAt, boolean isAiSummaryGenerated) { }
}
