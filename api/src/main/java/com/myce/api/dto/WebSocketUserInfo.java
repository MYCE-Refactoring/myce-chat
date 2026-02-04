package com.myce.api.dto;

import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WebSocketUserInfo {
    private Role role;
    private String name;
    private Long memberId;
    private LoginType loginType;
}
