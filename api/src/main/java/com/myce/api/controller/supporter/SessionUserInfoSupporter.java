package com.myce.api.controller.supporter;

import com.myce.api.dto.WebSocketUserInfo;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

public class SessionUserInfoSupporter {

    public static final String SESSION_USER_INFO_ID = "USER_INFO";

    public static void validateUserInfo(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttribute = headerAccessor.getSessionAttributes();
        if (sessionAttribute == null || !sessionAttribute.containsKey(SESSION_USER_INFO_ID)) {
            throw new CustomException(CustomErrorCode.INVALID_MEMBER);
        }
    }

    public static WebSocketUserInfo getUserInfo(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttribute = headerAccessor.getSessionAttributes();
        if (sessionAttribute == null || !sessionAttribute.containsKey(SESSION_USER_INFO_ID)) {
            throw new CustomException(CustomErrorCode.INVALID_MEMBER);
        }

        return (WebSocketUserInfo) sessionAttribute.get(SESSION_USER_INFO_ID);
    }
}
