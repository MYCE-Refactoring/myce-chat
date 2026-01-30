package com.myce.domain.repository;

import com.myce.domain.document.type.MessageSenderType;

public interface ChatMessageRepositoryCustom {

    /**
     * 동일한 채팅방에서 기준 seq 이전(포함)의 다른 발송자 메시지 unreadCount 감소
     */
    void decreaseUnreadCountBeforeSeq(String roomCode, MessageSenderType senderType, Long lastReadSeq);

    void updateUnreadCountEqualSeq(String roomCode, String messageId);
}
