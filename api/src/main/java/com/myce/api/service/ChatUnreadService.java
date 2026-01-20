package com.myce.api.service;


import com.myce.common.type.Role;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.type.MessageSenderType;
import java.util.Map;

/**
 * 채팅 읽지 않은 메시지 계산 통합 서비스
 * 목적: 모든 unread count 계산을 하나의 서비스로 통합하여 일관성 보장
 * 방식: 카카오톡 방식 - "내가 보낸 메시지를 상대방이 읽었는가?"
 */
public interface ChatUnreadService {
    
    /**
     * 특정 채팅방에서 특정 사용자 관점의 읽지 않은 메시지 수 계산
     * 
     * @param roomCode 채팅방 코드 (예: "admin-9-15", "platform-123")
     * @param viewerId 조회하는 사용자 ID
     * @param viewerRole 조회하는 사용자 역할 ("USER", "PLATFORM_ADMIN", "EXPO_ADMIN")
     * @return 읽지 않은 메시지 수 (내가 읽어야 할 메시지 개수)
     */
    long getUnreadCountForViewer(String roomCode, Map<String, String> readStatus, Long viewerId, Role viewerRole);
    
    int isReadMessage(ChatMessage message, Map<String, String> readStatus);

}