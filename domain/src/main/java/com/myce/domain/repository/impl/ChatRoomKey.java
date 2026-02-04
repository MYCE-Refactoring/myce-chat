package com.myce.domain.repository.impl;

public final class ChatRoomKey {
    public static final String ROOM_KEY_PREFIX = "chat:room:";
    public static final String ROOM_KEY_FORMAT = ROOM_KEY_PREFIX + "%s";
    public static final String ROOM_RECENT_KEY_FORMAT = ROOM_KEY_PREFIX + "%s:recent";
    public static final String ROOM_UNREAD_KEY_FORMAT = ROOM_KEY_PREFIX + "%s:unread:%d";
    public static final String ROOM_LAST_READ_KEY_FORMAT = ROOM_KEY_PREFIX + "%s:lastRead:%d";
    public static final String USER_BADGE_KEY_FORMAT = "chat:user:%d:badge";
    public static final String USER_ACTIVE_ROOMS_KEY_FORMAT = "chat:user:%d:activeRooms";
}
