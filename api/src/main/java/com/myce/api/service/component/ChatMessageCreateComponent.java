package com.myce.api.service.component;

import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.type.MessageSenderType;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageCreateComponent {

    private static final long AI_SENDER_ID = -1;
    private static final long SYSTEM_SENDER_ID = -99;
    private static final String DEFAULT_MESSAGE_TYPE = "TEXT";

    public ChatMessage createMessage(
            String roomCode, MessageSenderType senderType,
            Long senderId, String senderName, String content) {

        return ChatMessage.builder()
                .roomCode(roomCode)
                .senderType(senderType)
                .senderId(senderId)
                .senderName(senderName)
                .content(content)
                .isSystemMessage(true)
                .messageType(DEFAULT_MESSAGE_TYPE)
                .build();
    }

    public ChatMessage createSystemMessage(String roomCode, String content) {
        return ChatMessage.builder()
                .roomCode(roomCode)
                .senderType(MessageSenderType.SYSTEM)
                .senderId(SYSTEM_SENDER_ID)
                .senderName(MessageSenderType.SYSTEM.getDescription())
                .content(content)
                .isSystemMessage(true)
                .messageType(DEFAULT_MESSAGE_TYPE)
                .build();
    }

    public ChatMessage createAIMessage(String roomCode, String content) {
        return ChatMessage.builder()
                .roomCode(roomCode)
                .senderType(MessageSenderType.AI)
                .senderId(AI_SENDER_ID)
                .senderName(MessageSenderType.AI.getDescription())
                .content(content)
                .isSystemMessage(true)
                .messageType(DEFAULT_MESSAGE_TYPE)
                .build();
    }

}
