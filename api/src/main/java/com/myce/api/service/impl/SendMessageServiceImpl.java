package com.myce.api.service.impl;


import com.myce.api.dto.AdminCodeInfo;
import com.myce.api.dto.message.ChatPayload;
import com.myce.api.dto.message.ChatRoomStateInfo;
import com.myce.api.dto.message.WebSocketChatMessage;
import com.myce.api.dto.message.type.BroadcastType;
import com.myce.api.dto.message.type.TransitionReason;
import com.myce.api.dto.message.type.WebSocketMessagePayload;
import com.myce.api.dto.request.SendMessageRequest;
import com.myce.api.exception.CustomWebSocketError;
import com.myce.api.exception.CustomWebSocketException;
import com.myce.api.service.ChatMessageHandlerService;
import com.myce.api.service.ChatWebSocketBroadcaster;
import com.myce.api.service.SendMessageService;
import com.myce.api.service.client.ExpoClient;
import com.myce.api.service.component.ChatAdminAssignmentComponent;
import com.myce.api.service.component.ChatMessageSaveComponent;
import com.myce.api.util.ChatRoomStateSupporter;
import com.myce.api.util.RoomCodeSupporter;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.repository.ChatRoomRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class SendMessageServiceImpl implements SendMessageService {

    private final ExpoClient expoClient;
    private final ChatWebSocketBroadcaster broadcaster;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageHandlerService messageHandler;
    private final ChatMessageSaveComponent messageSaveComponent;
    private final ChatAdminAssignmentComponent adminAssignmentComponent;

    @Override
    public void sendChatMessage(Long memberId, Role role, LoginType loginType, String sessionId, SendMessageRequest request) {
        String roomCode = request.getRoomId();
        String content = request.getContent();

        log.debug("[SendMessageService] Send chat message.memberId={}, roomCode={}", memberId, roomCode);
        try {
            broadcastMessage(memberId, role, loginType, roomCode, content);
        } catch (Exception e) {
            log.error("[SendMessageService] Failed to send message. memberId={}, roomCode={}", memberId, roomCode, e);
            throw new CustomWebSocketException(new CustomWebSocketError(sessionId,
                    WebSocketMessagePayload.SEND_MESSAGE_FAIL.getMessage()));
        }
    }

    @Override
    public void sendAdminChatMessage(Long memberId, Role role, LoginType loginType, String sessionId, SendMessageRequest request) {
        String roomId = request.getRoomId();
        String content = request.getContent();

        try {
            broadcastAdminMessage(memberId, role, loginType, roomId, sessionId, content);
            log.debug("[SendMessageService] Success to send admin message. memberId={}, roomId={}", memberId, roomId);
        } catch (Exception e) {
            log.debug("[SendMessageService] Failed to send admin message. memberId={}, roomId={}", memberId, roomId, e);
            broadcaster.broadcastError(sessionId, memberId,
                    WebSocketMessagePayload.SEND_MESSAGE_FAIL.getMessage() + e.getMessage());
        }
    }

    public void sendSystemMessage(String roomCode, ChatRoomStateInfo roomState, ChatMessage chatMessage) {
        try {
            broadcastSystemMessage(roomCode, roomState, chatMessage);
            log.debug("[SendMessageService] Success to send system message. roomCode={}, roomState={}",
                    roomCode, roomState);
        } catch (Exception e) {
            log.debug("[SendMessageService] Failed to send system message. roomCode={}", roomCode, e);
        }
    }

    public void broadcastMessage(Long memberId, Role role, LoginType loginType, String roomCode,  String content) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        // 1. 메시지 저장
        ChatMessage chatMessage = messageSaveComponent.saveMessage(memberId, role, loginType, chatRoom, content);

        // 2. 사용자 메시지 브로드캐스트
        ChatRoomStateInfo chatRoomStateInfo = ChatRoomStateSupporter
                .createRoomStateInfo(chatRoom, TransitionReason.USER_MESSAGE);
        ChatPayload payload = getPayload(roomCode, chatMessage);
        broadcaster.broadcastUserMessage(roomCode, payload, chatRoomStateInfo);

        // 3. 사용자 메시지 플로우 처리 (AI 응답, 자동 읽음, 미읽음 업데이트)
        messageHandler.handleUserMessageFlow(
                memberId,
                role,
                chatRoom, content,
                chatMessage.getId());
    }

    private void broadcastAdminMessage(Long memberId, Role role, LoginType loginType, String roomCode, String sessionId, String content) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        // 1. 관리자 코드 결정
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

        // 2. 박람회 방에 대한 담당자 배정
        if (!RoomCodeSupporter.isPlatformRoom(roomCode)) {
            adminAssignmentComponent.assignAdminIfNeeded(chatRoom, adminCode);
        }

        // 3. 권한 검증
        if (!messageHandler.validateAdminPermission(chatRoom, adminCode, memberId, sessionId)) {
            return; // 권한 없음 - 이미 에러 메시지 전송됨
        }

        // 4. 상태별 처리 (상태 전환, 에러 처리 등)
        if (!RoomCodeSupporter.isPlatformRoom(roomCode)) {
            messageHandler.handleAdminStateTransition(chatRoom, adminCode, memberId, sessionId);
        }

        // 새로운 chatRoom 데이터 재조회 (상태 변경 가능성)
        chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalStateException("채팅방을 찾을 수 없습니다"));

        // 5. 담당자 배정 브로드캐스트
        if (chatRoom.hasAssignedAdmin()) {
            broadcaster.broadcastAdminAssignment(roomCode, chatRoom.getExpoId(),
                    chatRoom.getCurrentAdminCode(), chatRoom.getAdminDisplayName());
        }

        // 6. 메시지 저장
        ChatMessage chatMessage = messageSaveComponent.saveMessage(memberId, role, loginType, chatRoom, content);
        ChatPayload payload = getPayload(roomCode, chatMessage);

        // 7. 관리자 메시지 브로드캐스트
        broadcaster.broadcastAdminMessage(roomCode, payload, chatRoom, adminCode);
    }

    private void broadcastSystemMessage(String roomCode, ChatRoomStateInfo roomState, ChatMessage chatMessage) {
        ChatPayload payload = getPayload(roomCode, chatMessage);

        WebSocketChatMessage message = new WebSocketChatMessage(
                BroadcastType.SYSTEM_MESSAGE,
                payload,
                roomState
        );

        broadcaster.sendMessage(roomCode, message);
    }

    private ChatPayload getPayload(String roomCode, ChatMessage chatMessage) {
        return new ChatPayload(
                roomCode,
                chatMessage.getId(),
                chatMessage.getSenderId(),
                chatMessage.getSenderType(),
                chatMessage.getSenderName(),
                chatMessage.getContent(),
                chatMessage.getSentAt()
        );
    }
}
