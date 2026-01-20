package com.myce.api.service.impl;

import com.myce.api.controller.supporter.ChatRoomResponseMakeService;
import com.myce.api.dto.response.ChatRoomInfoListResponse;
import com.myce.api.service.ChatRoomService;
import com.myce.api.util.RoomCodeSupporter;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.repository.ChatRoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomResponseMakeService responseMakeService;
    private final PlatformRoomService platformRoomService;

    private final ChatRoomRepository chatRoomRepository;

    /**
     * 현재 로그인한 사용자의 채팅방 목록 조회
     */
    @Override
    public ChatRoomInfoListResponse getChatRooms(Long memberId, String memberName, String role) {
        log.debug("[ChatRoomService] Get chat rooms for member. memberId={}, role={}", memberId, role);
        Role memberRole = Role.fromName(role);
        String roomCode = RoomCodeSupporter.getPlatformRoomCode(memberId);
        boolean roomExists = chatRoomRepository.findByRoomCode(roomCode).isPresent();

        if (!roomExists) platformRoomService.createPlatformChatRoom(roomCode, memberId, memberName);

        List<ChatRoom> chatRooms;
        if (Role.PLATFORM_ADMIN.equals(memberRole)) {
            // 플랫폼 관리자: 모든 플랫폼 채팅방 조회
            chatRooms = chatRoomRepository.findByExpoIdIsNullAndIsActiveTrueOrderByLastMessageAtDesc();
        } else {
            // 일반 사용자, EXPO_ADMIN: 본인이 참여한 채팅방만 조회
            chatRooms = chatRoomRepository.findByMemberIdAndIsActiveTrueOrderByLastMessageAtDesc(memberId);
        }

        log.debug("[ChatRoomService] Get chat rooms for member. memberId={}, role={}, count={}",
                memberId, role, chatRooms.size());
        return responseMakeService.convertToResponse(chatRooms, memberId, memberRole);
    }

    /**
     * 현재 로그인한 사용자의 채팅방 목록 조회
     */
    @Override
    public ChatRoomInfoListResponse getPlatformChatRooms(Long memberId, String memberName, String role) {
        Role memberRole = Role.fromName(role);
        if (!Role.PLATFORM_ADMIN.equals(memberRole)) {
            throw new CustomException(CustomErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        List<ChatRoom> chatRooms = chatRoomRepository.findByExpoIdIsNullAndIsActiveTrueOrderByLastMessageAtDesc();
        return responseMakeService.convertToResponse(chatRooms, memberId, memberRole);
    }

    /**
     * 특정 박람회의 채팅방 목록 조회 (관리자 전용)
     */
    @Override
    public ChatRoomInfoListResponse getChatRoomsByExpo(Long expoId, Long adminId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByExpoIdAndIsActiveTrueOrderByLastMessageAtDesc(expoId);
        return responseMakeService.convertToResponse(chatRooms, adminId, Role.EXPO_ADMIN);
    }
}
