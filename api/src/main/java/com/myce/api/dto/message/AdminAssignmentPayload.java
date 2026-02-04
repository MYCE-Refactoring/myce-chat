package com.myce.api.dto.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminAssignmentPayload {
    private String roomCode;
    private String currentAdminCode;
    private String adminDisplayName;
}
