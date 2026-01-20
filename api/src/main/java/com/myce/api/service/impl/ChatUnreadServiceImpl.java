package com.myce.api.service.impl;

import com.myce.api.dto.message.type.MessageReaderType;
import com.myce.api.service.ChatUnreadService;
import com.myce.api.util.ChatReadStatusUtil;
import com.myce.api.util.RoomCodeSupporter;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.type.MessageSenderType;
import com.myce.domain.repository.ChatMessageRepository;
import com.myce.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 채팅 읽지 않은 메시지 계산 통합 서비스 구현체
 * 핵심 원칙: 카카오톡 방식
 * - 내가 보낸 메시지 → 상대방이 읽었는가?
 * - 상대방이 보낸 메시지 → 내가 읽었는가?
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatUnreadServiceImpl implements ChatUnreadService {
    
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    
    @Override
    public long getUnreadCountForViewer(String roomCode, String readStatus, Long viewerId, Role viewerRole) {
        log.trace("Start to calculate unread count. roomCode={}, viewerId={}, viewerRole={}",
                roomCode, viewerId, viewerRole);

        String readerType = getReaderType(viewerRole);
        String lastReadMessageId = ChatReadStatusUtil.extractLastReadMessageId(readStatus, readerType);

        log.trace("Current read status. userType: {}, lastReadId: {}", readerType, lastReadMessageId);
            
        String targetSenderType = determineTargetSenderType(viewerRole);
        long unreadCount = getUnreadCount(roomCode, lastReadMessageId, targetSenderType);

        log.debug("Success to get unread count for room. roomCode={}, viewerId={}, viewerRole={}",
                roomCode, viewerId, viewerRole);
        return unreadCount;
    }

    public int isReadMessage(ChatMessage message, String readStatus) {
        String messageId = message.getId();

        log.trace("[ChatRead] Check read message. messageId={}, readStatus{}", message, readStatus);
        boolean isRead; // 안읽음 1 읽음 0

        // 메시지 발송자에 따라 상대방의 읽음 상태 확인
        MessageSenderType senderType = message.getSenderType();
        if (MessageSenderType.ADMIN.equals(senderType) ||
                MessageSenderType.AI.equals(senderType)) {
            // 관리자나 AI가 보낸 메시지 -> 사용자가 읽었는지 확인
            String userLastReadId = ChatReadStatusUtil
                    .extractLastReadMessageId(readStatus, MessageReaderType.USER.name());
            isRead = isRead(messageId, userLastReadId);
        } else {
            // 사용자가 보낸 메시지 -> 상대방이 읽었는지 확인
            String roomCode = message.getRoomCode();
            String adminLastReadId = ChatReadStatusUtil
                    .extractLastReadMessageId(readStatus, MessageReaderType.ADMIN.name());

            isRead = isRead(messageId, adminLastReadId);

            if (roomCode != null && RoomCodeSupporter.isPlatformRoom(roomCode)) {
                // 플랫폼 채팅방: AI 또는 관리자 중 하나라도 읽었으면 읽음 처리
                String aiLastReadId = ChatReadStatusUtil
                        .extractLastReadMessageId(readStatus, MessageReaderType.AI.name());
                isRead = isRead(messageId, aiLastReadId);
            }
        }

        log.trace("[ChatRead] Check read message. messageId={}, isRead={}", message, isRead);
        return isRead ? 0 : 1;
    }

    private boolean isRead(String messageId, String lastReadId) {
        return lastReadId != null && messageId.compareTo(lastReadId) <= 0;
    }

    /**
     * 메시지 발송자 타입에 따라 읽음 상태를 확인할 상대방 타입 결정
     */
    private String determineReaderType(MessageSenderType messageSenderType) {
        if (MessageSenderType.SYSTEM.equals(messageSenderType)) {
            return null; // 시스템 메시지는 읽음 처리 안함
        }
        
        if (MessageSenderType.USER.equals(messageSenderType)) {
            // USER가 보낸 메시지 → ADMIN이 읽어야 함
            return "ADMIN";
        } else if (MessageSenderType.ADMIN.equals(messageSenderType) || MessageSenderType.AI.equals(messageSenderType)) {
            // ADMIN/AI가 보낸 메시지 → USER가 읽어야 함
            return "USER";
        }
        
        return null;
    }

    /**
     * 내 역할에 따라 내가 읽어야 할 메시지의 발송자 타입 결정
     */
    private String determineTargetSenderType(Role viewerRole) {
//        boolean isPlatformRoom = chatRoom.getExpoId() == null;

        if (Role.USER.equals(viewerRole)) {
            // 일반 사용자는 ADMIN/AI 메시지를 읽어야 함
            // 플랫폼 채팅방에서는 AI 또는 ADMIN 메시지 (둘 중 하나라도 있으면)
            // 일단 ADMIN으로 통일 (AI 메시지도 ADMIN 타입으로 저장되는 경우 많음)
            return MessageSenderType.ADMIN.name();
        } else {
            // 관리자는 USER 메시지를 읽어야 함
            return MessageSenderType.USER.name();
        }
    }

    private long getUnreadCount(String roomCode, String lastReadMessageId, String targetSenderType) {
        if (lastReadMessageId == null || lastReadMessageId.isEmpty()) {
            // 아직 아무것도 읽지 않았다면 전체 상대방 메시지 개수
            return chatMessageRepository.countByRoomCodeAndSenderType(roomCode, targetSenderType);
        } else {
            // 마지막 읽은 메시지 이후의 상대방 메시지 개수
            return chatMessageRepository.countByRoomCodeAndSenderTypeAndIdGreaterThan(
                    roomCode, targetSenderType, lastReadMessageId);
        }
    }

    private String getReaderType(Role role) {
        return Role.EXPO_SUPER_ADMIN.equals(role) || Role.PLATFORM_ADMIN.equals(role) ?
                MessageReaderType.ADMIN.name() : MessageReaderType.USER.name();
    }
    
}