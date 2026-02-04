package com.myce.api.service;

import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.common.dto.PageResponse;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.type.MessageSenderType;
import java.util.List;

/**
 * 채팅 메시지 생성 서비스
 */

public interface ChatMessageService {

    ChatMessage saveAIChatMessage(String roomCode, String content);

    ChatMessage saveSystemChatMessage(String roomCode, String content);

    ChatMessage saveChatMessage(String roomCode, MessageSenderType senderType,
            Long senderId, String senderName, String content);

    List<ChatMessage> getRecentMessages(String roomCode);

    /**
     * 특정 채팅방의 읽지 않은 메시지 수 조회 (역할 기반 접근 제어)
     */
    Long getUnreadCount(String roomCode, Long memberId, String memberRole, LoginType loginType);

    ChatMessage getRecentMessage(String roomCode);

    /**
     * 채팅방의 메시지 히스토리 조회 (페이징)
     */
    PageResponse<ChatMessageResponse> getMessages(String roomCode, int page, int size, Long memberId, Role role);
}
