package com.myce.api.service.component;

import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.repository.ChatRoomCacheRepository;
import com.myce.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatAdminAssignmentComponent {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomCacheRepository chatRoomCacheRepository;

    public void assignAdminIfNeeded(ChatRoom chatRoom, String adminCode) {
        log.info("Start assign admin. roomCode={}, adminCode={}", chatRoom.getRoomCode(), adminCode);

        validateNeedAssignAdmin(chatRoom, adminCode);
        // Save to MongoDB and update Redis cache when changes occur
        log.info("🔧 needsUpdate check - room: {}, adminCode: {}", chatRoom.getRoomCode(), adminCode);
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        chatRoomCacheRepository.cacheChatRoom(chatRoom.getRoomCode(), savedRoom);
        log.info("🔧 ChatRoom saved and cached - room: {}, adminCode: {}",
                chatRoom.getRoomCode(), adminCode);
    }

    private void validateNeedAssignAdmin(ChatRoom chatRoom, String adminCode) {
        // 할당된 담당자가 없을 때
        if (!chatRoom.hasAssignedAdmin()) {
            log.info("🔧 No admin assigned, attempting to assign: {}", adminCode);
            // Atomic assignment with collision protection
            boolean assigned = chatRoom.assignAdmin(adminCode);
            log.info("🔧 assignAdmin result: {}", assigned);
            if (assigned) {
                String displayName = getAdminDisplayName(adminCode);
                log.info("🔧 Generated displayName: {}", displayName);
                chatRoom.setAdminDisplayName(displayName);
                log.info(" Admin assigned successfully: {} to room {} - NEW STATE: {}",
                        adminCode, chatRoom.getRoomCode(), chatRoom.getCurrentState());
            } else {
                log.warn(" Admin assignment failed (collision): {} for room {}", adminCode, chatRoom.getRoomCode());
                throw new CustomException(CustomErrorCode.CHAT_ROOM_ACCESS_DENIED);
            }
        } else if (!chatRoom.getCurrentAdminCode().equals(adminCode)) {
            log.warn(" Admin permission denied: {} attempted access to room {} (owned by {})",
                    adminCode, chatRoom.getRoomCode(), chatRoom.getCurrentAdminCode());
            throw new CustomException(CustomErrorCode.CHAT_ROOM_ACCESS_DENIED);
        } else {
            // Same admin updating activity
            chatRoom.updateAdminActivity();
            log.debug("🔧 Admin activity updated: {} for room {} - STATE: {}",
                    adminCode, chatRoom.getRoomCode(), chatRoom.getCurrentState());
        }
    }

    /**
     * 관리자 표시 이름 생성
     */
    private String getAdminDisplayName(String adminCode) {
        if ("SUPER_ADMIN".equals(adminCode)) {
            return "박람회 관리자";
        } else {
            return "박람회 관리자 (" + adminCode + ")";
        }
    }

}
