package com.myce.api.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class ChatRoomInfoListResponse {

    private int totalCount;
    private final List<ChatRoomInfoResponse> chatRooms;

    public ChatRoomInfoListResponse() {
        this.chatRooms = new ArrayList<>();
    }

    public void addChatRoomInfo(ChatRoomInfoResponse chatRoomInfo) {
        this.chatRooms.add(chatRoomInfo);
        this.totalCount++;
    }
}