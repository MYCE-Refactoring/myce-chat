package com.myce.api.dto.message;

import com.myce.domain.document.type.MessageSenderType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRoomPreviewPayload {
    private String roomCode;
    private String lastMessageId;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private MessageSenderType senderType;
}
