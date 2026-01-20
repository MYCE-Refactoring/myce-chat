package com.myce.api.dto.message;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HandoffNotificationInfo {
    private String roomCode;
    private Long userId;
    private String userName;
    private LocalDateTime sendAt;
}
