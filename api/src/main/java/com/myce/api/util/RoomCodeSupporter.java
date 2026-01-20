package com.myce.api.util;

import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RoomCodeSupporter {
    private static final String ADMIN_ROOM_PREFIX = "admin-";
    private static final String ROOM_DELIMITER = "-";
    private static final String PLATFORM_ROOM_PREFIX = "platform-";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+");

    public static String getPlatformRoomCode(Long memberId) {
        return PLATFORM_ROOM_PREFIX + memberId;
    }

    public static String getAdminRoomCode(Long expoId, Long memberId) {
        return ADMIN_ROOM_PREFIX + expoId + ROOM_DELIMITER + memberId;
    }

    public static boolean isPlatformRoom(String roomCode) {
        return roomCode.startsWith(PLATFORM_ROOM_PREFIX);
    }

    public static boolean isAdminRoom(String roomCode) {
        return roomCode.startsWith(ADMIN_ROOM_PREFIX);
    }

    /**
     * 룸 코드에서 박람회 ID 추출
     */
    public static Long extractExpoIdFromAdminRoomCode(String roomCode) {
        if (roomCode == null || !roomCode.startsWith(ADMIN_ROOM_PREFIX)) return null;
        String[] parts = roomCode.split(ROOM_DELIMITER);
        if (parts.length < 3 || !isNumber(parts[1])) return null;

        return Long.parseLong(parts[1]);
    }

    public static Long extractExpoIdFromRoomCode(String roomCode) {
        String[] parts = roomCode.split(ROOM_DELIMITER);
        return Long.parseLong(parts[1]);
    }

    public static Long extractMemberIdFromRoomCode(String roomCode) {
        String[] parts = roomCode.split(ROOM_DELIMITER);
        return Long.parseLong(parts[2]);
    }

    public static Long extractMemberIdFromPlatformRoomCode(String roomCode) {
        String[] parts = roomCode.split(ROOM_DELIMITER);
        return Long.parseLong(parts[1]);
    }

    /**
     * roomCode 형식 검증
     */
    public static boolean isValidRoomCodeFormat(String roomCode) {
        if (roomCode == null) return false;

        if (roomCode.startsWith(PLATFORM_ROOM_PREFIX)) {
            return isValidPlatformRoomCodeFormat(roomCode);
        } else if (roomCode.startsWith(ADMIN_ROOM_PREFIX)) {
            return isValidRoomAdminroomCodeFormat(roomCode);
        }

        return false;
    }

    // 플랫폼 방 형식: platform-{memberId}
    private static boolean isValidPlatformRoomCodeFormat(String roomCode) {
        String[] parts = roomCode.split(ROOM_DELIMITER);
        if (parts.length != 2) return false;
        return isNumber(parts[1]);
    }

    // 일반 채팅방 형식: admin-{expoId}-{userId}
    private static boolean isValidRoomAdminroomCodeFormat(String roomCode) {
        String[] parts = roomCode.split(ROOM_DELIMITER);
        if (parts.length != 3) return false;
        return isNumber(parts[1]) && isNumber(parts[2]);
    }

    private static boolean isNumber(String numberStr) {
        return NUMBER_PATTERN.matcher(numberStr).matches();
    }
}
