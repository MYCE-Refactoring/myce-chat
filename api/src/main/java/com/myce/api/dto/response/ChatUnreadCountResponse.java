package com.myce.api.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class ChatUnreadCountResponse {
    Long totalUnreadCount;
    List<RoomUnreadCount> unreadCounts;

    public ChatUnreadCountResponse(Long totalUnreadCount) {
        this.totalUnreadCount = totalUnreadCount;
        unreadCounts = new ArrayList<>();
    }

    public void updateTotalUnreadCount(Long totalUnreadCount) {
        this.totalUnreadCount = totalUnreadCount;
    }

    public void addRoomUnreadCount(String roomCode, Long unreadCount) {
        unreadCounts.add(new RoomUnreadCount(roomCode, unreadCount));
    }

    @Getter
    @AllArgsConstructor
    public static class RoomUnreadCount {
        String roomCode;
        Long unreadCount;
    }
}
