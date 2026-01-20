package com.myce.domain.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.repository.ChatRoomCacheRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatRoomCacheRepositoryImpl implements ChatRoomCacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration BADGE_TTL = Duration.ofDays(7);
    private static final Duration CHAT_ROOM_TTL = Duration.ofMinutes(30);

    /**
     * 채팅방 캐시 무효화
     * 채팅방 삭제 시 관련 캐시 정리
     */
    @Override
    public void invalidateRoomCache(String roomCode) {
        log.trace("[ChatRoomCache] Invalidate cached room. roomCode={}", roomCode);
        try {
            // 채팅방 관련 모든 캐시 삭제
            String keyPattern = String.format(ChatRoomKey.ROOM_KEY_FORMAT, roomCode) +"*";
            Set<String> keys = redisTemplate.keys(keyPattern);

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("[ChatRoomCache] Success to invalidate cached room. roomCode={}", roomCode);
            }

        } catch (Exception e) {
            log.debug("[ChatRoomCache] Fail to invalidate cached room. roomCode={}", roomCode, e);
        }
    }

    /**
     * 사용자별 활성 채팅방 목록 조회
     * 배지 카운트 재계산 시 사용
     */
    @Override
    public List<String> getUserActiveRooms(Long memberId) {
        log.trace("[ChatRoomCache] Get user active rooms. memberId={}", memberId);
        try {
            String key = String.format(ChatRoomKey.USER_ACTIVE_ROOMS_KEY_FORMAT, memberId);
            Set<Object> rooms = redisTemplate.opsForSet().members(key);

            if (rooms == null || rooms.isEmpty()) {
                return new ArrayList<>();
            }
            log.debug("[ChatRoomCache] Success to get member active rooms. memberId={}", memberId);
            return rooms.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[ChatRoomCache] Fail to get member active rooms. memberId={}", memberId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 사용자 활성 채팅방 추가
     * 새 채팅방 생성 시 호출
     */
    @Override
    public void addUserActiveRoom(Long memberId, String roomCode) {
        log.trace("[ChatRoomCache] Add member active room. memberId={}, roomCode={}",
                memberId, roomCode);
        try {
            String key = String.format(ChatRoomKey.USER_ACTIVE_ROOMS_KEY_FORMAT, memberId);
            redisTemplate.opsForSet().add(key, roomCode);
            redisTemplate.expire(key, BADGE_TTL);

            log.debug("[ChatRoomCache] Success to add member active room. memberId={}, roomCode={}",
                    memberId, roomCode);

        } catch (Exception e) {
            log.error("[ChatRoomCache] Fail to add member active room. memberId={}, roomCode={}",
                    memberId, roomCode, e);
        }
    }

    /**
     * ChatRoom 캐시 조회
     */
    @Override
    public ChatRoom getCachedChatRoom(String roomCode) {
        log.trace("[ChatRoomCache] Get cached chat room. roomCode={}", roomCode);
        try {
            String key = String.format(ChatRoomKey.ROOM_KEY_FORMAT, roomCode);
            Object cachedData = redisTemplate.opsForValue().get(key);

            if (cachedData != null) {
                ChatRoom chatRoom;
                if (cachedData instanceof String) {
                    chatRoom = objectMapper.readValue((String) cachedData, ChatRoom.class);
                } else {
                    chatRoom = objectMapper.convertValue(cachedData, ChatRoom.class);
                }

                log.debug("[ChatRoomCache] Cache hit. roomCode={}", roomCode);
                return chatRoom;
            }

            log.debug("[ChatRoomCache] Cache miss. roomCode={}", roomCode);
            return null;

        } catch (Exception e) {
            log.error("[ChatRoomCache] Fail to get cached chat room. roomCode={}, error={}",
                    roomCode, e.getMessage(), e);
            return null;
        }
    }

    /**
     * ChatRoom 캐싱
     */
    @Override
    public void cacheChatRoom(String roomCode, ChatRoom chatRoom) {
        log.trace("[ChatRoomCache] ChatRoom cached. roomCode={}", roomCode);
        try {
            String key = String.format(ChatRoomKey.ROOM_KEY_FORMAT, roomCode);
            String jsonValue = objectMapper.writeValueAsString(chatRoom);

            // 30분 TTL (ChatRoom 정보는 상대적으로 오래 유지)
            redisTemplate.opsForValue().set(key, jsonValue, CHAT_ROOM_TTL);
            log.debug("[ChatRoomCache] Success to ChatRoom cached. roomCode={}", roomCode);

        } catch (Exception e) {
            log.error("[ChatRoomCache] Fail to caching chat room. roomCode={}, error={}", roomCode, e.getMessage(), e);
        }
    }
}
