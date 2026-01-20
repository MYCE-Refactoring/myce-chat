package com.myce.api.service;


import com.myce.api.dto.response.ChatRoomInfoListResponse;

/**
 * 채팅방 비즈니스 로직 서비스
 */
public interface ChatRoomService {


    /**
     * 사용자별 채팅방 목록 조회 (USER: 본인 참여, ADMIN: 관리 박람회 전체)
     */
    ChatRoomInfoListResponse getChatRooms(Long memberId, String memberName, String role);

    /**
     * 플랫폼 관리자 채팅방 목록 조회
     */
    ChatRoomInfoListResponse getPlatformChatRooms(Long memberId, String memberName, String role);

    /**
     * 박람회별 채팅방 목록 조회 (관리자 전용, 권한 검증)
     */
    ChatRoomInfoListResponse getChatRoomsByExpo(Long expoId, Long adminId);
    
}