package com.myce.api.service.ai;

import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.repository.ChatMessageRepository;
import com.myce.domain.repository.ChatRoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatDataFacade {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public boolean checkRoomIsWaitingAdmin(String roomCode){
        ChatRoom chatRoom = getChatRoom(roomCode);
        return chatRoom.isWaitingForAdmin();
    }

    public List<ChatMessage> getRecentMessages(String roomCode){
        return chatMessageRepository
                .findTop50ByRoomCodeOrderBySentAtDesc(roomCode);
    }

    public ChatRoom getChatRoom(String roomCode) {
        return chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));
    }
}
