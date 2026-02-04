package com.myce.api.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatMessagesResponse {
    private final List<ChatMessageResponse> messages;
    private final int page;
    private final int size;
    private final int totalPage;
}
