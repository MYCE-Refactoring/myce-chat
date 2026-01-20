package com.myce.api.service.impl;

import com.myce.api.dto.message.ChatUnReadCountPayload;
import com.myce.api.dto.message.WebSocketBaseMessage;
import com.myce.api.dto.message.type.BroadcastType;
import com.myce.api.dto.message.type.MessageReaderType;
import com.myce.api.service.ChatReadStatusService;
import com.myce.api.service.ChatWebSocketBroadcaster;
import com.myce.api.util.RoomCodeSupporter;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.repository.ChatMessageCacheRepository;
import com.myce.domain.repository.ChatRoomCacheRepository;
import com.myce.domain.repository.ChatRoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatReadStatusServiceImpl implements ChatReadStatusService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomAccessCheckService accessCheckService;
    private final ChatWebSocketBroadcaster webSocketBroadcaster;
    private final ChatRoomCacheRepository chatRoomCacheRepository;
    private final ChatMessageCacheRepository chatMessageCacheRepository;

    @Override
    public void updateChatReadStatus(String roomCode, MessageReaderType readerType) {
        ChatUnReadCountPayload readStatusDto = new ChatUnReadCountPayload(roomCode, readerType, 0);
        WebSocketBaseMessage message = new WebSocketBaseMessage(BroadcastType.READ_STATUS_UPDATE, readStatusDto);
        webSocketBroadcaster.sendMessage(roomCode, message);
    }

    public void markAsReadForMember(String roomCode, String lastReadMessageId, Long memberId, Role role) {
        log.debug("[ChatReadStatusService] Mark as read for member. roomCode={}, lastReadMessageId={}, "
                + "memberId={}", roomCode, lastReadMessageId, memberId);
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        boolean isPlatformRoom = RoomCodeSupporter.isPlatformRoom(roomCode);
        accessCheckService.validateAccess(isPlatformRoom, chatRoom.getMemberId(), chatRoom.getExpoId(), memberId, role);

        String readerType = Role.EXPO_SUPER_ADMIN.equals(role) || Role.PLATFORM_ADMIN.equals(role) ?
                MessageReaderType.ADMIN.name() : MessageReaderType.USER.name();
        chatRoom.updateReadStatus(readerType,  lastReadMessageId);
        chatRoomRepository.save(chatRoom);
        log.debug("[ChatReadStatusService] Mark as read for member. roomCode={}, memberId={}, lastReadMessageId={}",
                roomCode, memberId, lastReadMessageId);
    }

    @Override
    @Transactional
    public void markAsReadForAdmin(Long expoId, String roomCode, String lastReadMessageId, Long memberId) {
        log.debug("[ChatReadStatusService] Mark as read for admin. expoId={}, roomCode={}, lastReadMessageId={}, "
                + "memberId={}", expoId, roomCode, lastReadMessageId, memberId);

        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        if (!chatRoom.getExpoId().equals(expoId)) {
            throw new CustomException(CustomErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        resetAndRecalculateBadgeCount(roomCode, memberId);
        log.debug("[ChatReadStatusService] Redis unread count reset for admin: {} in room: {}", memberId, roomCode);

        // 마지막 읽은 메시지 ID Redis 저장
        if (lastReadMessageId != null && !lastReadMessageId.trim().isEmpty()) {
            chatMessageCacheRepository.setLastReadMessageId(roomCode, memberId, lastReadMessageId);
        }

        // 마지막 메시지 ID를 가져와서 읽음 처리 (가장 최근 메시지까지 읽음 처리)
        chatRoom.updateReadStatus(MessageReaderType.ADMIN.name(),  lastReadMessageId);

        // 관리자 활동 시간 업데이트 (담당자가 있을 경우)
        chatRoom.updateAdminActivity();
        chatRoomRepository.save(chatRoom);

        // WebSocket을 통해 상대방(사용자)에게 읽음 상태 변경 알림
        webSocketBroadcaster.broadcastUnreadCountUpdate(expoId, roomCode, 0L);

    }

    private void resetAndRecalculateBadgeCount(String roomCode, Long memberId) {
        chatMessageCacheRepository.resetUnreadCount(roomCode, memberId);
        List<String> activeRooms = chatRoomCacheRepository.getUserActiveRooms(memberId);
        chatMessageCacheRepository.recalculateBadgeCount(activeRooms, memberId);
    }
}
