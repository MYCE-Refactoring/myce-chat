package com.myce.api.dto.message.type;

public final class WebSocketDestination {
    // topic
    public static final String AUTH_TEST = "/topic/auth-test";
    public static final String USER_ERROR = "/topic/user/errors";
    public static final String ADMIN_HANDOFF_NOTIFICATION = "/topic/platform/admin-updates";
    private static final String SEND_CHAT_MESSAGE_FORMAT = "/topic/chat/%s";
    private static final String ADMIN_UPDATE_STATE = "/topic/expo/%s/admin-updates";
    public static final String CHAT_ROOM_STATE = "/topic/chat-room-updates";

    public static final String ERROR = "/queue/errors";

    public static String getSendChatMessageDestination(String roomCode) {
        return String.format(SEND_CHAT_MESSAGE_FORMAT, roomCode);
    }

    public static String getAdminUpdateStateDestination(Long expoId) {
        return String.format(ADMIN_UPDATE_STATE, expoId);
    }

}