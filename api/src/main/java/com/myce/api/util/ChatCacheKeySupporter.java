package com.myce.api.util;

import com.myce.api.dto.message.type.MessageReaderType;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;

public final class ChatCacheKeySupporter {

    // Shared cache bucket for all admins
    public static final Long ADMIN_GROUP_MEMBER_ID = -1L;

    private ChatCacheKeySupporter() {
    }

    public static Long resolveCacheMemberId(MessageReaderType readerType, Long memberId) {
        if (MessageReaderType.ADMIN.equals(readerType)) {
            return ADMIN_GROUP_MEMBER_ID;
        }
        return memberId;
    }

    public static Long resolveCacheMemberId(String roomCode, Long memberId, Role role, LoginType loginType) {
        MessageReaderType readerType = ChatMessageTypeUtil.getReaderType(roomCode, memberId, role, loginType);
        return resolveCacheMemberId(readerType, memberId);
    }
}
