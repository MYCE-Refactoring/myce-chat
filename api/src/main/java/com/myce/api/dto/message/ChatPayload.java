package com.myce.api.dto.message;

import com.myce.domain.document.type.MessageSenderType;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ChatPayload {
    private final String roomCode;
    private final String messageId;
    private final Long seq;
    private final Long senderId;
    private final MessageSenderType senderType;
    private final String senderName;
    private final String content;
    private final int unreadCount;
    private final LocalDateTime sentAt;

    private String adminCode;
    private String adminDisplayName;

    public ChatPayload(String roomCode, String messageId, Long seq, Long senderId, MessageSenderType senderType,
            String senderName, String content, int unreadCount, LocalDateTime sentAt) {
        this.roomCode = roomCode;
        this.messageId = messageId;
        this.seq = seq;
        this.senderId = senderId;
        this.senderType = senderType;
        this.senderName = senderName;
        this.content = content;
        this.unreadCount = unreadCount;
        this.sentAt = sentAt;
    }

    public void addAdminInfo(String adminCode, String adminDisplayName) {
        this.adminCode = adminCode;
        this.adminDisplayName = adminDisplayName;
    }
}
