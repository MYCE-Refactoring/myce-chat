package com.myce.api.service.impl;

import com.myce.api.controller.supporter.ChatRoomResponseMakeService;
import com.myce.api.dto.ExpoInfo;
import com.myce.api.dto.MemberInfo;
import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.api.dto.response.ChatRoomInfoListResponse;
import com.myce.api.dto.response.ChatRoomInfoResponse;
import com.myce.api.dto.response.ChatUnreadCountResponse;
import com.myce.api.mapper.ChatMessageMapper;
import com.myce.api.mapper.ChatRoomMapper;
import com.myce.api.service.ChatUnreadService;
import com.myce.api.service.ExpoChatService;
import com.myce.api.service.client.ExpoClient;
import com.myce.api.service.client.MemberClient;
import com.myce.api.util.RoomCodeSupporter;
import com.myce.common.dto.PageResponse;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import com.myce.domain.document.ChatMessage;
import com.myce.domain.document.ChatRoom;
import com.myce.domain.document.type.MessageSenderType;
import com.myce.domain.repository.ChatMessageCacheRepository;
import com.myce.domain.repository.ChatMessageRepository;
import com.myce.domain.repository.ChatRoomCacheRepository;
import com.myce.domain.repository.ChatRoomRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 박람회 관리자 채팅 서비스 구현체
 * 기존 Service + ServiceImpl 패턴 준수
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpoChatServiceImpl implements ExpoChatService {

    private final ExpoClient expoClient;
    private final MemberClient memberClient;

    private final ChatUnreadService chatUnreadService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomAccessCheckService accessCheckService;
    private final ChatRoomCacheRepository chatRoomCacheRepository;
    private final ChatRoomResponseMakeService responseMakeService;
    private final ChatMessageCacheRepository chatMessageCacheRepository;

    @Override
    public ChatRoomInfoListResponse getChatRoomsForAdmin(Long expoId, Long memberId, LoginType loginType) {
        // 권한 검증
        accessCheckService.validateAdminPermission(expoId, memberId, loginType);

        // 해당 박람회의 채팅방 목록 조회
        List<ChatRoom> chatRooms = chatRoomRepository.findByExpoIdAndIsActiveTrueOrderByLastMessageAtDesc(expoId);
        return responseMakeService.convertToResponse(chatRooms, 0L, Role.EXPO_ADMIN);
    }

    @Override
    public PageResponse<ChatMessageResponse> getMessages(Long expoId, String roomCode, int page, int size, Long memberId) {

        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));

        if (!chatRoom.getExpoId().equals(expoId)) {
            throw new CustomException(CustomErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<ChatMessage> messages = chatMessageRepository.findByRoomCodeOrderBySentAtDesc(roomCode, pageable);

        String readStatus = chatRoom.getReadStatusJson();
        List<ChatMessageResponse> chatMessageResponse = new ArrayList<>();
        for (ChatMessage chatMessage: messages) {
            int isRead = chatUnreadService.isReadMessage(chatMessage, readStatus);
            chatMessageResponse.add(mapToMessageResponse(chatMessage, isRead));
        }

        return new PageResponse<>(
                chatMessageResponse,
                messages.getNumber(),
                messages.getSize(),
                messages.getTotalElements(),
                messages.getTotalPages()
        );
    }
    
    /**
     * 관리자 읽음 상태 업데이트
     */
    // 중복 메서드 제거됨 - ChatReadStatusUtil로 통합

    @Override
    public Long getUnreadCount(Long expoId, String roomCode, Long memberId) {
        // 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.CHAT_ROOM_NOT_EXIST));
        
        // 박람회 일치 확인
        if (!chatRoom.getExpoId().equals(expoId)) {
            throw new CustomException(CustomErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
        
        // 성능 최적화: 미읽음 카운트 조회 시 권한 검증 생략 (채팅방 접근 시에만 검증)
        
        try {
            // Redis에서 관리자의 미읽음 카운트 조회 (10ms 미만)
            Long cachedUnreadCount = chatMessageCacheRepository.getUnreadCount(roomCode, memberId);
            
            if (cachedUnreadCount != null && cachedUnreadCount > 0) {
                log.debug("Cache hit: unread count {} for admin {} in room {}", cachedUnreadCount, memberId, roomCode);
                return cachedUnreadCount;
            }
            
            // 캐시 미스 시 MongoDB에서 계산하고 캐싱
            long unreadCount = chatMessageRepository.countByRoomCodeAndSenderType(roomCode, "USER");
            
            // 결과를 Redis에 캐싱 (다음 조회 시 빠른 응답)
            if (unreadCount > 0) {
                // 정확한 값으로 설정하기 위해 리셋 후 증가
                chatMessageCacheRepository.resetUnreadCount(roomCode, memberId);
                chatMessageCacheRepository.incrementUnreadCount(roomCode, memberId, unreadCount);
            }
            
            log.debug("Cache miss: calculated unread count {} for admin {} in room {}", unreadCount, memberId, roomCode);
            return unreadCount;
            
        } catch (Exception e) {
            log.error("안읽은 메시지 수 조회 실패 - roomCode: {}", roomCode, e);
            return 0L; // 에러 시 0 반환
        }
    }

    // extractLastReadMessageId 메서드 제거됨 - ChatUnreadService로 통합
    @Override
    public ChatUnreadCountResponse getAllUnreadCountsForUser(Long memberId) {
        try {
            // Redis에서 전체 배지 카운트 조회 (5ms 이내)
            Long totalBadgeCount = chatMessageCacheRepository.getBadgeCount(memberId);
            log.debug(" Redis 배지 카운트 조회 - memberId: {}, count: {}", memberId, totalBadgeCount);
            
            if (totalBadgeCount == 0) {
                return new ChatUnreadCountResponse(0L);
            }
            
            // 상세 정보가 필요한 경우에만 개별 채팅방 조회 (옵션)
            List<ChatRoom> userChatRooms = chatRoomRepository.findByMemberIdAndIsActiveTrueOrderByLastMessageAtDesc(memberId);
            if (userChatRooms.isEmpty()) {
                // Redis 카운트가 있는데 채팅방이 없으면 Redis 재계산
                totalBadgeCount = recalculateBadgeCount(memberId);
                return new ChatUnreadCountResponse(totalBadgeCount);
            }

            ChatUnreadCountResponse response = new ChatUnreadCountResponse(0L);
            // 개별 채팅방 미읽음 카운트는 Redis에서 조회 (빠른 응답)
            for (ChatRoom chatRoom : userChatRooms) {
                String roomCode = chatRoom.getRoomCode();
                Long unreadCount = chatMessageCacheRepository.getUnreadCount(roomCode, memberId);
                if (unreadCount == null) {
                    String readStatus = chatRoom.getReadStatusJson();
                    unreadCount = chatUnreadService.getUnreadCountForViewer(roomCode, readStatus, memberId, Role.USER);
                    chatMessageCacheRepository.incrementUnreadCount(roomCode, memberId, unreadCount);
                }
                response.addRoomUnreadCount(roomCode, unreadCount);
            }
            
            log.debug(" 전체 미읽음 카운트 조회 완료 - memberId: {}, total: {}, rooms: {}",
                     memberId, totalBadgeCount, response.getUnreadCounts().size());
            
            return response;
            
        } catch (Exception e) {
            log.error(" 사용자 전체 읽지 않은 메시지 수 조회 실패 - memberId: {}", memberId, e);
            // 에러 시 기존 방식으로 폴백
            return getAllUnreadCountsForUserFallback(memberId);
        }
    }

    @Override
    @Transactional
    public ChatRoomInfoResponse getOrCreateExpoChatRoom(Long expoId, Long memberId) {
        log.info("[ExpoChatService] Get or create expo chat room. expoId={}, userId={}", expoId, memberId);

        ExpoInfo expo = expoClient.getExpoInfo(expoId);
        MemberInfo member = memberClient.getMemberInfo(memberId);

        // 3. 채팅방 코드 생성 (admin-{expoId}-{userId})
        String roomCode = RoomCodeSupporter.getAdminRoomCode(expoId, memberId);

        // 4. 기존 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode).orElse(null);

        if (chatRoom != null) {
            log.info(" 기존 채팅방 조회 성공 - roomCode: {}", roomCode);

            // 기존 채팅방 재활성화 (필요한 경우)
            if (!chatRoom.getIsActive()) {
                chatRoom.reactivate();
                chatRoomRepository.save(chatRoom);
                log.info("🔄 비활성 채팅방 재활성화 - roomCode: {}", roomCode);
            }

            return ChatRoomMapper.convertToResponse(chatRoom, 0);
        }

        // 5. 새 채팅방 생성
        ChatRoom newRoom = ChatRoom.builder()
                .roomCode(roomCode)
                .memberId(memberId)
                .memberName(member.getName())
                .expoId(expoId)
                .expoTitle(expo.getTitle())
                .build();

        ChatRoom savedRoom = chatRoomRepository.save(newRoom);
        log.info(" 새 박람회 채팅방 생성 완료 - roomCode: {}, expoTitle: {}", roomCode, expo.getTitle());

        // 6. AI 환영 메시지 생성 (선택사항 - 필요시 구현)
        // createWelcomeMessage(savedRoom, expo, member);

        return ChatRoomMapper.convertToResponse(savedRoom, 0);
    }

    /**
     * ChatMessage -> MessageResponse 매핑
     */
    private ChatMessageResponse mapToMessageResponse(ChatMessage message, int isRead) {
        // 관리자 메시지인 경우 관리자 정보 추가
        String adminCode = null;
        String adminDisplayName = null;
        if (MessageSenderType.ADMIN.equals(message.getSenderType())) {
            adminCode = message.getActualSender();
            adminDisplayName = MessageSenderType.ADMIN.name();
        }

        // ChatMessageMapper 사용
        return ChatMessageMapper.toResponse(message, isRead, adminCode, adminDisplayName);
    }

    /**
     * Redis 실패 시 폴백 메서드 (기존 방식)
     */
    private ChatUnreadCountResponse getAllUnreadCountsForUserFallback(Long memberId) {
        List<ChatRoom> userChatRooms = chatRoomRepository.findByMemberIdAndIsActiveTrueOrderByLastMessageAtDesc(memberId);
        ChatUnreadCountResponse response = new ChatUnreadCountResponse(0L);
        if (userChatRooms.isEmpty()) return response;

        long totalBadgeCount = 0L;
        for (ChatRoom chatRoom : userChatRooms) {
            String roomCode = chatRoom.getRoomCode();
            String readStatus = chatRoom.getReadStatusJson();
            long count = chatUnreadService.getUnreadCountForViewer(roomCode, readStatus, memberId, Role.USER);
            response.addRoomUnreadCount(roomCode, count);
            totalBadgeCount += count;
        }

        response.updateTotalUnreadCount(totalBadgeCount);
        return response;
    }

    private long recalculateBadgeCount(Long memberId) {
        List<String> activeRooms = chatRoomCacheRepository.getUserActiveRooms(memberId);
        return chatMessageCacheRepository.recalculateBadgeCount(activeRooms, memberId);
    }
}