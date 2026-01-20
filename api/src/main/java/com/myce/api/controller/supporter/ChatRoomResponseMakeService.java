package com.myce.api.controller.supporter;

import com.myce.api.dto.response.ChatRoomInfoListResponse;
import com.myce.api.dto.response.ChatRoomInfoResponse;
import com.myce.api.mapper.ChatRoomMapper;
import com.myce.api.service.ChatUnreadService;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatRoom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomResponseMakeService {

    private final ChatUnreadService chatUnreadService;

    public ChatRoomInfoListResponse convertToResponse
            (List<ChatRoom> chatRooms, Long viewerId, Role viewerRole) {

        Map<String, Long> chatRoomUnreadCounts =
                getUnreadCounts(chatRooms, viewerId, viewerRole);

        return ChatRoomMapper.convertToResponse(chatRooms, chatRoomUnreadCounts);
    }

    public List<ChatRoomInfoResponse> convertToListForResponse
            (List<ChatRoom> chatRooms, Long viewerId, Role viewerRole) {

        Map<String, Long> chatRoomUnreadCounts =
                getUnreadCounts(chatRooms, viewerId, viewerRole);

        List<ChatRoomInfoResponse> responseList = new ArrayList<>();
        for (ChatRoom chatRoom : chatRooms) {
            long unreadCount = chatRoomUnreadCounts.get(chatRoom.getRoomCode());
            responseList.add(ChatRoomMapper.convertToResponse(chatRoom, unreadCount));
        }

        return responseList;
    }

    private Map<String, Long> getUnreadCounts
            (List<ChatRoom> chatRooms, Long viewerId, Role viewerRole) {
        Map<String, Long> chatRoomUnreadCounts = new HashMap<>();
        for (ChatRoom chatRoom : chatRooms) {
            String roomCode = chatRoom.getRoomCode();

            long unreadCount = chatUnreadService.getUnreadCountForViewer
                    (roomCode, chatRoom.getReadStatus(), viewerId, viewerRole);
            chatRoomUnreadCounts.put(roomCode, unreadCount);
        }

        return chatRoomUnreadCounts;
    }

}
