package com.myce.api.mapper;

import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.domain.document.ChatMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatMessageMapper {

    /**
     * unreadCount를 포함한 DTO 변환
     */
    public static ChatMessageResponse toResponse(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .roomCode(chatMessage.getRoomCode())
                .messageId(chatMessage.getId())
                .seq(chatMessage.getSeq())
                .senderId(chatMessage.getSenderId())
                .senderType(chatMessage.getSenderType())
                .senderName(chatMessage.getSenderName())
                .content(chatMessage.getContent())
                .sentAt(chatMessage.getSentAt())
                .unreadCount(chatMessage.getUnreadCount())
                .build();
    }

    /**
     * 관리자 정보와 unreadCount를 포함한 DTO 변환
     */
    public static ChatMessageResponse toResponse(ChatMessage chatMessage, String adminCode, String adminDisplayName) {
        return ChatMessageResponse.builder()
                .roomCode(chatMessage.getRoomCode())
                .messageId(chatMessage.getId())
                .seq(chatMessage.getSeq())
                .senderId(chatMessage.getSenderId())
                .senderType(chatMessage.getSenderType())
                .senderName(chatMessage.getSenderName())
                .adminCode(adminCode)
                .adminDisplayName(adminDisplayName)
                .content(chatMessage.getContent())
                .sentAt(chatMessage.getSentAt())
                .unreadCount(chatMessage.getUnreadCount())
                .build();
    }
}
