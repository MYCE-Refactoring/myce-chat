package com.myce.api.dto.message;

import com.myce.api.dto.message.type.MessageReaderType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatUnReadCountPayload {
    private String roomCode;
    private MessageReaderType messageReaderType;
    private long unReadCount;
}
