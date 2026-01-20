package com.myce.api.service;

import com.myce.api.dto.message.type.MessageReaderType;
import com.myce.common.type.Role;

public interface ChatReadStatusService {

    void updateChatReadStatus(String roomCode, MessageReaderType readerType);
    void markAsReadForMember(String roomCode, String lastReadMessageId, Long memberId, Role role);
    void markAsReadForAdmin(Long expoId, String roomCode, String lastReadMessageId, Long memberId);
}
