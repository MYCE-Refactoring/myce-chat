package com.myce.api.util;

import com.myce.api.dto.message.type.MessageReaderType;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import com.myce.domain.document.type.MessageSenderType;
import com.myce.api.util.RoomCodeSupporter;

public class ChatMessageTypeUtil {

    public static MessageSenderType getSenderType(Role role) {
        if (role == null) {
            return MessageSenderType.USER;
        }

        return Role.USER.equals(role) ? MessageSenderType.USER : MessageSenderType.ADMIN;
    }

    public static MessageReaderType getReaderTypeBySender(MessageSenderType role) {
        if (MessageSenderType.ADMIN.equals(role)
                || MessageSenderType.PLATFORM_ADMIN.equals(role)
                || MessageSenderType.AI.equals(role)) {
            return MessageReaderType.USER;
        }

        return MessageReaderType.ADMIN;
    }

    public static MessageReaderType getReaderTypeByRole(Role role) {
        if (role == null) {
            return MessageReaderType.USER;
        }

        return Role.EXPO_SUPER_ADMIN.equals(role)
                || Role.EXPO_ADMIN.equals(role)
                || Role.PLATFORM_ADMIN.equals(role)
                ? MessageReaderType.ADMIN
                : MessageReaderType.USER;
    }

    public static MessageReaderType getReaderType(
            String roomCode,
            Long memberId,
            Role role,
            LoginType loginType
    ) {
        if (roomCode == null || memberId == null) {
            return getReaderTypeByRole(role);
        }

        if (isRoomParticipant(roomCode, memberId)) {
            return MessageReaderType.USER;
        }

        if (RoomCodeSupporter.isPlatformRoom(roomCode)) {
            return Role.PLATFORM_ADMIN.equals(role) ? MessageReaderType.ADMIN : MessageReaderType.USER;
        }

        if (Role.EXPO_ADMIN.equals(role)
                || Role.EXPO_SUPER_ADMIN.equals(role)
                || LoginType.ADMIN_CODE.equals(loginType)) {
            return MessageReaderType.ADMIN;
        }

        return MessageReaderType.USER;
    }

    public static MessageSenderType getSenderType(
            String roomCode,
            Long memberId,
            Role role,
            LoginType loginType
    ) {
        MessageReaderType readerType = getReaderType(roomCode, memberId, role, loginType);
        if (MessageReaderType.USER.equals(readerType)) {
            return MessageSenderType.USER;
        }

        if (RoomCodeSupporter.isPlatformRoom(roomCode) && Role.PLATFORM_ADMIN.equals(role)) {
            return MessageSenderType.PLATFORM_ADMIN;
        }

        return MessageSenderType.ADMIN;
    }

    public static MessageSenderType getCounterpartSenderType(
            String roomCode,
            Long memberId,
            Role role,
            LoginType loginType
    ) {
        MessageSenderType senderType = getSenderType(roomCode, memberId, role, loginType);
        if (MessageSenderType.USER.equals(senderType)) {
            return RoomCodeSupporter.isPlatformRoom(roomCode)
                    ? MessageSenderType.PLATFORM_ADMIN
                    : MessageSenderType.ADMIN;
        }

        return MessageSenderType.USER;
    }

    private static boolean isRoomParticipant(String roomCode, Long memberId) {
        if (roomCode == null || memberId == null) {
            return false;
        }

        if (RoomCodeSupporter.isPlatformRoom(roomCode)) {
            Long roomMemberId = RoomCodeSupporter.extractMemberIdFromPlatformRoomCode(roomCode);
            return memberId.equals(roomMemberId);
        }

        if (RoomCodeSupporter.isAdminRoom(roomCode)) {
            Long roomMemberId = RoomCodeSupporter.extractMemberIdFromRoomCode(roomCode);
            return memberId.equals(roomMemberId);
        }

        return false;
    }
}
