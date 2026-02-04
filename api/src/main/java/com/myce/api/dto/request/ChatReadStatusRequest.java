package com.myce.api.dto.request;

import com.myce.api.dto.message.type.MessageReaderType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatReadStatusRequest {
    private String roomCode;
    private MessageReaderType readerType;
}
