package com.myce.domain.repository;

import com.myce.domain.document.ChatRoom;
import java.util.List;

public interface ChatRoomCacheRepository {

    /**
     * 채팅방 캐시 무효화
     * @param roomCode 채팅방 코드
     */
    void invalidateRoomCache(String roomCode);

    /**
     * 사용자별 활성 채팅방 목록 조회
     * @param userId 사용자 ID
     * @return 활성 채팅방 코드 목록
     */
    List<String> getUserActiveRooms(Long userId);

    /**
     * 사용자 활성 채팅방 추가
     * @param userId 사용자 ID
     * @param roomCode 채팅방 코드
     */
    void addUserActiveRoom(Long userId, String roomCode);

    /**
     * ChatRoom 캐시 조회
     * @param roomCode 채팅방 코드
     * @return 캐시된 ChatRoom (캐시 미스 시 null)
     */
    ChatRoom getCachedChatRoom(String roomCode);

    /**
     * ChatRoom 캐싱
     * @param roomCode 채팅방 코드
     * @param chatRoom 캐싱할 ChatRoom
     */
    void cacheChatRoom(String roomCode, ChatRoom chatRoom);

}
