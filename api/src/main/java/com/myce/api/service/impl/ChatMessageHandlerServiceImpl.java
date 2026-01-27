package com.myce.api.service.impl;

import com.myce.api.dto.message.ChatPayload;
import com.myce.api.dto.message.WebSocketBaseMessage;
import com.myce.api.dto.message.type.BroadcastType;
import com.myce.api.dto.message.type.MessageReaderType;
import com.myce.api.dto.message.type.SystemMessage;
import com.myce.api.exception.CustomWebSocketError;
import com.myce.api.exception.CustomWebSocketException;
import com.myce.api.service.ChatMessageHandlerService;
import com.myce.api.service.ChatMessageService;
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
import com.myce.domain.repository.ChatMessageCacheRepository;
import com.myce.domain.repository.ChatMessageRepository;
import com.myce.domain.repository.ChatRoomCacheRepository;
import com.myce.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageCacheRepository chatMessageCacheRepository;
    /**
     * 사용자 메시지 플로우 처리
     * 메시지 수신 후 자동 읽음, AI 응답, 미읽음 카운트 업데이트 등을 처리합니다.
     */
    @Override
    public void handleUserMessageFlow(Long memberId, Role role, ChatRoom chatRoom, ChatMessage chatMessage) {
        String roomCode = chatRoom.getRoomCode();
        ChatRoomState currentState = chatRoom.getCurrentState();
        String messageId = chatMessage.getId();
        log.info("[ChatMessageHandler] Handle user message flow. memberId={}, role={}, roomCode={}, messageId={}",
                memberId, role, roomCode, messageId);

        if (chatMessage.getSenderType().equals(MessageSenderType.USER) && isNeedAiResponse(roomCode, currentState)) {
            handleAutoReadLogic(chatRoom, memberId, chatMessage); // 자동 읽음 처리
            handleAIResponse(chatRoom, chatMessage.getContent()); // AI 응답 처리
        } else {
            handleUnreadCountUpdate(roomCode); // 미읽음 카운트 업데이트
        }
    }

    public void handleAutoReadLogic(ChatRoom chatRoom, Long memberId, ChatMessage chatMessage) {
        String roomCode = chatRoom.getRoomCode();
        String messageId = chatMessage.getId();
        Long messageSeq = chatMessage.getSeq();
        log.info("[ChatMessageHandler] Start auto read logic. memberId={}, messageSeq={}, roomCode={}",
                memberId, messageSeq, roomCode);

        chatMessage.decreaseUnreadCount();
        MessageReaderType reader = MessageReaderType.AI;
        chatRoom.updateReadStatus(reader.name(), messageSeq);
        chatRoomRepository.save(chatRoom);
        chatMessageRepository.updateUnreadCountEqualSeq(roomCode, chatMessage.getId());
        broadcaster.broadcastReadStatusUpdate(roomCode, messageId, memberId, reader);

        log.info("[ChatMessageHandler] Success to auto read logic. memberId={}, messageSeq={}, roomCode={}",
                memberId, messageSeq, roomCode);
    }

    /**
     * 미읽음 카운트 업데이트
     * 박람회 채팅에서 관리자의 미읽음 메시지 수를 업데이트하고 브로드캐스트합니다.
     */
    @Override
    public void handleUnreadCountUpdate(String roomCode) {
        if (RoomCodeSupporter.isPlatformRoom(roomCode)) {

        } else {
            Long expoId = RoomCodeSupporter.extractExpoIdFromAdminRoomCode(roomCode);
            ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode).orElse(null);

            // 관리자가 마지막으로 읽은 메시지 이후의 USER 메시지만 계산
            Long unreadCount = chatUnreadService
                    .getUnreadCount(roomCode, chatRoom.getReadStatus(), 0L, Role.EXPO_ADMIN, null);
            broadcaster.broadcastUnreadCountUpdate(roomCode, MessageReaderType.ADMIN, unreadCount);
            log.debug("[ChatMessageHandler] Success to update unread count. roomCode: {}, expoId: {}, unreadCount: {}",
                    roomCode, expoId, unreadCount);
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
        String roomCode = chatRoom.getRoomCode();
        ChatRoomState currentState = chatRoom.getCurrentState();

        log.debug("[ChatMessageHandler] Handle ai response. roomCode={}, state={}", roomCode, currentState);
        ChatMessage chatMessage = getNewAiMessageForMember(roomCode, content);
        chatMessageCacheRepository.addMessageToCache(roomCode, chatMessage);

        String messageId = chatMessage.getId();
        Long messageSeq = chatMessage.getSeq();
        chatRoom.updateLastMessageInfo(messageId, chatMessage.getContent());

        // AI가 사용자 메시지를 "읽음" 처리
        chatRoom.updateReadStatus(MessageReaderType.AI.name(), messageSeq);
        chatRoomRepository.save(chatRoom);
        log.debug("[ChatMessageHandler] Update read state to AI. roomCode={}, messageSeq={}", roomCode, messageSeq);

        // 읽음 상태 업데이트 브로드캐스트
        sendAiMessage(roomCode, chatMessage);

        log.debug("[ChatMessageHandler] Success to send ai response. roomCode={}, messageId={}", roomCode, messageId);
    }

    private void sendAiMessage(String roomCode, ChatMessage chatMessage) {
        ChatPayload payload = new ChatPayload(
                roomCode,
                chatMessage.getId(),
                chatMessage.getSeq(),
                chatMessage.getSenderId(),
                MessageSenderType.AI,
                MessageSenderType.AI.getDescription(),
                chatMessage.getContent(),
                chatMessage.getSentAt()
        );
        WebSocketBaseMessage message = new WebSocketBaseMessage(BroadcastType.AI_MESSAGE, payload);
        broadcaster.sendMessage(roomCode, message);
    }

    private boolean isNeedAiResponse(String roomCode, ChatRoomState currentRoomStatus) {
        return RoomCodeSupporter.isPlatformRoom(roomCode) &&
                (currentRoomStatus.equals(ChatRoomState.AI_ACTIVE) || currentRoomStatus.equals(ChatRoomState.WAITING_FOR_ADMIN));
    }

    private ChatMessage getNewAiMessageForMember(String roomCode, String userMessage) {
        String aiResponse = chatGenerateService.generateAIResponse(userMessage, roomCode);
        return chatMessageService.saveAIChatMessage(roomCode, aiResponse);
    }

}
