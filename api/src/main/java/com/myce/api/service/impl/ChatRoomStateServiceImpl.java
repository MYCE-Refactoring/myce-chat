package com.myce.api.service.impl;

import com.myce.api.dto.AdminCodeInfo;
import com.myce.api.service.AIChatService;
import com.myce.api.service.client.ExpoClient;
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
import com.myce.domain.document.type.MessageSenderType;
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

    private final ExpoClient expoClient;
    private final AIChatService aiChatService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomRepository chatRoomRepository;
    private final ButtonUpdateService buttonUpdateService;
    private final ChatWebSocketBroadcaster webSocketBroadcaster;

    @Override
    @Transactional
    public void adminHandoff(WebSocketUserInfo userInfo, String roomCode, String sessionId) {
        log.debug("[ChatRoom-Handoff] Admin handoff. roomCode={}", roomCode);
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        // AI 서비스를 통한 핸드오프 요청 처리
        ChatMessageResponse chatMessageResponse = aiChatService.requestAdminHandoff(chatRoom);
        ChatRoomStateInfo chatRoomStateInfo =
                ChatRoomStateSupporter.createRoomStateInfo(chatRoom, TransitionReason.HANDOFF_REQUEST);

        sendHandoffMessage(chatMessageResponse, chatRoomStateInfo, BroadcastType.ADMIN_HANDOFF_REQUEST);

        //  플랫폼 채팅방인 경우 플랫폼 관리자에게도 알림
        if (RoomCodeSupporter.isPlatformRoom(roomCode)) {
            sendNotificationToAdmin(roomCode, chatRoom.getMemberId(), chatRoom.getMemberName(), chatRoomStateInfo);
        }

        // 버튼 상태 업데이트 브로드캐스트
        buttonUpdateService.sendButtonStateUpdate(roomCode, ChatRoomState.WAITING_FOR_ADMIN);
        log.debug("[ChatRoom-Handoff] Success to admin handoff process. roomCode={}, memberId={}", roomCode,
                chatRoom.getMemberId());
    }

    @Override
    public void cancelHandoff(WebSocketUserInfo userInfo, String roomCode, String sessionId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        // AI 서비스를 통한 핸드오프 취소 처리
        ChatMessageResponse chatMessageResponse = aiChatService.cancelAdminHandoff(chatRoom);
        ChatRoomStateInfo chatRoomStateInfo =
                ChatRoomStateSupporter.createRoomStateInfo(chatRoom, TransitionReason.HANDOFF_REQUEST);

        sendHandoffMessage(chatMessageResponse, chatRoomStateInfo, BroadcastType.AI_MESSAGE);

        // 버튼 상태 업데이트 브로드캐스트
        buttonUpdateService.sendButtonStateUpdate(roomCode, ChatRoomState.WAITING_FOR_ADMIN);
        log.debug("Success to cancel handoff process. roomCode={}, memberId={}", roomCode, chatRoom.getMemberId());
    }

    private void sendHandoffMessage(
            ChatMessageResponse handoffResponse, ChatRoomStateInfo chatRoomStateInfo, BroadcastType broadcastType) {
        String roomCode = handoffResponse.getRoomCode();

        // 핸드오프 요청 메시지 브로드캐스트
        ChatPayload payload = new ChatPayload(
                roomCode,
                handoffResponse.getMessageId(),
                handoffResponse.getSeq(),
                handoffResponse.getSenderId(),
                MessageSenderType.AI,
                MessageSenderType.AI.getDescription(),
                handoffResponse.getContent(),
                handoffResponse.getSentAt()
        );

        WebSocketChatMessage chatMessage =
                new WebSocketChatMessage(broadcastType, payload, chatRoomStateInfo);

        webSocketBroadcaster.sendMessage(roomCode, chatMessage);
        log.debug("Success to send handoff message. roomCode={}, transitionReason={}, payload={}",
                roomCode, chatRoomStateInfo.getTransitionReason().name(), payload);
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
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        if (!chatRoom.getCurrentState().equals(ChatRoomState.AI_ACTIVE)) {
            throw new CustomException(CustomErrorCode.ONLY_AI_ACTIVE_STATE);
        }

        Long memberId = userInfo.getMemberId();
        log.info("Start to admin pre intervention. roomCode={}, memberId={}, currentState={}",
                    roomCode, memberId, chatRoom.getCurrentState());

        String adminCode = handoffAdmin(chatRoom, memberId, userInfo.getLoginType());
        log.info("Success to pre intervention. roomCode={}, memberId={}, adminCode={}, newState={}",
                roomCode, memberId, adminCode, chatRoom.getCurrentState());
    }

    @Override
    public void acceptHandoff(WebSocketUserInfo userInfo, String roomCode, String sessionId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        if (!chatRoom.getCurrentState().equals(ChatRoomState.WAITING_FOR_ADMIN)) {
            throw new CustomException(CustomErrorCode.ONLY_ADMIN_WAITING_STATE);
        }

        Long memberId = userInfo.getMemberId();
        log.info("Start to accept admin handoff. roomCode={}, memberId={}, currentState={}",
                roomCode, memberId, chatRoom.getCurrentState());

        String adminCode = handoffAdmin(chatRoom, memberId, userInfo.getLoginType());

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

    private String handoffAdmin(ChatRoom chatRoom, Long memberId, LoginType loginType) {
        String roomCode = chatRoom.getRoomCode();
        String adminCode = Role.EXPO_SUPER_ADMIN.name();
        if (RoomCodeSupporter.isPlatformRoom(roomCode)) {
            adminCode = Role.PLATFORM_ADMIN.name();
        }
        else if (LoginType.ADMIN_CODE.equals(loginType)) {
            AdminCodeInfo adminCodeInfo = expoClient.getAdminCodeInto(memberId);
            String code = adminCodeInfo.getCode();
            log.debug("Success to determine admin code. id={}, code={}", memberId, code);
            adminCode = code;
        }

        aiChatService.handoffToAdmin(chatRoom, adminCode);

        return adminCode;
    }

    @Override
    public void aiHandoff(WebSocketUserInfo userInfo, String roomCode, String sessionId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        // AI 서비스를 통한 AI 복귀 처리
        ChatMessageResponse aiReturnResponse = aiChatService.manageAIHandoff(chatRoom);

        ChatPayload payload = new ChatPayload(
                roomCode,
                aiReturnResponse.getMessageId(),
                aiReturnResponse.getSeq(),
                aiReturnResponse.getSenderId(),
                MessageSenderType.AI,
                aiReturnResponse.getSenderName(),
                aiReturnResponse.getContent(),
                aiReturnResponse.getSentAt()
        );

        ChatRoomStateInfo chatRoomStateInfo =
                ChatRoomStateSupporter.createRoomStateInfo(chatRoom, TransitionReason.HANDOFF_AI_REQUEST);

        WebSocketChatMessage webSocketChatMessage = new WebSocketChatMessage(
                BroadcastType.AI_HANDOFF_REQUEST, payload, chatRoomStateInfo
        );

        webSocketBroadcaster.sendMessage(roomCode, webSocketChatMessage);
        buttonUpdateService.sendButtonStateUpdate(roomCode, ChatRoomState.AI_ACTIVE);
        log.info("Success to ai handoff. roomCode={}, newState={}", roomCode, chatRoom.getCurrentState());
    }

}
