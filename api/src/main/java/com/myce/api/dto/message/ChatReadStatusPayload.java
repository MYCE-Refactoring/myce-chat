package com.myce.api.dto.message;

import com.myce.api.dto.message.type.MessageReaderType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatReadStatusPayload {
    private String messageId;
    private Long readBy;
    private MessageReaderType messageReaderType;
    private LocalDateTime timestamp;

}
