package com.myce.common.type;

import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum Role {
    PLATFORM_ADMIN("플랫폼 관리자"),
    USER("사용자"),
    EXPO_SUPER_ADMIN("박람회 최고 관리자"),
    EXPO_ADMIN("박람회 관리자");

    @Getter
    private final String displayName;

    public static Role fromName(String name) {
        for(Role role : Role.values()) {
            if(role.name().equals(name)) {
                return role;
            }
        }

        return null;
    }
}
