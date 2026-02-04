package com.myce.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatStatusResponse {
    private String roomCode;
    private boolean isAiEnabled;
    private String roomType;
}
