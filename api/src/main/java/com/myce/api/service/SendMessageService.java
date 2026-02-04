package com.myce.api.service;


import com.myce.api.dto.message.ChatRoomStateInfo;
import com.myce.api.dto.request.SendMessageRequest;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatMessage;

public interface SendMessageService {
    void sendChatMessage(Long memberId, Role role, LoginType loginType, String sessionId, SendMessageRequest request);
    void sendAdminChatMessage(Long memberId, Role role, LoginType loginType, String sessionId,
            SendMessageRequest request);
    void sendSystemMessage(String roomCode, ChatRoomStateInfo roomState, ChatMessage chatMessage);
}
