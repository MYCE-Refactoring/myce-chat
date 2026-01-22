package com.myce.api.service.ai;

import com.myce.api.dto.ChatStatusResponse;
import com.myce.api.dto.ConversationSummaryResponse;
import com.myce.api.dto.message.ChatRoomStateInfo;
import com.myce.api.dto.message.type.MessageReaderType;
import com.myce.api.dto.message.type.SystemMessage;
import com.myce.api.dto.message.type.TransitionReason;
import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.api.mapper.ChatMessageMapper;
import com.myce.api.service.AIChatService;
import com.myce.api.service.ButtonUpdateService;
import com.myce.api.service.ChatMessageService;
import com.myce.api.service.ChatReadStatusService;
import com.myce.api.service.SendMessageService;
import com.myce.api.util.ChatRoomStateSupporter;
import com.myce.api.util.RoomCodeSupporter;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.document.type.ChatRoomState;
import com.myce.domain.repository.ChatRoomCacheRepository;
import com.myce.domain.repository.ChatRoomRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 채팅 서비스 구현체
 * AWS Bedrock Nova Lite 기반 플랫폼 상담 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatServiceImpl implements AIChatService {

    private final SendMessageService sendMessageService;
    private final ChatMessageService chatMessageService;
    private final ButtonUpdateService buttonUpdateService;
    private final ChatReadStatusService readStatusService;
    private final AIChatGenerateService chatGenerateService;
    private final ChatRoomCacheRepository chatCacheRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Override
    public ChatStatusResponse getAiChatStatus(String roomCode) {
        boolean isPlatformRoom = RoomCodeSupporter.isPlatformRoom(roomCode);
        //TODO 프론트에서 어떻게 쓰이는지 확인하고 enum으로 분리하기
        String roomType = isPlatformRoom ? "platform" : "expo";
        return new ChatStatusResponse(roomCode, isPlatformRoom, roomType);
    }

    @Override
    @Transactional
    public void handoffToAdmin(ChatRoom chatRoom, String adminCode) {
        String roomCode = chatRoom.getRoomCode();
        log.info("Starting handoff transaction. roomCode={}, adminCode={}, "
                        + "currentState : waiting={}, hasAdmin={}",
            roomCode, adminCode, chatRoom.isWaitingForAdmin(), chatRoom.hasAssignedAdmin());

        // STEP 1: IMMEDIATE STATE TRANSITION - Block AI responses first
        chatRoom.assignAdmin(adminCode);
        chatRoom.stopWaitingForAdmin();
        chatRoom.transitionToState(ChatRoomState.ADMIN_ACTIVE);

        chatCacheRepository.invalidateRoomCache(roomCode);

        log.info("Admin assigned and AI blocked - roomCode: {}, adminCode: {}, hasAdmin: {}, finalState: {}",
            roomCode, adminCode, chatRoom.hasAssignedAdmin(), chatRoom.getCurrentState());

        // STEP 2: GENERATE AI SUMMARY (for system message only)
        String conversationSummary = chatGenerateService.generateConversationSummary(roomCode);

        // STEP 2.5: SEND HANDOFF-TO-OPERATOR SYSTEM MESSAGE (persistent) - 타입 구분
        ChatMessage chatMessage = chatMessageService.saveSystemChatMessage(roomCode, conversationSummary);
        chatRoom.updateLastMessageInfo(chatMessage.getId(), chatMessage.getContent());

        // Broadcast system message (not regular chat message)
        ChatRoomStateInfo roomState = ChatRoomStateSupporter
                .createRoomStateInfo(chatRoom, TransitionReason.HANDOFF_TO_OPERATOR);

        sendMessageService.sendSystemMessage(chatRoom.getRoomCode(), roomState, chatMessage);

        // STEP 3: UPDATE BUTTON STATE TO ADMIN_ACTIVE
        buttonUpdateService.sendButtonStateUpdate(roomCode, ChatRoomState.ADMIN_ACTIVE);

        log.info("Complete handoff workflow finished. roomCode: {}, adminCode: {}, "
                        + "finalState: hasAdmin={}, currentState: {}",
            roomCode, adminCode, chatRoom.hasAssignedAdmin(), chatRoom.getCurrentState());
    }

    @Override
    public ConversationSummaryResponse getConversationSummaryForAdmin(String roomCode, String userRole) {
        if (!RoomCodeSupporter.isPlatformRoom(roomCode)) {
            throw new CustomException(CustomErrorCode.ONLY_PLATFORM_CHAT_ROOM);
        }

        if(!Role.PLATFORM_ADMIN.name().equalsIgnoreCase(userRole)) {
            throw new CustomException(CustomErrorCode.ONLY_PLATFORM_ADMIN);
        }

        String summaryMessage = chatGenerateService.generateConversationSummary(roomCode);
        return new ConversationSummaryResponse(
                roomCode, summaryMessage, LocalDateTime.now()
        );
    }

    @Override
    public ChatMessageResponse requestAdminHandoff(ChatRoom chatRoom) {
        String roomCode = chatRoom.getRoomCode();
        ChatMessage savedMessage = chatMessageService
                .saveAIChatMessage(roomCode, SystemMessage.AI_INVITE_MESSAGE);

        chatRoom.startWaitingForAdmin();
        chatCacheRepository.cacheChatRoom(roomCode, chatRoom);

        return ChatMessageMapper.toResponse(savedMessage);
    }

    @Override
    public ChatMessageResponse cancelAdminHandoff(ChatRoom chatRoom) {
        String roomCode = chatRoom.getRoomCode();
        chatRoom.stopWaitingForAdmin(); // waitingForAdmin = false
        chatRoom.transitionToState(ChatRoomState.AI_ACTIVE); // currentState = AI_ACTIVE
        chatCacheRepository.invalidateRoomCache(roomCode);

        ChatMessage savedMessage = chatMessageService.saveAIChatMessage(roomCode, SystemMessage.CANCEL_HANDOFF);

        log.info("Success to cancel admin handoff. roomCode={}", roomCode);
        return ChatMessageMapper.toResponse(savedMessage);
    }

    @Override
    @Transactional
    public ChatMessageResponse manageAIHandoff(ChatRoom chatRoom) {
        chatRoom.releaseAdmin(); // currentState = AI_ACTIVE로 변경됨
        chatRoom.stopWaitingForAdmin(); // waitingForAdmin = false로 변경됨

        String roomCode = chatRoom.getRoomCode();
        chatCacheRepository.invalidateRoomCache(roomCode);
        log.debug("Reset redis cache for ai handoff. roomCode={}", roomCode);

        ChatMessage chatMessage = chatMessageService
                .saveSystemChatMessage(roomCode, SystemMessage.AI_HANDOFF_MESSAGE);

        // Broadcast system message (not regular chat message)
        ChatRoomStateInfo roomState = ChatRoomStateSupporter
                .createRoomStateInfo(chatRoom, TransitionReason.HANDOFF_TO_AI);

        sendMessageService.sendSystemMessage(chatRoom.getRoomCode(), roomState, chatMessage);

        // AI 복귀 메시지 생성
        ChatMessage returnChatMessage = chatMessageService.saveAIChatMessage(roomCode, SystemMessage.AI_RETURN_MESSAGE);

        log.info("Success to ai handoff. roomCode={}, roomState={}", roomCode, chatRoom.getCurrentState());
        return ChatMessageMapper.toResponse(returnChatMessage);
    }

    /**
     * AI가 사용자 메시지를 읽었음을 알리는 읽음 상태 업데이트
     */
    public void sendAIReadStatusUpdate(ChatRoom chatRoom) {
        String roomCode = chatRoom.getRoomCode();

        // 가장 최근 메시지 ID 조회
        ChatMessage recentMessage = chatMessageService.getRecentMessage(roomCode);
        if (recentMessage == null) return;

        // readStatusJson에 AI 읽음 상태 업데이트
        String latestMessageId = recentMessage.getId();
        chatRoom.updateReadStatus(MessageReaderType.AI.name(),  latestMessageId);
        chatRoomRepository.save(chatRoom);

        log.debug("Update read state to AI. roomCode={}, messageId={}", roomCode, latestMessageId);
        readStatusService.updateChatReadStatus(roomCode, MessageReaderType.AI);
    }
}