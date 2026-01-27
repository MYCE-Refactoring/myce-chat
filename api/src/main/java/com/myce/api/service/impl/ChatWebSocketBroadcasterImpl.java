package com.myce.api.service.impl;

import com.myce.api.dto.message.AdminAssignmentPayload;
import com.myce.api.dto.message.ChatPayload;
import com.myce.api.dto.message.ChatReadStatusPayload;
import com.myce.api.dto.message.ChatRoomStateInfo;
import com.myce.api.dto.message.ChatUnReadCountPayload;
import com.myce.api.dto.message.WebSocketBaseMessage;
import com.myce.api.dto.message.WebSocketChatMessage;
import com.myce.api.dto.message.WebSocketErrorMessage;
import com.myce.api.dto.message.type.BroadcastType;
import com.myce.api.dto.message.type.MessageReaderType;
import com.myce.api.dto.message.type.TransitionReason;
import com.myce.api.dto.message.type.WebSocketDestination;
import com.myce.api.service.ChatWebSocketBroadcaster;
import com.myce.api.util.ChatRoomStateSupporter;
import com.myce.domain.document.ChatRoom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 메시지 브로드캐스트 구현
 * TODO 에러처리 상세화하기
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWebSocketBroadcasterImpl implements ChatWebSocketBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastUserMessage(String roomId, ChatPayload payload, ChatRoomStateInfo chatRoomStateInfo) {
        String messageId = payload.getMessageId();

        WebSocketChatMessage broadcastMessage = new WebSocketChatMessage(
                BroadcastType.MESSAGE,
                payload,
                chatRoomStateInfo
        );

        String destination = WebSocketDestination.getSendChatMessageDestination(roomId);
        try {
            messagingTemplate.convertAndSend(destination, broadcastMessage);
            log.debug("[WebSocketBroadcaster] Success to broadcast user message. roomId={}, messageId={}", roomId,
                    messageId);

        } catch (MessagingException e) {
            log.debug("[WebSocketBroadcaster] Fail to broadcast user message. roomId={}, messageId={}",
                    roomId, messageId, e);
        }
    }

    @Override
    public void broadcastAdminMessage(String roomCode, ChatPayload payload, ChatRoom chatRoom, String adminCode) {
        String messageId = payload.getMessageId();
        payload.addAdminInfo(adminCode, chatRoom.getAdminDisplayName());
        ChatRoomStateInfo chatRoomStateInfo = ChatRoomStateSupporter.createRoomStateInfo(
                chatRoom,
                TransitionReason.ADMIN_MESSAGE
        );

        WebSocketChatMessage broadcastMessage = new WebSocketChatMessage(
                BroadcastType.ADMIN_MESSAGE,
                payload,
                chatRoomStateInfo
        );

        try {
            sendMessage(roomCode, broadcastMessage);
            log.debug("[WebSocketBroadcaster] Success to broadcast admin message. roomId={}, messageId={}", roomCode,
                    messageId);
        } catch (MessagingException e) {
            log.error("[WebSocketBroadcaster] Fail to broadcast admin message. roomId={}, messageId={}, e",
                    roomCode, messageId);
        }
    }

    @Override
    public void broadcastReadStatusUpdate(String roomCode, String messageId, Long readBy, MessageReaderType readerType) {
        ChatReadStatusPayload payload = new ChatReadStatusPayload(messageId, readBy, readerType, LocalDateTime.now());
        WebSocketBaseMessage message = new WebSocketBaseMessage(BroadcastType.READ_STATUS_UPDATE, payload);

        try {
            sendMessage(roomCode, message);
            log.debug("[WebSocketBroadcaster] Success to broadcast read status. roomCode={}, messageId={}, readBy={}",
                    roomCode, messageId, readBy);
        } catch (Exception e) {
            log.debug("[WebSocketBroadcaster] Fail to broadcast read status. roomCode={}, messageId={}, readBy={}",
                    roomCode, messageId, readBy);
        }
    }

    @Override
    public void broadcastUnreadCountUpdate(String roomCode, MessageReaderType readerType, Long unreadCount) {
        ChatUnReadCountPayload payload = new ChatUnReadCountPayload(roomCode, readerType, unreadCount);

        WebSocketBaseMessage message = new WebSocketBaseMessage(BroadcastType.UNREAD_COUNT_UPDATE, payload);

        try {
            messagingTemplate.convertAndSend(WebSocketDestination.CHAT_ROOM_STATE, message);
            log.debug("[WebSocketBroadcaster] Success to broadcast unread count. roomCode={}, unreadCount={}",
                    roomCode, unreadCount);
        } catch (Exception e) {
            log.debug("[WebSocketBroadcaster] Fail to broadcast unread count. roomCode={}, unreadCount={}",
                    roomCode, unreadCount);
        }
    }

    @Override
    public void broadcastAdminAssignment(String roomCode, Long expoId,
            String currentAdminCode, String adminDisplayName) {
        AdminAssignmentPayload payload = new AdminAssignmentPayload(roomCode, currentAdminCode, adminDisplayName);
        WebSocketBaseMessage message = new WebSocketBaseMessage(BroadcastType.ADMIN_ASSIGNMENT_UPDATE, payload);

        String destination = WebSocketDestination.getSendChatMessageDestination(roomCode);

        try {
            messagingTemplate.convertAndSend(destination, message);

            if (expoId != null) {
                String adminUpdateDestination = WebSocketDestination.getAdminUpdateStateDestination(expoId);
                messagingTemplate.convertAndSend(adminUpdateDestination, message);
            }

            log.debug("[WebSocketBroadcaster] Success to broadcast admin assignment. expoId={}, roomCode={}, "
                    + "currentAdminCode={}", expoId, roomCode, currentAdminCode);
        } catch (Exception e) {
            log.debug("[WebSocketBroadcaster] Fail to broadcast admin assignment. expoId={}, roomCode={}, "
                    + "currentAdminCode={}", expoId, roomCode, currentAdminCode);
        }
    }

    @Override
    public void broadcastError(String sessionId, Long memberId, String errorMessage) {
        WebSocketErrorMessage message = new WebSocketErrorMessage(BroadcastType.ERROR, errorMessage);

        try {
            messagingTemplate.convertAndSendToUser(sessionId, WebSocketDestination.ERROR, message);
            log.debug("[WebSocketBroadcaster] Success to send error message. sessionId={}, memberId={}", sessionId, memberId);
        } catch (MessagingException e) {
            log.debug("F[WebSocketBroadcaster] ail to send error message. sessionId={}, memberId={}", sessionId, memberId, e);
        }
    }

    public void broadcastNotifyAdminHandoff(WebSocketBaseMessage broadcastMessage) {
        String destination = WebSocketDestination.ADMIN_HANDOFF_NOTIFICATION;
        messagingTemplate.convertAndSend(destination, broadcastMessage);
    }

    public void sendMessage(String roomId, WebSocketBaseMessage broadcastMessage) {
        String destination = WebSocketDestination.getSendChatMessageDestination(roomId);
        messagingTemplate.convertAndSend(destination, broadcastMessage);
    }
}
