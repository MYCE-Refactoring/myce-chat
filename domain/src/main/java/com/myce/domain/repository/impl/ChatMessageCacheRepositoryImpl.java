package com.myce.domain.repository.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.repository.ChatMessageCacheRepository;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatMessageCacheRepositoryImpl implements ChatMessageCacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // 캐시 설정 상수
    private static final int MAX_CACHED_MESSAGES = 50;
    private static final Duration CACHE_TTL = Duration.ofDays(7);
    private static final Duration BADGE_TTL = Duration.ofDays(7);

    @PostConstruct
    public void init() {
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    }

    /**
     * 최근 메시지 캐시 조회
     * 캐시 히트 시 5-10ms, 미스 시 null 반환
     */
    @Override
    public List<ChatMessage> getCachedRecentMessages(String roomCode, int limit) {
        log.trace("[ChatMessageCache] Get cache recent message. roomCode={}, count={}", roomCode, limit);
        String key = String.format(ChatRoomKey.ROOM_RECENT_KEY_FORMAT, roomCode);

        try {
            List<Object> cachedObjects = redisTemplate.opsForList().range(key, 0, limit - 1);

            if (cachedObjects == null || cachedObjects.isEmpty()) {
                log.debug("[ChatMessageCache] Cache miss for recent message. roomCode={}", roomCode);
                return null;
            }

            // Object를 ChatMessage로 변환
            List<ChatMessage> messages = cachedObjects.stream()
                    .map(obj -> objectMapper.convertValue(obj, ChatMessage.class))
                    .collect(Collectors.toList());

            log.debug("[ChatMessageCache] Cache hit for recent message. roomCode={}, count={}",
                    roomCode, messages.size());
            return messages;

        } catch (Exception e) {
            log.error("[ChatMessageCache] Fail to get cached recent message. roomCode={}", roomCode, e);
            return null;
        }
    }

    /**
     * 최근 메시지 캐싱
     * MongoDB 조회 후 Redis에 저장
     */
    @Override
    public void cacheRecentMessages(String roomCode, List<ChatMessage> messages) {
        log.trace("[ChatMessageCache] Caching recent message. roomCode={}, size={}", roomCode, messages.size());
        String key = String.format(ChatRoomKey.ROOM_RECENT_KEY_FORMAT, roomCode);

        try {
            // 기존 캐시 삭제
            redisTemplate.delete(key);

            // 새 메시지 캐싱 (최신 메시지가 앞에 오도록)
            List<Object> messagesToCache = messages.stream()
                    .limit(MAX_CACHED_MESSAGES)
                    .collect(Collectors.toList());

            if (!messagesToCache.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(key, messagesToCache.toArray());
                redisTemplate.expire(key, CACHE_TTL);
                log.debug("[ChatMessageCache] Success to cache recent message. roomCode={}, size={}",
                        roomCode, messagesToCache.size());
            }
        } catch (Exception e) {
            log.debug("[ChatMessageCache] Fail to cache recent message. roomCode={}, size={}",
                    roomCode, messages.size());
        }
    }

    /**
     * 새 메시지를 캐시에 추가 (동기)
     * WebSocket 메시지 전송 시 즉시 캐싱하여 실시간 반영
     */
    @Override
    public CompletableFuture<Void> addMessageToCache(String roomCode, ChatMessage message) {
        String messageId = message.getId();
        log.trace("[ChatMessageCache] Caching new message. roomCode={}, messageId={}", roomCode, messageId);

        String key = String.format(ChatRoomKey.ROOM_RECENT_KEY_FORMAT, roomCode);
        try {
            // 새 메시지를 리스트 앞에 추가 (최신 메시지가 앞에)
            redisTemplate.opsForList().leftPush(key, message);

            // 리스트 크기 제한
            redisTemplate.opsForList().trim(key, 0, MAX_CACHED_MESSAGES - 1);

            // TTL 갱신
            redisTemplate.expire(key, CACHE_TTL);

            log.debug("[ChatMessageCache] Success to cache new message. roomCode={}, messageId={}", roomCode, messageId);
        } catch (Exception e) {
            log.debug("[ChatMessageCache] Fail to cache new message. roomCode={}, messageId={}", roomCode, messageId);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 미읽음 카운트 증가
     * 메시지 전송 시 수신자의 미읽음 카운트 증가
     */
    @Override
    public Long incrementUnreadCount(String roomCode, Long memberId, long cnt) {
        log.trace("[ChatMessageCache] Increment unread count. roomCode={}, count={}", roomCode, cnt);

        String unreadKey = String.format(ChatRoomKey.ROOM_UNREAD_KEY_FORMAT, roomCode, memberId);
        try {
            Long count = redisTemplate.opsForValue().increment(unreadKey, cnt);
            redisTemplate.expire(unreadKey, CACHE_TTL);
            log.debug("[ChatMessageCache] Success increment unread count. roomCode={}, count={}", roomCode, cnt);
            return count;

        } catch (Exception e) {
            log.warn("[ChatMessageCache] Fail increment unread count. roomCode={}, count={}", roomCode, cnt, e);
            return 0L;
        }
    }

    /**
     * 미읽음 카운트 리셋
     * 채팅방 읽음 처리 시 호출
     */
    @Override
    public void resetUnreadCount(String roomCode, Long memberId) {
        log.trace("[ChatMessageCache] Reset unread count. roomCode={}, memberId={}", roomCode, memberId);

        try {
            String unreadKey = String.format(ChatRoomKey.ROOM_UNREAD_KEY_FORMAT, roomCode, memberId);
            redisTemplate.opsForValue().set(unreadKey, 0L, CACHE_TTL);

            log.debug("[ChatMessageCache] Success to reset unread count. roomCode={}, memberId={}",
                    roomCode, memberId);


        } catch (Exception e) {
            log.error("[ChatMessageCache] Fail to reset unread count. roomCode={}, memberId={}",
                    roomCode, memberId, e);
        }
    }

    @Override
    public Long getUnreadCount(String roomCode, Long memberId) {
        log.trace("[ChatMessageCache] Get unread count. roomCode={}, memberId={}", roomCode, memberId);

        String unreadKey = String.format(ChatRoomKey.ROOM_UNREAD_KEY_FORMAT, roomCode, memberId);
        try {
            Object value = redisTemplate.opsForValue().get(unreadKey);

            log.debug("[ChatMessageCache] Success to get unread count. roomCode={}, memberId={}, count={}",
                    roomCode, memberId, value);
            return value == null ? 0L : Long.parseLong(value.toString());
        } catch (Exception e) {
            log.warn("[ChatMessageCache] Fail to get unread count. roomCode={}, memberId={}", roomCode, memberId, e);
            return 0L;
        }
    }

    /**
     * 마지막 읽은 메시지 seq 저장
     * 읽음 상태 추적용
     */
    @Override
    public void setLastReadSeq(String roomCode, Long memberId, Long messageSeq) {
        log.trace("[ChatMessageCache] Caching last read seq. roomCode={}, memberId={}, seq={}",
                roomCode, memberId, messageSeq);

        try {
            String key = String.format(ChatRoomKey.ROOM_LAST_READ_KEY_FORMAT, roomCode, memberId);
            redisTemplate.opsForValue().set(key, messageSeq, CACHE_TTL);

            log.debug("[ChatMessageCache] Success to cache last read seq. roomCode={}, memberId={}, seq={}",
                    roomCode, memberId, messageSeq);

        } catch (Exception e) {
            log.debug("[ChatMessageCache] Fail to cache last read seq. roomCode={}, memberId={}, seq={}",
                    roomCode, memberId, messageSeq);
        }
    }

    /**
     * 마지막 읽은 메시지 seq 조회
     */
    @Override
    public Long getLastReadSeq(String roomCode, Long memberId) {
        log.trace("[ChatMessageCache] Get cache last read seq. roomCode={}, memberId={}",
                roomCode, memberId);
        try {
            String key = String.format(ChatRoomKey.ROOM_LAST_READ_KEY_FORMAT, roomCode, memberId);
            Object value = redisTemplate.opsForValue().get(key);

            log.debug("[ChatMessageCache] Success to get cache last read seq. "
                            + "roomCode={}, memberId={}, lastReadSeq={}", roomCode, memberId, value);

            return value != null ? Long.parseLong(value.toString()) : null;
        } catch (Exception e) {
            log.debug("[ChatMessageCache] Fail to get cache last read seq. "
                    + "roomCode={}, memberId={}", roomCode, memberId);
            return null;
        }
    }

    /**
     * 전체 배지 카운트 증가
     * 사용자의 전체 미읽음 메시지 수
     */
    @Override
    public Long incrementBadgeCount(Long memberId) {
        log.trace("[ChatMessageCache] Increment badge count. memberId={}", memberId);
        try {
            String badgeKey = String.format(ChatRoomKey.USER_BADGE_KEY_FORMAT, memberId);
            Long count = redisTemplate.opsForValue().increment(badgeKey);
            redisTemplate.expire(badgeKey, BADGE_TTL);

            log.debug("[ChatMessageCache] Success increment badge count. memberId={}, newCount={}",
                    memberId, count);
            return count;

        } catch (Exception e) {
            log.warn("[ChatMessageCache] Fail increment badge count. memberId={}, ", memberId);
            return 0L;
        }
    }

    /**
     * 전체 배지 카운트 재계산
     * 읽음 처리 후 전체 카운트 재계산
     */
    @Override
    public Long recalculateBadgeCount(List<String> activeRooms, Long memberId) {
        log.trace("[ChatMessageCache] Recalculate badge count. memberId={}", memberId);

        try {
            long totalUnread = 0;
            for (String roomCode : activeRooms) {
                totalUnread += getUnreadCount(roomCode, memberId);
            }

            // 전체 배지 카운트 업데이트
            String badgeKey = String.format(ChatRoomKey.USER_BADGE_KEY_FORMAT, memberId);
            redisTemplate.opsForValue().set(badgeKey, totalUnread, BADGE_TTL);

            log.debug("[ChatMessageCache] Success to recalculate badge count. memberId={}, count={}",
                    memberId, totalUnread);
            return totalUnread;

        } catch (Exception e) {
            log.warn("[ChatMessageCache] Fail to recalculate badge count. memberId={}", memberId);
            log.error("Error recalculating badge count for user: {}", memberId, e);
            return 0L;
        }
    }

    /**
     * 전체 배지 카운트 조회
     * FAB 버튼에 표시할 전체 미읽음 수
     */
    @Override
    public Long getBadgeCount(Long memberId) {
        log.trace("[ChatMessageCache] Get badge count. memberId={}", memberId);
        String badgeKey = String.format(ChatRoomKey.USER_BADGE_KEY_FORMAT, memberId);

        Object value = redisTemplate.opsForValue().get(badgeKey);
        log.debug("[ChatMessageCache] Success to get badge count. memberId={}", memberId);

        return value == null ? 0L : Long.parseLong(value.toString());
    }
}
