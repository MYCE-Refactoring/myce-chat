package com.myce.api.dto;

import com.myce.common.type.Role;
import com.myce.domain.document.type.MessageSenderType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SenderInfo {
    private Role senderRole;
    private MessageSenderType senderType;
    private String senderName;
}
