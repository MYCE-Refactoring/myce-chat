package com.myce.domain.repository;

import com.myce.domain.document.ChatMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ChatMessageCacheRepository {

    List<ChatMessage> getCachedRecentMessages(String roomCode, int limit);

    void cacheRecentMessages(String roomCode, List<ChatMessage> messages);

    /**
     * 새 메시지 캐시에 추가
     * @param roomCode 채팅방 코드
     * @param message 추가할 메시지
     * @return 비동기 처리 결과
     */
    CompletableFuture<Void> addMessageToCache(String roomCode, ChatMessage message);

    /**
     * 미읽음 카운트 증가
     * @param roomCode 채팅방 코드
     * @param memberId 사용자 ID
     * @return 증가 후 카운트
     */
    Long incrementUnreadCount(String roomCode, Long memberId, long count);

    /**
     * 미읽음 카운트 리셋
     * @param roomCode 채팅방 코드
     * @param memberId 사용자 ID
     */
    void resetUnreadCount(String roomCode, Long memberId);

    /**
     * 미읽음 카운트 조회
     * @param roomCode 채팅방 코드
     * @param memberId 사용자 ID
     * @return 미읽음 카운트
     */
    Long getUnreadCount(String roomCode, Long memberId);

    void setLastReadMessageId(String roomCode, Long memberId, String messageId);

    String getLastReadMessageId(String roomCode, Long memberId);

    /**
     * 전체 배지 카운트 증가
     * @param memberId 사용자 ID
     * @return 증가 후 전체 카운트
     */
    Long incrementBadgeCount(Long memberId);

    /**
     * 전체 배지 카운트 재계산
     * @param memberId 사용자 ID
     * @return 재계산된 전체 카운트
     */
    Long recalculateBadgeCount(List<String> activeRooms, Long memberId);

    Long getBadgeCount(Long memberId);

}
