package com.myce.api.mapper;

import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.domain.document.ChatMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatMessageMapper {

    public static ChatMessageResponse toResponse(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .roomCode(chatMessage.getRoomCode())
                .messageId(chatMessage.getId())
                .senderId(chatMessage.getSenderId())
                .senderType(chatMessage.getSenderType())
                .senderName(chatMessage.getSenderName())
                .content(chatMessage.getContent())
                .sentAt(chatMessage.getSentAt())
                .build();
    }
    
    /**
     * unreadCount를 포함한 DTO 변환
     */
    public static ChatMessageResponse toResponse(ChatMessage chatMessage, int unreadCount) {
        return ChatMessageResponse.builder()
                .roomCode(chatMessage.getRoomCode())
                .messageId(chatMessage.getId())
                .senderId(chatMessage.getSenderId())
                .senderType(chatMessage.getSenderType())
                .senderName(chatMessage.getSenderName())
                .content(chatMessage.getContent())
                .sentAt(chatMessage.getSentAt())
                .unreadCount(unreadCount)
                .build();
    }

    /**
     * 관리자 정보와 unreadCount를 포함한 DTO 변환
     */
    public static ChatMessageResponse toResponse(ChatMessage chatMessage, int unreadCount, String adminCode,
            String adminDisplayName) {
        return ChatMessageResponse.builder()
                .roomCode(chatMessage.getRoomCode())
                .messageId(chatMessage.getId())
                .senderId(chatMessage.getSenderId())
                .senderType(chatMessage.getSenderType())
                .senderName(chatMessage.getSenderName())
                .adminCode(adminCode)
                .adminDisplayName(adminDisplayName)
                .content(chatMessage.getContent())
                .sentAt(chatMessage.getSentAt())
                .unreadCount(unreadCount)
                .build();
    }
}