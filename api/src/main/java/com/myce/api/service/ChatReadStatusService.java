package com.myce.api.service;

import com.myce.api.dto.message.type.MessageReaderType;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;

public interface ChatReadStatusService {

    void updateChatReadStatus(String roomCode, MessageReaderType readerType);
    void markAsReadForMember(String roomCode, Long lastReadSeq, Long memberId, Role role, LoginType loginType);
    void markAsReadForAdmin(Long expoId, String roomCode, Long lastReadSeq, Long memberId, Role role,
            LoginType loginType);
}
