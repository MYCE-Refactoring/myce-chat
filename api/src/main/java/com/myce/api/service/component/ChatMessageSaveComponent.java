package com.myce.api.service.component;

import com.myce.api.dto.SenderInfo;
import com.myce.api.service.ChatMessageService;
import com.myce.api.service.client.ExpoClient;
import com.myce.api.service.impl.ChatRoomAccessCheckService;
import com.myce.api.util.RoomCodeSupporter;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.document.type.ChatRoomState;
import com.myce.domain.document.type.MessageSenderType;
import com.myce.domain.repository.ChatMessageCacheRepository;
import com.myce.domain.repository.ChatRoomCacheRepository;
import com.myce.domain.repository.ChatRoomRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageSaveComponent {

    private final ExpoClient expoClient;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageService chatMessageService;
    private final ChatRoomCacheRepository chatRoomCacheRepository;
    private final ChatMessageCacheRepository chatMessageCacheRepository;
    private final ChatRoomAccessCheckService chatRoomAccessCheckService;

    public ChatMessage saveMessage(Long memberId, Role role, LoginType loginType, ChatRoom chatRoom, String content) {
        String roomCode = chatRoom.getRoomCode();

        log.trace("Start to save message. userId: {}, roomCode: {}, content: '{}'", memberId, roomCode, content);
        SenderInfo senderInfo = getSenderInfo(roomCode, memberId, role, loginType);

        ChatMessage chatMessage = chatMessageService.saveChatMessage(
                roomCode, senderInfo.getSenderType(), memberId, senderInfo.getSenderName(), content
        );

        // 1. Redis에 즉시 메시지 추가 (비동기)
        chatMessageCacheRepository.addMessageToCache(roomCode, chatMessage);

        // 2. 미읽음 카운트 증가 (수신자 찾기)
        Long receiverId = getReceiverId(chatRoom, memberId, senderInfo.getSenderRole());
        if (receiverId != null) {
            chatMessageCacheRepository.incrementUnreadCount(roomCode, receiverId, 1);
            chatMessageCacheRepository.incrementBadgeCount(receiverId);
            log.debug("Updated unread count for receiver: {} in room: {}", receiverId, roomCode);
        }

        // 3. 사용자 활성 채팅방에 추가
        chatRoomCacheRepository.addUserActiveRoom(memberId, roomCode);
        if (receiverId != null) chatRoomCacheRepository.addUserActiveRoom(receiverId, roomCode);

        // 4. MongoDB 저장 및 채팅방 업데이트 (동기 - 임시)
        updateChatRoomLastMessage(roomCode, chatMessage.getId(), content);
        log.trace("Success to save chat message. messageId: {}, roomCode: {}", chatMessage.getId(), roomCode);

        return chatMessage;
    }

    /**
     * 수신자 ID 찾기
     * 채팅방 타입에 따라 수신자 결정
     */
    private Long getReceiverId(ChatRoom chatRoom, Long senderId, Role senderRole) {
        String roomCode = chatRoom.getId();
        if (RoomCodeSupporter.isPlatformRoom(roomCode)) {
            Long roomUserId = RoomCodeSupporter.extractMemberIdFromPlatformRoomCode(roomCode);

            if (Role.PLATFORM_ADMIN.equals(senderRole)) {
                return roomUserId;
            } else {
                // 사용자가 보낸 경우 → 현재 활성 플랫폼 관리자가 수신자
                if (chatRoom.getCurrentState().equals(ChatRoomState.ADMIN_ACTIVE)) {
                    // ADMIN_ACTIVE 상태일 때 현재 담당 관리자의 ID 반환
                    String currentAdminCode = chatRoom.getCurrentAdminCode();
                    if ("PLATFORM_ADMIN".equals(currentAdminCode)) {
                        // 플랫폼 관리자 중 첫 번째 활성 관리자 찾기 (임시로 null 반환)
                        // TODO: 실제 활성 플랫폼 관리자 ID를 찾는 로직 구현 필요
                        return null;
                    }
                }
                return null; // AI 상태이거나 관리자를 찾을 수 없는 경우
            }
        } else if (RoomCodeSupporter.isAdminRoom(roomCode)) {
            // 박람회 채팅: admin-{expoId}-{userId}
            Long expoId = RoomCodeSupporter.extractExpoIdFromAdminRoomCode(roomCode);
            Long memberId = RoomCodeSupporter.extractMemberIdFromRoomCode(roomCode);

            if (Role.EXPO_ADMIN.equals(senderRole)) {
                // 관리자가 보낸 경우 → 참가자가 수신자
                return memberId;
            } else {
                // 사용자가 보낸 경우 → 현재 배정된 관리자가 수신자
                // ChatRoom에서 현재 배정된 관리자 정보 확인
                if (chatRoom.hasAssignedAdmin()) {
                    return null;
                }
                return null;
            }
        }

        return null;
    }

    /**
     * 채팅방 마지막 메시지 업데이트
     */
    private void updateChatRoomLastMessage(String roomId, String messageId, String content) {
        Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findByRoomCode(roomId);

        if (chatRoomOpt.isPresent()) {
            ChatRoom chatRoom = chatRoomOpt.get();
            chatRoom.updateLastMessageInfo(messageId, content);
            chatRoomRepository.save(chatRoom);
        }
    }

    public SenderInfo getSenderInfo(String roomCode, Long memberId, Role role, LoginType loginType) {
        String name = "임시";

        Role senderRole = role;
        String senderName = name;
        MessageSenderType senderType = role.equals(Role.USER) ? MessageSenderType.USER : MessageSenderType.ADMIN;
        // Handle platform rooms (format: platform-{userId})
        // TODO 각 상황에 따른 메소드로 분리 - SenderTypeService 따로 생성하면 될 듯
        if (RoomCodeSupporter.isPlatformRoom(roomCode) && Role.PLATFORM_ADMIN.equals(role)) {
            senderName = Role.PLATFORM_ADMIN.getDisplayName();
        } else {
            Long expoId = RoomCodeSupporter.extractExpoIdFromRoomCode(roomCode);
            Long participantId = RoomCodeSupporter.extractMemberIdFromRoomCode(roomCode);

            chatRoomAccessCheckService.isValidAccess(loginType, expoId, memberId, participantId, role);

            if (LoginType.ADMIN_CODE.equals(loginType)
                    && expoClient.checkAdminExpoAccessible(expoId, memberId)) {
                // TODO senderRole = "ADMIN"로 되어있었음 처리 필요;
                senderName = role.getDisplayName();
            } else if (Role.EXPO_ADMIN.equals(role)) {
                boolean isExpoOwner = expoClient.checkMemberExpoOwner(expoId, memberId);
                if (isExpoOwner) {
                    senderName = role.getDisplayName();
                } else {
                    senderRole = Role.USER;
                    senderType = MessageSenderType.USER;
                }
            }

            log.debug("Check message sender type. memberId={}, senderRole={}, senderName={}, loginType={}, expoId={}",
                    memberId, senderRole, senderName, loginType, expoId);
        }

        return new SenderInfo(senderRole, senderType, senderName);
    }
}
