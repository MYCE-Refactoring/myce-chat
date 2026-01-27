package com.myce.api.service.component;

import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.type.MessageSenderType;
import com.myce.domain.repository.SequenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessageCreateComponent {

    private static final long AI_SENDER_ID = -1;
    private static final long SYSTEM_SENDER_ID = -99;
    private static final String DEFAULT_MESSAGE_TYPE = "TEXT";
    private static final int DEFAULT_UNREAD_COUNT = 1;

    private final SequenceGenerator sequenceGenerator;

    public ChatMessage createMessage(
            String roomCode, MessageSenderType senderType,
            Long senderId, String senderName, String content) {

        return  ChatMessage.builder()
                .roomCode(roomCode)
                .seq(sequenceGenerator.generateSequence(ChatMessage.SEQUENCE_NAME))
                .senderType(senderType)
                .senderId(senderId)
                .senderName(senderName)
                .content(content)
                .isSystemMessage(true)
                .unreadCount(DEFAULT_UNREAD_COUNT)
                .messageType(DEFAULT_MESSAGE_TYPE)
                .build();
    }

    public ChatMessage createSystemMessage(String roomCode, String content) {
        return ChatMessage.builder()
                .roomCode(roomCode)
                .seq(sequenceGenerator.generateSequence(ChatMessage.SEQUENCE_NAME))
                .senderType(MessageSenderType.SYSTEM)
                .senderId(SYSTEM_SENDER_ID)
                .senderName(MessageSenderType.SYSTEM.getDescription())
                .content(content)
                .isSystemMessage(true)
                .unreadCount(0)
                .messageType(DEFAULT_MESSAGE_TYPE)
                .build();

    }

    public ChatMessage createAIMessage(String roomCode, String content) {
        return ChatMessage.builder()
                .roomCode(roomCode)
                .seq(sequenceGenerator.generateSequence(ChatMessage.SEQUENCE_NAME))
                .senderType(MessageSenderType.AI)
                .senderId(AI_SENDER_ID)
                .senderName(MessageSenderType.AI.getDescription())
                .content(content)
                .isSystemMessage(true)
                .unreadCount(0)
                .messageType(DEFAULT_MESSAGE_TYPE)
                .build();

    }

}
