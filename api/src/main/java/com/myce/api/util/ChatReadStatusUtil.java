package com.myce.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅 읽음 상태 처리 유틸리티 클래스
 * 
 * 기존 ChatRoomServiceImpl과 ExpoChatServiceImpl에서 중복으로 구현된
 * 읽음 상태 JSON 업데이트 로직을 통합하여 일관성과 유지보수성 향상
 */
@Slf4j
public final class ChatReadStatusUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();;

    private ChatReadStatusUtil() {
    }

    public static String updateReadStatus(String readStatus, String reader, String lastReadMessageId) {
        try {
            Map<String, String> statusMap = parseReadStatus(readStatus);
            statusMap.put(reader, lastReadMessageId);
            return objectMapper.writeValueAsString(statusMap);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid readStatus JSON: " + readStatus, e);
        }
    }

    public static String extractLastReadMessageId(String readStatus, String reader) {
        if (readStatus == null || readStatus.isEmpty() || readStatus.equals("{}")) {
            return null;
        }

        try {
            Map<String, String> statusMap = parseReadStatus(readStatus);
            if (!statusMap.containsKey(reader)) {
                return null;
            }

            return statusMap.get(reader);
        } catch (JsonProcessingException e) {
            log.warn("readStatus 파싱 실패: {}", readStatus, e);
            return null;
        }
    }

    private static Map<String, String> parseReadStatus(String readStatus)
            throws JsonProcessingException {

        if (readStatus == null || readStatus.isBlank() || "{}".equals(readStatus)) {
            return new HashMap<>();
        }

        return objectMapper.readValue(readStatus, new TypeReference<>() {});
    }
}