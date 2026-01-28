package com.myce.api.service.impl;

import com.myce.api.dto.message.ChatPayload;
import com.myce.api.dto.message.ChatUnReadCountPayload;
import com.myce.api.dto.message.WebSocketBaseMessage;
import com.myce.api.dto.message.type.BroadcastType;
import com.myce.api.dto.message.type.MessageReaderType;
import com.myce.api.dto.message.type.SystemMessage;
import com.myce.api.dto.message.type.WebSocketDestination;
import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.api.exception.CustomWebSocketError;
import com.myce.api.exception.CustomWebSocketException;
import com.myce.api.mapper.ChatMessageMapper;
import com.myce.api.service.ChatMessageHandlerService;
import com.myce.api.service.ChatMessageService;
import com.myce.api.service.ChatReadStatusService;
import com.myce.api.service.ChatUnreadService;
import com.myce.api.service.ChatWebSocketBroadcaster;
import com.myce.api.service.ai.AIChatGenerateService;
import com.myce.api.util.RoomCodeSupporter;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.document.type.ChatRoomState;
import com.myce.domain.document.type.MessageSenderType;
import com.myce.domain.repository.ChatRoomCacheRepository;
import com.myce.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메시지 핸들링 구현
 * WebSocket 메시지 수신 후의 복잡한 비즈니스 로직 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageHandlerServiceImpl implements ChatMessageHandlerService {

    private final AIChatGenerateService chatGenerateService;
    private final ChatUnreadService chatUnreadService;
    private final ChatWebSocketBroadcaster broadcaster;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomCacheRepository chatCacheRepository;
    private final ChatMessageService chatMessageService;
    private final ChatReadStatusService readStatusService;
    private final SimpMessagingTemplate messagingTemplate;
    /**
     * 사용자 메시지 플로우 처리
     * 메시지 수신 후 자동 읽음, AI 응답, 미읽음 카운트 업데이트 등을 처리합니다.
     */
    @Override
    @Transactional
    public void handleUserMessageFlow(Long memberId, Role role, ChatRoom chatRoom, String content, String messageId) {
        String roomCode = chatRoom.getRoomCode();
        ChatRoomState currentState = chatRoom.getCurrentState();

        log.info("[ChatMessageHandler] Handle user message flow. memberId={}, role={}, roomCode={}, messageId={}",
                memberId, role, roomCode, messageId);
        if (RoomCodeSupporter.isPlatformRoom(roomCode) &&
                (currentState.equals(ChatRoomState.AI_ACTIVE) || currentState.equals(ChatRoomState.ADMIN_ACTIVE))) {
            // AI 응답 처리 (필요시)
            handleAIResponse(chatRoom, content);
        } else {
            // 자동 읽음 처리 (필요시)
            handleAutoReadLogic(memberId, role, messageId, chatRoom);

            // 미읽음 카운트 업데이트 (박람회 관리자 용)
            handleUnreadCountUpdate(roomCode);
        }

    }

    @Override
    @Transactional
    public void handleAutoReadLogic(Long memberId, Role role, String messageId, ChatRoom currentRoom) {
        String roomCode = currentRoom.getRoomCode();
        ChatRoomState state = currentRoom.getCurrentState();

        log.info( "[ChatMessageHandler] Start auto read logic. memberId={}, messageId={}, roomCode={}", memberId, memberId, roomCode );

        readStatusService.markAsReadForMember( roomCode, messageId, memberId, role );

        // 플랫폼 관리자가 메시지를 보낸 경우 → 해당 유저의 미읽음 메시지도 자동 읽음 처리
        if (Role.PLATFORM_ADMIN.equals( role ) && state.equals( ChatRoomState.ADMIN_ACTIVE )) {
            Long platformUserId = RoomCodeSupporter.extractMemberIdFromPlatformRoomCode( roomCode );

            // 유저의 미읽은 메시지들을 모두 읽음 처리
            readStatusService.markAsReadForMember( roomCode, messageId, platformUserId, Role.USER );
        }

        // 읽음 상태 변경을 WebSocket으로 브로드캐스트
        broadcaster.broadcastReadStatusUpdate(roomCode, messageId, memberId, MessageReaderType.USER);

        log.info("[ChatMessageHandler] Success to auto read logic. memberId={}, messageId={}, roomCode={}",
                memberId, memberId, roomCode);
    }

    /**
     * 미읽음 카운트 업데이트
     * 박람회 채팅에서 관리자의 미읽음 메시지 수를 업데이트하고 브로드캐스트합니다.
     */
    @Override
    public void handleUnreadCountUpdate(String roomCode) {
        try {
            Long expoId = RoomCodeSupporter.extractExpoIdFromAdminRoomCode(roomCode);
            if (expoId == null) return;

            ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode).orElse(null);
            if (chatRoom == null) return;

            // 관리자가 마지막으로 읽은 메시지 이후의 USER 메시지만 계산
            Long unreadCount = chatUnreadService
                    .getUnreadCountForViewer(roomCode, chatRoom.getReadStatus(), 0L, Role.EXPO_ADMIN);

            broadcaster.broadcastUnreadCountUpdate(expoId, roomCode, unreadCount);

            log.debug("Success to update unread count. roomCode: {}, expoId: {}, unreadCount: {}",
                roomCode, expoId, unreadCount);

        } catch (Exception unreadUpdateError) {
            log.warn("Fail to update unread count. roomCode: {}, error: {}",
                roomCode, unreadUpdateError.getMessage());
        }
    }

    @Override
    @Transactional
    public void handleAdminStateTransition(ChatRoom chatRoom, String adminCode, Long userId, String sessionId) {
        String roomCode = chatRoom.getRoomCode();
        ChatRoomState currentState = chatRoom.getCurrentState();

        log.debug("관리자 상태 전환 처리 시작 - roomCode: {}, currentState: {}, adminCode: {}",
            roomCode, currentState, adminCode);

        switch (currentState) {
            case WAITING_FOR_ADMIN -> {
                // AI 핸드오프 시스템으로 요약 및 상태 전환
                try {
                    // AI 서비스에 위임하여 관리자로 핸드오프
//                    aiChatService.handoffToAdmin(chatRoom, adminCode);
                    ChatRoom refreshedRoom = chatRoomRepository.findByRoomCode(roomCode)
                        .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));
                    if (refreshedRoom != null) {
                        log.info("AI 핸드오프 완료 - roomCode: {}, adminCode: {}, newState: {}",
                            roomCode, adminCode, refreshedRoom.getCurrentState());
                    }
                } catch (Exception handoffError) {
                    log.error("AI 핸드오프 실패 - roomCode: {}, adminCode: {}", roomCode, adminCode, handoffError);
                }
            }

            case AI_ACTIVE -> {
                // AI 활성 상태에서 직접 메시지 차단
                log.debug("AI_ACTIVE 상태에서 직접 관리자 메시지 차단 - roomCode: {}, adminCode: {}",
                    roomCode, adminCode);
                throw new CustomWebSocketException(
                        new CustomWebSocketError(sessionId, SystemMessage.USE_HAND_OFF_MESSAGE));
            }

            case ADMIN_ACTIVE -> {
                // 관리자 이미 활성 - 활동 시간만 업데이트
                chatRoom.updateAdminActivity();
                ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
                chatCacheRepository.cacheChatRoom(roomCode, savedRoom);
                log.debug("관리자 활동 시간 업데이트 - roomCode: {}, state: {}", roomCode, currentState);
            }

            default -> {
                log.debug("상태 전환 처리 안함 - roomCode: {}, state: {}", roomCode, currentState);
            }
        }
    }

    @Override
    public boolean validateAdminPermission(
            ChatRoom chatRoom, String adminCode, Long memberId, String sessionId) {
        if (!chatRoom.hasAssignedAdmin()) return true; // 아직 배정 안됨 - 허용

        if (!chatRoom.hasAdminPermission(adminCode)) {
            String errorMsg = String.format(SystemMessage.PERMISSION_DENIED_ADMIN_CHAT,
                    chatRoom.getAdminDisplayName());
            broadcaster.broadcastError(sessionId, memberId, errorMsg);

            return false;
        }

        return true;
    }

    /**
     * AI 응답 처리 (내부용)
     */
    private void handleAIResponse(ChatRoom chatRoom, String content) {
        ChatMessageResponse aiResponse = getAIMessage(chatRoom, content);

        String roomCode = chatRoom.getRoomCode();
        if (aiResponse != null) {
            ChatPayload payload = new ChatPayload(
                    chatRoom.getRoomCode(),
                    aiResponse.getMessageId(),
                    aiResponse.getSenderId(),
                    MessageSenderType.AI,
                    MessageSenderType.AI.getDescription(),
                    aiResponse.getContent(),
                    aiResponse.getSentAt()
            );
            WebSocketBaseMessage message = new WebSocketBaseMessage(BroadcastType.AI_MESSAGE, payload);
            broadcaster.sendMessage(roomCode, message);

            log.debug("AI 메시지 브로드캐스트 완료 - roomCode: {}", roomCode);
        }
    }

    private ChatMessageResponse getAIMessage(ChatRoom chatRoom, String content) {
        String roomCode = chatRoom.getRoomCode();
        ChatRoom currentRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        ChatRoomState currentState = currentRoom.getCurrentState();
        if (!(currentState.equals(ChatRoomState.AI_ACTIVE) ||
                currentState.equals(ChatRoomState.WAITING_FOR_ADMIN))) return null;

        log.debug("AI 응답 처리 시작 - roomId: {}, 현재상태: {}", roomCode, currentState);
        return sendAIMessage(chatRoom, content);
    }

    public ChatMessageResponse sendAIMessage(ChatRoom chatRoom, String userMessage) {
        String roomCode = chatRoom.getRoomCode();

        String aiResponse = chatGenerateService.generateAIResponse(userMessage, roomCode);
        ChatMessage chatMessage = chatMessageService.saveAIChatMessage(roomCode, aiResponse);
        chatRoom.updateLastMessageInfo(chatMessage.getId(), chatMessage.getContent());

        // AI가 사용자 메시지를 "읽음" 처리 - 읽음 상태 업데이트 브로드캐스트
        sendAIReadStatusUpdate(chatRoom);

        return ChatMessageMapper.toResponse(chatMessage);
    }

    public void sendAIReadStatusUpdate(ChatRoom chatRoom) {
        String roomCode = chatRoom.getRoomCode();

        // 가장 최근 메시지 ID 조회
        ChatMessage recentMessages = chatMessageService.getRecentMessage(roomCode);
        if (recentMessages == null) return;

        // readStatusJson에 AI 읽음 상태 업데이트
        String latestMessageId = recentMessages.getId();
        chatRoom.updateReadStatus(MessageReaderType.AI.name(),  latestMessageId);
        chatRoomRepository.save(chatRoom);

        log.debug("Update read state to AI. roomCode={}, messageId={}", roomCode, latestMessageId);
        ChatUnReadCountPayload readStatusDto = new ChatUnReadCountPayload(roomCode, MessageReaderType.AI, 0);
        WebSocketBaseMessage message = new WebSocketBaseMessage(BroadcastType.READ_STATUS_UPDATE, readStatusDto);
        String destination = WebSocketDestination.getSendChatMessageDestination(roomCode);
        messagingTemplate.convertAndSend(destination, message);
    }
}
