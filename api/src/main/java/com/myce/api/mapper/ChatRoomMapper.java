package com.myce.api.mapper;

import com.myce.api.dto.response.ChatRoomInfoListResponse;
import com.myce.api.dto.response.ChatRoomInfoResponse;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatRoom;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ChatRoomMapper {

    public static ChatRoomInfoListResponse convertToResponse
            (List<ChatRoom> chatRooms, Map<String, Long> chatRoomUnreadCounts) {

        ChatRoomInfoListResponse response = new ChatRoomInfoListResponse();
        for (ChatRoom chatRoom : chatRooms) {
            long unreadCount = chatRoomUnreadCounts.get(chatRoom.getRoomCode());
            ChatRoomInfoResponse chatRoomInfo = convertToResponse(chatRoom, unreadCount);
            response.addChatRoomInfo(chatRoomInfo);
        }

        return response;
    }

    public static ChatRoomInfoResponse convertToResponse(ChatRoom chatRoom, long unreadCount) {
        return ChatRoomInfoResponse.builder()
                .id(chatRoom.getId())
                .roomCode(chatRoom.getRoomCode())
                .expoId(chatRoom.getExpoId())
                .expoTitle(chatRoom.getRoomTitle())
                .otherMemberId(chatRoom.getMemberId())
                .otherMemberName(chatRoom.getMemberName())
                .otherMemberRole(chatRoom.getExpoId() == null ? Role.USER.name() : chatRoom.getRoomTitle())
                .lastMessage(chatRoom.getLastMessage())
                .lastMessageAt(chatRoom.getLastMessageAt())
                .unreadCount(unreadCount)
                .isActive(chatRoom.getIsActive())
                .currentAdminCode(chatRoom.getCurrentAdminCode())
                .adminDisplayName(chatRoom.getAdminDisplayName())
                .currentState(chatRoom.getCurrentState() != null ? chatRoom.getCurrentState().name() : null)
                .build();
    }
}
