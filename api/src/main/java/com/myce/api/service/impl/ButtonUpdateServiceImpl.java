package com.myce.api.service.impl;

import com.myce.api.dto.message.ButtonStatePayload;
import com.myce.api.dto.message.ChatRoomStateInfo;
import com.myce.api.dto.message.WebSocketChatMessage;
import com.myce.api.dto.message.type.BroadcastType;
import com.myce.api.dto.message.type.TransitionReason;
import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.api.service.ButtonUpdateService;
import com.myce.api.service.ChatWebSocketBroadcaster;
import com.myce.api.util.ChatRoomStateSupporter;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.document.type.ChatRoomState;
import com.myce.domain.repository.ChatRoomRepository;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ButtonUpdateServiceImpl implements ButtonUpdateService {

    private final ChatWebSocketBroadcaster broadcaster;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * 버튼 상태 업데이트 브로드캐스트 (상태 기반)
     */
    public void sendButtonStateUpdate(String roomCode, ChatRoomState newState) {
        sendButtonStateUpdate(roomCode, newState, (ChatMessage) null);
    }

    public void sendButtonStateUpdate(String roomCode, ChatRoomState newState, ChatMessage message) {
        ChatRoom currentRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        ChatRoomStateInfo chatRoomStateInfo = ChatRoomStateSupporter.createRoomStateInfo(
                currentRoom,
                TransitionReason.BUTTON_STATE_UPDATE);

        ButtonStatePayload payload = ButtonStatePayload.of(roomCode, newState, message);
        WebSocketChatMessage messagePayload = new WebSocketChatMessage(
                BroadcastType.BUTTON_STATE_UPDATE,
                payload,
                chatRoomStateInfo
        );

        broadcaster.sendMessage(roomCode, messagePayload);
    }

    public void sendButtonStateUpdate(String roomCode, ChatRoomState newState, ChatMessageResponse message) {
        ChatRoom currentRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        ChatRoomStateInfo chatRoomStateInfo = ChatRoomStateSupporter.createRoomStateInfo(
                currentRoom,
                TransitionReason.BUTTON_STATE_UPDATE);

        ButtonStatePayload payload = ButtonStatePayload.of(roomCode, newState, message);
        WebSocketChatMessage messagePayload = new WebSocketChatMessage(
                BroadcastType.BUTTON_STATE_UPDATE,
                payload,
                chatRoomStateInfo
        );

        broadcaster.sendMessage(roomCode, messagePayload);
    }
}
