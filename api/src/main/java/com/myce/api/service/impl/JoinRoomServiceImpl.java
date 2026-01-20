package com.myce.api.service.impl;

import com.myce.api.dto.MemberInfo;
import com.myce.api.dto.WebSocketUserInfo;
import com.myce.api.dto.message.type.WebSocketMessagePayload;
import com.myce.api.exception.CustomWebSocketException;
import com.myce.api.exception.CustomWebSocketError;
import com.myce.api.service.JoinRoomService;
import com.myce.api.service.client.MemberClient;
import com.myce.api.util.RoomCodeSupporter;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.repository.ChatRoomRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JoinRoomServiceImpl implements JoinRoomService {

    private final MemberClient memberClient;

    private final PlatformRoomService platformRoomService;
    private final ChatRoomAccessCheckService chatRoomAccessCheckService;

    private final ChatRoomRepository chatRoomRepository;

    @Override
    @Transactional
    public void joinRoom(WebSocketUserInfo userInfo, String roomCode, String sessionId) {
        Long memberId = userInfo.getMemberId();
        try {
            if (RoomCodeSupporter.isPlatformRoom(roomCode)) {
                joinPlatformChatRoom(roomCode, memberId, userInfo.getRole());
            } else {
                joinMemberChatRoom(roomCode, memberId, userInfo.getLoginType(), userInfo.getRole());
            }

        } catch (CustomException e) {
            String errorMessage = WebSocketMessagePayload.JOIN_MESSAGE_FAIL.getMessage() + "\n" + e.getMessage();
            log.error("Failed to join chat room. roomCode={}, memberId={}, sessionId={}", roomCode, memberId, sessionId);
            throw new CustomWebSocketException(new CustomWebSocketError(sessionId, errorMessage));
        }
    }

    private void joinPlatformChatRoom(String roomCode, Long memberId, Role role) {
        Long roomCodeMemberId = RoomCodeSupporter.extractMemberIdFromPlatformRoomCode(roomCode);

        // 권한 확인: 본인의 플랫폼 방이거나 플랫폼 관리자
        if (!roomCodeMemberId.equals(memberId) && !Role.PLATFORM_ADMIN.equals(role)) {
            throw new CustomException(CustomErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        if (chatRoomRepository.findByRoomCode(roomCode).isEmpty()) {
            MemberInfo memberInfo = memberClient.getMemberInfo(memberId);
            String memberName = memberInfo != null ? memberInfo.getName() : "플랫폼 사용자";

            platformRoomService.createPlatformChatRoom(roomCode, memberId, memberName);
        }
    }

    private void joinMemberChatRoom(String roomId, Long memberId, LoginType loginType, Role role) {
        Long expoId = RoomCodeSupporter.extractExpoIdFromRoomCode(roomId);
        Long participantId = RoomCodeSupporter.extractMemberIdFromRoomCode(roomId);

        if (!chatRoomAccessCheckService.isValidAccess(loginType, expoId, memberId, participantId, role)) {
            throw new CustomException(CustomErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        ensureChatRoomExists(roomId, expoId, participantId);
    }

    /**
     * 채팅방 존재 확인 및 생성
     */
    private void ensureChatRoomExists(String roomId, Long expoId, Long participantId) {
        Optional<ChatRoom> existingRoom = chatRoomRepository.findByRoomCode(roomId);

        if (existingRoom.isEmpty()) {
            ChatRoom newRoom = ChatRoom.builder()
                    .roomCode(roomId)
                    .expoId(expoId)
                    .memberId(participantId)
                    .build();

            chatRoomRepository.save(newRoom);
        }
    }
}
