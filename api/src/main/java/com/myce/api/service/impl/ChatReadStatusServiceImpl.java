package com.myce.api.service.impl;

import com.myce.api.dto.message.ChatUnReadCountPayload;
import com.myce.api.dto.message.WebSocketBaseMessage;
import com.myce.api.dto.message.type.BroadcastType;
import com.myce.api.dto.message.type.MessageReaderType;
import com.myce.api.service.ChatReadStatusService;
import com.myce.api.service.ChatWebSocketBroadcaster;
import com.myce.api.util.ChatMessageTypeUtil;
import com.myce.api.util.RoomCodeSupporter;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.document.type.MessageSenderType;
import com.myce.domain.repository.ChatMessageCacheRepository;
import com.myce.domain.repository.ChatMessageRepository;
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
    private final ChatMessageRepository chatMessageRepository;

    @Override
    public void updateChatReadStatus(String roomCode, MessageReaderType readerType) {
        ChatUnReadCountPayload readStatusDto = new ChatUnReadCountPayload(roomCode, readerType, 0);
        WebSocketBaseMessage message = new WebSocketBaseMessage(BroadcastType.READ_STATUS_UPDATE, readStatusDto);
        webSocketBroadcaster.sendMessage(roomCode, message);
    }

    @Override
    @Transactional
    public void markAsReadForMember(String roomCode, Long lastReadSeq, Long memberId, Role role,
            LoginType loginType) {
        log.debug("[ChatReadStatusService] Mark as read for member. roomCode={}, lastReadSeq={}, memberId={}, "
                        + "LoginType={}", roomCode, lastReadSeq, memberId, loginType);
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        boolean isPlatformRoom = RoomCodeSupporter.isPlatformRoom(roomCode);
        accessCheckService.validateAccess(isPlatformRoom, chatRoom.getMemberId(), chatRoom.getExpoId(), memberId, role);

        log.debug("[ChatReadStatusService] Redis unread count reset for admin: {} in room: {}", memberId, roomCode);

        MessageReaderType readerType = ChatMessageTypeUtil.getReaderType(roomCode, memberId, role, loginType);
        if (lastReadSeq != null) {
            chatRoom.updateReadStatus(readerType.name(), lastReadSeq);
            resetAndRecalculateBadgeCount(roomCode, memberId);
        }
        chatRoomRepository.save(chatRoom);

        MessageSenderType readerSenderType = ChatMessageTypeUtil.getSenderType(
                roomCode, memberId, role, loginType);
        if (lastReadSeq != null) {
            decreaseUnreadCount(roomCode, readerSenderType, lastReadSeq);
        }

        log.debug("[ChatReadStatusService] Mark as read for member. roomCode={}, memberId={}, lastReadSeq={}",
                roomCode, memberId, lastReadSeq);
    }

    @Override
    @Transactional
    public void markAsReadForAdmin(Long expoId, String roomCode, Long lastReadSeq, Long memberId, Role role,
            LoginType loginType) {
        log.debug("[ChatReadStatusService] Mark as read for admin. expoId={}, roomCode={}, lastReadSeq={}, "
                + "memberId={}", expoId, roomCode, lastReadSeq, memberId);

        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        if (!chatRoom.getExpoId().equals(expoId)) {
            throw new CustomException(CustomErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        boolean isPlatformRoom = RoomCodeSupporter.isPlatformRoom(roomCode);
        accessCheckService.validateAccess(isPlatformRoom, chatRoom.getMemberId(), chatRoom.getExpoId(), memberId, role);

        log.debug("[ChatReadStatusService] Redis unread count reset for admin: {} in room: {}", memberId, roomCode);

        // 마지막 읽은 메시지 seq Redis 저장
        if (lastReadSeq != null) {
            chatMessageCacheRepository.setLastReadSeq(roomCode, memberId, lastReadSeq);
            resetAndRecalculateBadgeCount(roomCode, memberId);
        }

        // 마지막 메시지 seq를 기준으로 읽음 처리
        MessageReaderType readerType = ChatMessageTypeUtil.getReaderType(roomCode, memberId, role, loginType);
        if (lastReadSeq != null) {
            chatRoom.updateReadStatus(readerType.name(), lastReadSeq);
        }

        // 관리자 활동 시간 업데이트 (담당자가 있을 경우)
        chatRoom.updateAdminActivity();
        chatRoomRepository.save(chatRoom);

        MessageSenderType readerSenderType = ChatMessageTypeUtil.getSenderType(
                roomCode, memberId, role, loginType);
        if (lastReadSeq != null) {
            decreaseUnreadCount(roomCode, readerSenderType, lastReadSeq);
        }

        // WebSocket을 통해 상대방(사용자)에게 읽음 상태 변경 알림
        webSocketBroadcaster.broadcastUnreadCountUpdate(roomCode, MessageReaderType.USER, 0L);
    }

    private void resetAndRecalculateBadgeCount(String roomCode, Long memberId) {
        chatMessageCacheRepository.resetUnreadCount(roomCode, memberId);
        List<String> activeRooms = chatRoomCacheRepository.getUserActiveRooms(memberId);
        chatMessageCacheRepository.recalculateBadgeCount(activeRooms, memberId);
    }

    private void decreaseUnreadCount(String roomCode, MessageSenderType readerSenderType, Long lastReadSeq) {
        if (readerSenderType == null) return;

        chatMessageRepository.decreaseUnreadCountBeforeSeq(roomCode, readerSenderType, lastReadSeq);
    }

}
