package com.myce.api.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConversationSummaryResponse {
    private String roomCode;
    private String summary;
    private LocalDateTime generatedAt;
}
