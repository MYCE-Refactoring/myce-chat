package com.myce.api.service.impl;

import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.api.mapper.ChatMessageMapper;
import com.myce.api.service.ChatMessageService;
import com.myce.api.service.ChatUnreadService;
import com.myce.api.service.component.ChatMessageCreateComponent;
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
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 채팅 메시지 생성 서비스 구현체
 * <p>
 * 메시지 생성 로직을 중앙화하여 일관성 보장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatUnreadService unreadService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomAccessCheckService accessCheckService;
    private final ChatRoomCacheRepository chatRoomCacheRepository;
    private final ChatMessageCacheRepository chatMessageCacheRepository;
    private final ChatMessageCreateComponent chatMessageCreateComponent;

    private static final int MAX_PAGE_SIZE = 1000;

    @Override
    public ChatMessage saveAIChatMessage(String roomCode, String content) {
        ChatMessage chatMessage = chatMessageCreateComponent
                .createAIMessage(roomCode, content);
        return chatMessageRepository.save(chatMessage);
    }

    @Override
    public ChatMessage saveSystemChatMessage(String roomCode, String content) {
        ChatMessage chatMessage = chatMessageCreateComponent
                .createSystemMessage(roomCode, content);
        return chatMessageRepository.save(chatMessage);
    }

    @Override
    public ChatMessage saveChatMessage(
            String roomCode, MessageSenderType senderType,
            Long senderId, String senderName, String content) {

        ChatMessage chatMessage = chatMessageCreateComponent
                .createMessage(roomCode, senderType, senderId, senderName, content);
        return chatMessageRepository.save(chatMessage);
    }

    @Override
    public List<ChatMessage> getRecentMessages(String roomCode) {
        return chatMessageRepository
                .findTop50ByRoomCodeOrderBySentAtDesc(roomCode);
    }

    @Override
    public ChatMessage getRecentMessage(String roomCode) {
        return chatMessageRepository
                .findTop1ByRoomCodeOrderBySentAtDesc(roomCode).orElse(null);
    }

    @Override
    public PageResponse<ChatMessageResponse> getMessages(
            String roomCode, int page, int size, Long memberId, Role role) {
        log.debug("[ChatMessage] get message. roomCode={}, page={}, size={}, memberId={}, role={}",
                roomCode, page, size, memberId, role);
        boolean isPlatformRoom = RoomCodeSupporter.isPlatformRoom(roomCode);

        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_EXIST));
        accessCheckService.validateAccess(isPlatformRoom, chatRoom.getMemberId(), chatRoom.getExpoId(), memberId, role);

        List<ChatMessage> chatMessages = null;
        int pageNumber = 1;
        int pageSize = size;
        long totalElements = size;
        int totalPage = 1;
        // Redis 캐시 확인
        if (page == 0 && size <= 50) {
            chatMessages = chatMessageCacheRepository.getCachedRecentMessages(roomCode, size);
            if (chatMessages != null && !chatMessages.isEmpty()) {
                if (chatMessages.size() < size) {
                    // 캐시가 부족하면 DB에서 다시 조회 (스크롤/hasMore 오류 방지)
                    chatMessages = null;
                } else {
                    Map<String, Long> readStatus = chatRoom.getReadStatus();
                    for (ChatMessage chatMessage : chatMessages) {
                        log.debug("@@@@@@@@ message check. messageSeq={}, isRead={}", chatMessage.getSeq(), chatMessage.getUnreadCount());
                        if (chatMessage.getUnreadCount() > 0 && unreadService.isReadMessage(chatMessage, readStatus)) {
                            chatMessage.decreaseUnreadCount();
                            log.debug("@@@@@@@@@@@ decrease Unread message. messageSeq={}", chatMessage.getSeq());
                        }
                    }
                }
            }
        }

        if (chatMessages == null || chatMessages.isEmpty()) {
            // 캐시 미스 또는 첫 페이지가 아닌 경우 - MongoDB 조회
            log.debug("[ChatMessage] Cache miss or not first page for get message. roomCode={}", roomCode);
            Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
            Page<ChatMessage> messagePage = chatMessageRepository.findByRoomCodeOrderBySentAtDesc(roomCode, pageable);

            chatMessages = messagePage.getContent();
            if (pageable.getPageNumber() == 0 && !messagePage.getContent().isEmpty()) {
                chatMessageCacheRepository.cacheRecentMessages(roomCode, messagePage.getContent());
                log.debug("[ChatMessage] Cached messages. roomCode={}, size={}",
                        roomCode, messagePage.getContent().size());
            }

            pageNumber = messagePage.getNumber();
            pageSize = messagePage.getSize();
            totalElements = messagePage.getTotalElements();
            totalPage = messagePage.getTotalPages();
        }

        List<ChatMessageResponse> chatMessageResponse = chatMessages.stream()
            .map(message -> {
                return ChatMessageMapper.toResponse(message);
            })
            .toList();

        log.debug("[ChatMessage] Success to get message. roomCode: {}, messageCount: {}, totalMessageCount: {}",
                roomCode, pageSize, totalElements);


        return new PageResponse<>(
                chatMessageResponse,
                pageNumber,
                pageSize,
                totalElements,
                totalPage
        );
    }

    /**
     * 읽지 않은 메시지 수 조회
     */
    @Override
    public Long getUnreadCount(String roomCode, Long memberId, String memberRole, LoginType loginType) {
        // 권한 검증
        Role role = Role.fromName(memberRole);
        ChatRoom chatRoom = chatRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new CustomException(CustomErrorCode.EXPO_NOT_EXIST));
        boolean isPlatformRoom = RoomCodeSupporter.isPlatformRoom(roomCode);
        accessCheckService.validateAccess(isPlatformRoom, chatRoom.getMemberId(), chatRoom.getExpoId(), memberId, role);

        return unreadService.getUnreadCount(roomCode, chatRoom.getReadStatus(), memberId, role, loginType);
    }
}
