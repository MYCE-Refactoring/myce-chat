package com.myce.api.controller;

import com.myce.api.auth.dto.CustomUserDetails;
import com.myce.api.dto.request.ChatReadRequest;
import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.api.service.ChatMessageService;
import com.myce.api.service.ChatReadStatusService;
import com.myce.common.dto.PageResponse;
import com.myce.common.type.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats/rooms/{room-code}")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final ChatReadStatusService readStatusService;

    /**
     * 채팅방 메시지 히스토리 조회 (페이징)
     */
    @GetMapping("/messages")
    public ResponseEntity<PageResponse<ChatMessageResponse>> getMessages(
            @PathVariable("room-code") String roomCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long memberId = customUserDetails.getMemberId();
        Role role = Role.fromName(customUserDetails.getRole());
        PageResponse<ChatMessageResponse> response = chatMessageService.getMessages(
                roomCode, page, size, memberId, role);

        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 채팅방 읽음 처리 API
     */
    @PostMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable("room-code") String roomCode,
            @RequestBody(required = false) ChatReadRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Role role = Role.fromName(customUserDetails.getRole());
        Long lastReadSeq = request != null ? request.getLastReadSeq() : null;
        readStatusService.markAsReadForMember(
                roomCode,
                lastReadSeq,
                customUserDetails.getMemberId(),
                role,
                customUserDetails.getLoginType()
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * 특정 채팅방의 읽지 않은 메시지 수 조회 API
     * 역할 기반 접근 제어: USER는 본인 방만, ADMIN은 관리 권한 있는 방만
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable("room-code") String roomCode,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long unreadCount = chatMessageService.getUnreadCount(
                roomCode,
                customUserDetails.getMemberId(),
                customUserDetails.getRole(),
                customUserDetails.getLoginType()
        );

        return ResponseEntity.ok(unreadCount);
    }
}
