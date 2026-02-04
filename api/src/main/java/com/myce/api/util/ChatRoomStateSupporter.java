package com.myce.api.util;

import com.myce.api.dto.message.ChatRoomStateInfo;
import com.myce.api.dto.message.type.TransitionReason;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.document.type.ChatRoomState;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class ChatRoomStateSupporter {

    /**
     * 채팅방 상태 정보 생성
     */
    public static ChatRoomStateInfo createRoomStateInfo(ChatRoom chatRoom, TransitionReason transitionReason) {
        LocalDateTime now = LocalDateTime.now();
        if (chatRoom == null) {
            return new ChatRoomStateInfo(ChatRoomState.AI_ACTIVE, now, transitionReason);
        }

        ChatRoomState currentState = chatRoom.getCurrentState();
        ChatRoomStateInfo stateInfo = new ChatRoomStateInfo(currentState, now, transitionReason);

        // Add admin info for admin active states
        if (currentState.equals(ChatRoomState.ADMIN_ACTIVE) && chatRoom.hasAssignedAdmin()) {
            stateInfo.addAdminInfo(
                    chatRoom.getCurrentAdminCode(),
                    chatRoom.getAdminDisplayName() != null ?
                            chatRoom.getAdminDisplayName() : Role.EXPO_ADMIN.getDisplayName(),
                    chatRoom.getLastAdminActivity() != null ? chatRoom.getLastAdminActivity() : now
            );

            return stateInfo;
        }


        if (currentState.equals(ChatRoomState.WAITING_FOR_ADMIN)
                && chatRoom.getHandoffRequestedAt() != null) {
            stateInfo.addHandOffInfo(
                    chatRoom.getHandoffRequestedAt(),
                    false
            );
        }

        return stateInfo;
    }

}
