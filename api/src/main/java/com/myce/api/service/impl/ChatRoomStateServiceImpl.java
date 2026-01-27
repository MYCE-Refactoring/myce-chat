package com.myce.api.service.impl;

import com.myce.api.exception.CustomWebSocketError;
import com.myce.api.exception.CustomWebSocketException;
import com.myce.api.service.AIChatService;
import com.myce.api.util.ChatRoomStateSupporter;
import com.myce.api.util.RoomCodeSupporter;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.document.type.ChatRoomState;
import com.myce.api.dto.WebSocketUserInfo;
import com.myce.api.dto.message.ChatPayload;
import com.myce.api.dto.message.ChatRoomStateInfo;
import com.myce.api.dto.message.HandoffNotificationInfo;
import com.myce.api.dto.message.WebSocketChatMessage;
import com.myce.api.dto.message.type.BroadcastType;
import com.myce.api.dto.message.type.SystemMessage;
import com.myce.api.dto.message.type.TransitionReason;
import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.domain.repository.ChatRoomRepository;
import com.myce.api.service.ButtonUpdateService;
import com.myce.api.service.ChatMessageService;
import com.myce.api.service.ChatRoomStateService;
import com.myce.api.service.ChatWebSocketBroadcaster;
import com.myce.common.exception.CustomException;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class ChatRoomStateServiceImpl implements ChatRoomStateService {

    private final AIChatService aiChatService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomRepository chatRoomRepository;
    private final ButtonUpdateService buttonUpdateService;
    private final ChatWebSocketBroadcaster webSocketBroadcaster;

    @Override
    @Transactional
    public void adminHandoff(WebSocketUserInfo userInfo, String roomCode, String sessionId) {
        log.debug("[ChatRoom-Handoff] Admin handoff. roomCode={}", roomCode);
        if (!RoomCodeSupporter.isPlatformRoom(roomCode)) {
            throw new CustomException(CustomErrorCode.ONLY_PLATFORM_CHAT_ROOM);
        }
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        // AI 서비스를 통한 핸드오프 요청 처리
        ChatMessage chatMessage = aiChatService.requestAdminHandoff(chatRoom);
        ChatRoomStateInfo chatRoomStateInfo =
                ChatRoomStateSupporter.createRoomStateInfo(chatRoom, TransitionReason.HANDOFF_REQUEST);

        buttonUpdateService.sendButtonStateUpdate(
                roomCode,
                ChatRoomState.WAITING_FOR_ADMIN,
                chatMessage
        );

        //  플랫폼 채팅방인 경우 플랫폼 관리자에게도 알림
        if (RoomCodeSupporter.isPlatformRoom(roomCode)) {
            sendNotificationToAdmin(roomCode, chatRoom.getMemberId(), chatRoom.getMemberName(), chatRoomStateInfo);
        }

        chatRoom.transitionToState(ChatRoomState.WAITING_FOR_ADMIN);
        chatRoomRepository.save(chatRoom);

        log.debug("[ChatRoom-Handoff] Success to admin handoff process. roomCode={}, memberId={}", roomCode,
                chatRoom.getMemberId());
    }

    @Override
    public void cancelHandoff(WebSocketUserInfo userInfo, String roomCode, String sessionId) {
        if (!RoomCodeSupporter.isPlatformRoom(roomCode)) {
            throw new CustomException(CustomErrorCode.ONLY_PLATFORM_CHAT_ROOM);
        }
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        // AI 서비스를 통한 핸드오프 취소 처리
        ChatMessage chatMessage = aiChatService.cancelAdminHandoff(chatRoom);
        // 버튼 상태 업데이트 브로드캐스트 (상태 + 메시지 동시 전달)
        buttonUpdateService.sendButtonStateUpdate(
                roomCode,
                ChatRoomState.AI_ACTIVE,
                chatMessage
        );

        chatRoom.transitionToState(ChatRoomState.AI_ACTIVE);
        chatRoomRepository.save(chatRoom);

        log.debug("Success to cancel handoff process. roomCode={}, memberId={}", roomCode, chatRoom.getMemberId());
    }

    private void sendNotificationToAdmin(
            String roomCode, Long memberId, String memberName, ChatRoomStateInfo chatRoomState) {

        HandoffNotificationInfo notificationInfo = new HandoffNotificationInfo(
                roomCode,
                memberId,
                memberName,
                LocalDateTime.now()
        );

        WebSocketChatMessage message = new WebSocketChatMessage(
                BroadcastType.PLATFORM_HANDOFF_REQUEST,
                notificationInfo,
                chatRoomState
        );

        webSocketBroadcaster.broadcastNotifyAdminHandoff(message);
        log.info("Success to send notification to platform admin for request admin handoff. roomCode={}", roomCode);
    }

    @Override
    public void adminPreIntervention(WebSocketUserInfo userInfo, String roomCode, String sessionId) {
        if (!RoomCodeSupporter.isPlatformRoom(roomCode)) {
            throw new CustomException(CustomErrorCode.ONLY_PLATFORM_CHAT_ROOM);
        }
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        if (!chatRoom.getCurrentState().equals(ChatRoomState.AI_ACTIVE)) {
            throw new CustomException(CustomErrorCode.ONLY_AI_ACTIVE_STATE);
        }

        if (!userInfo.getRole().equals(Role.PLATFORM_ADMIN)) {
            throw new CustomWebSocketException(new CustomWebSocketError(sessionId,
                    CustomErrorCode.CHAT_ROOM_ACCESS_DENIED.getMessage()));
        }

        Long memberId = userInfo.getMemberId();
        log.info("Start to admin pre intervention. roomCode={}, memberId={}, currentState={}",
                    roomCode, memberId, chatRoom.getCurrentState());

        String adminCode = handoffAdmin(chatRoom);
        log.info("Success to pre intervention. roomCode={}, memberId={}, adminCode={}, newState={}",
                roomCode, memberId, adminCode, chatRoom.getCurrentState());
    }

    @Override
    public void acceptHandoff(WebSocketUserInfo userInfo, String roomCode, String sessionId) {
        if (!RoomCodeSupporter.isPlatformRoom(roomCode)) {
            throw new CustomException(CustomErrorCode.ONLY_PLATFORM_CHAT_ROOM);
        }
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        if (!chatRoom.getCurrentState().equals(ChatRoomState.WAITING_FOR_ADMIN)) {
            throw new CustomException(CustomErrorCode.ONLY_ADMIN_WAITING_STATE);
        }

        if (!Role.PLATFORM_ADMIN.equals(userInfo.getRole())) {
            throw new CustomWebSocketException(new CustomWebSocketError(sessionId,
                    CustomErrorCode.CHAT_ROOM_ACCESS_DENIED.getMessage()));
        }

        Long memberId = userInfo.getMemberId();
        log.info("Start to accept admin handoff. roomCode={}, memberId={}, currentState={}",
                roomCode, memberId, chatRoom.getCurrentState());

        String adminCode = handoffAdmin(chatRoom);

        // Save handoff acceptance system message to database for persistence
        ChatMessage chatMessage = chatMessageService.saveSystemChatMessage(roomCode, SystemMessage.SUCCESS_ADMIN_HANDOFF);
        ChatPayload payload = new ChatPayload(
                roomCode,
                chatMessage.getId(),
                chatMessage.getSeq(),
                chatMessage.getSenderId(),
                chatMessage.getSenderType(),
                chatMessage.getSenderName(),
                chatMessage.getContent(),
                chatMessage.getUnreadCount(),
                chatMessage.getSentAt()
        );

        payload.addAdminInfo(adminCode, chatRoom.getAdminDisplayName());

        ChatRoomStateInfo chatRoomStateInfo =
                ChatRoomStateSupporter.createRoomStateInfo(chatRoom, TransitionReason.HANDOFF_ACCEPTED);

        WebSocketChatMessage webSocketChatMessage = new WebSocketChatMessage(
                BroadcastType.SYSTEM_MESSAGE, payload, chatRoomStateInfo
        );

        webSocketBroadcaster.sendMessage(roomCode, webSocketChatMessage);
        buttonUpdateService.sendButtonStateUpdate(roomCode, ChatRoomState.ADMIN_ACTIVE);

        log.info("Success to accept admin handoff. roomCode={}, memberId={}, adminCode={}, newState={}",
                    roomCode, memberId, adminCode, chatRoom.getCurrentState());
    }

    private String handoffAdmin(ChatRoom chatRoom) {
        String roomCode = chatRoom.getRoomCode();
        if (!RoomCodeSupporter.isPlatformRoom(roomCode)) {
            throw new CustomException(CustomErrorCode.ONLY_PLATFORM_CHAT_ROOM);
        }
        String adminCode = Role.PLATFORM_ADMIN.name();

        aiChatService.handoffToAdmin(chatRoom, adminCode);
        chatRoom.transitionToState(ChatRoomState.ADMIN_ACTIVE);
        chatRoomRepository.save(chatRoom);

        return adminCode;
    }

    @Override
    public void aiHandoff(WebSocketUserInfo userInfo, String roomCode, String sessionId) {
        if (!RoomCodeSupporter.isPlatformRoom(roomCode)) {
            throw new CustomException(CustomErrorCode.ONLY_PLATFORM_CHAT_ROOM);
        }
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        // AI 서비스를 통한 AI 복귀 처리
        ChatMessageResponse message = aiChatService.manageAIHandoff(chatRoom);

        buttonUpdateService.sendButtonStateUpdate(
                roomCode,
                ChatRoomState.AI_ACTIVE,
                message
        );

        chatRoom.transitionToState(ChatRoomState.AI_ACTIVE);
        chatRoomRepository.save(chatRoom);
        log.info("Success to ai handoff. roomCode={}, newState={}", roomCode, chatRoom.getCurrentState());
    }

}
