package com.myce.api.service.impl;

import com.myce.api.dto.message.type.MessageReaderType;
import com.myce.api.service.ChatUnreadService;
import com.myce.api.util.ChatMessageTypeUtil;
import com.myce.api.util.RoomCodeSupporter;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.type.MessageSenderType;
import com.myce.domain.repository.ChatMessageRepository;
import java.util.Map;
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
    
    private final ChatMessageRepository chatMessageRepository;
    
    @Override
    public long getUnreadCount(
            String roomCode,
            Map<String, Long> readStatus,
            Long memberId,
            Role role,
            LoginType loginType
    ) {
        log.debug("[UnreadService] Start to calculate unread count. roomCode={}, readStatus={} memberId={}, "
                        + "role={}", roomCode, readStatus, memberId, role);

        MessageReaderType readerType = ChatMessageTypeUtil.getReaderType(roomCode, memberId, role, loginType);

        String targetSenderType = ChatMessageTypeUtil
                .getCounterpartSenderType(roomCode, memberId, role, loginType)
                .name();
        if (readStatus == null || !readStatus.containsKey(readerType.name())) {
            return chatMessageRepository.countByRoomCodeAndSenderType(roomCode, targetSenderType);
        }

        Long lastReadSeq = readStatus.get(readerType.name());
        if (lastReadSeq == null) {
            return chatMessageRepository.countByRoomCodeAndSenderType(roomCode, targetSenderType);
        }

        return chatMessageRepository.countByRoomCodeAndSenderTypeAndSeqGreaterThan(
                roomCode, targetSenderType, lastReadSeq);
    }

    public boolean isReadMessage(ChatMessage message, Map<String, Long> readStatus) {
        Long messageSeq = message.getSeq();
        log.debug("[ChatRead] Check read message. messageSeq={}, readStatus{}", messageSeq, readStatus);

        if (readStatus == null) return false;

        String roomCode = message.getRoomCode();
        MessageSenderType senderType = message.getSenderType();
        boolean isRead = isReadBySenderType(messageSeq, readStatus, senderType);

        if (!isRead && RoomCodeSupporter.isPlatformRoom(roomCode)) {
            Long aiLastReadSeq = readStatus.get(MessageReaderType.AI.name());
            if (aiLastReadSeq != null) isRead = isBeforeMessage(messageSeq, aiLastReadSeq);
        }

        log.debug("[ChatRead] Check read message. messageSeq={}, isRead={}", messageSeq, isRead);
        return isRead;
    }

    private boolean isReadBySenderType(Long messageSeq, Map<String, Long> readStatus, MessageSenderType senderType) {
        String readerType = ChatMessageTypeUtil.getReaderTypeBySender(senderType).name();
        Long lastReadSeq = readStatus.get(readerType);
        if (lastReadSeq != null) return isBeforeMessage(messageSeq, lastReadSeq);

        return false;
    }

    private boolean isBeforeMessage(Long currentMessageSeq, Long lastReadSeq) {
        return currentMessageSeq != null && lastReadSeq != null && currentMessageSeq <= lastReadSeq;
    }
}
