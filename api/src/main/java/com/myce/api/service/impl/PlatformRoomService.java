package com.myce.api.service.impl;

import com.myce.domain.document.ChatRoom;
import com.myce.domain.document.type.ChatRoomState;
import com.myce.domain.repository.ChatRoomCacheRepository;
import com.myce.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomCacheRepository chatCacheRepository;

    public void createPlatformChatRoom(String roomCode, Long memberId, String memberName) {
        ChatRoom newRoom = ChatRoom.builder()
                .roomCode(roomCode)
                .memberId(memberId)
                .memberName(memberName)
                .build();

        // 플랫폼 채팅방은 기본적으로 AI_ACTIVE 상태로 시작
        // (생성자에서 자동으로 설정되지만 명시적으로 보장)
        newRoom.transitionToState(ChatRoomState.AI_ACTIVE);

        ChatRoom savedRoom = chatRoomRepository.save(newRoom);
        chatCacheRepository.cacheChatRoom(roomCode, savedRoom);

        log.info("Created Platform chat room. roomCode={}, memberId={}", roomCode, memberId);
    }

}
