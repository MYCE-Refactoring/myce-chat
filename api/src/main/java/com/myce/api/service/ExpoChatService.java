package com.myce.api.service;

import com.myce.api.auth.dto.CustomUserDetails;
import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.api.dto.response.ChatRoomInfoListResponse;
import com.myce.api.dto.response.ChatRoomInfoResponse;
import com.myce.api.dto.response.ChatUnreadCountResponse;
import com.myce.common.dto.PageResponse;
import com.myce.common.type.LoginType;
import java.awt.print.Pageable;
import java.util.Map;

/**
 * 박람회 관리자 채팅 서비스 인터페이스
 */
public interface ExpoChatService {

    /**
     * 관리자용 채팅방 목록 조회
     */
    ChatRoomInfoListResponse getChatRoomsForAdmin(Long expoId, Long memberId, LoginType loginType);

    /**
     * 채팅방 메시지 조회 (페이징)
     */
    PageResponse<ChatMessageResponse> getMessages(Long expoId, String roomCode, int page, int size, Long memberId);

    /**
     * 안읽은 메시지 수 조회
     */
    Long getUnreadCount(Long expoId, String roomCode, Long memberId);
    
    /**
     * 사용자용 전체 읽지 않은 메시지 수 조회 (FAB용)
     */
    ChatUnreadCountResponse getAllUnreadCountsForUser(Long userId);
    
    /**
     * 박람회 채팅방 생성 또는 조회
     */
    ChatRoomInfoResponse getOrCreateExpoChatRoom(Long expoId, Long memberId);
}