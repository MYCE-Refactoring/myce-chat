package com.myce.api.service;


import com.myce.api.dto.message.ChatPayload;
import com.myce.api.dto.message.ChatRoomStateInfo;
import com.myce.api.dto.message.WebSocketBaseMessage;
import com.myce.api.dto.message.type.MessageReaderType;
import com.myce.domain.document.ChatRoom;

/**
 * WebSocket 메시지 브로드캐스트 서비스
 *
 * 채팅 메시지, 상태 업데이트, 에러 등을 WebSocket으로 브로드캐스트하는 기능 담당
 */
public interface ChatWebSocketBroadcaster {

    /**
     * 사용자 메시지 브로드캐스트
     */
    void broadcastUserMessage(String roomId, ChatPayload payload, ChatRoomStateInfo chatRoomStateInfo);

    /**
     * 관리자 메시지 브로드캐스트
     */
    void broadcastAdminMessage(String roomCode, ChatPayload payload, ChatRoom chatRoom, String adminCode);

    /**
     * 읽음 상태 업데이트 브로드캐스트
     */
    void broadcastReadStatusUpdate(String roomId, String messageId, Long readBy, MessageReaderType readerType);

    /**
     * 미읽음 카운트 업데이트 브로드캐스트 (박람회 관리자용)
     */
    void broadcastUnreadCountUpdate(Long expoId, String roomCode, Long unreadCount);

    /**
     * 담당자 배정 브로드캐스트
     */
    void broadcastAdminAssignment(String roomCode, Long expoId, String currentAdminCode, String adminDisplayName);

    /**
     * 에러 메시지 브로드캐스트
     */
    void broadcastError(String sessionId, Long memberId, String errorMessage);

    void broadcastNotifyAdminHandoff(WebSocketBaseMessage broadcastMessage);

    void sendMessage(String roomId, WebSocketBaseMessage broadcastMessage);
}
