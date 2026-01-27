package com.myce.api.controller;

import com.myce.api.auth.dto.CustomUserDetails;
import com.myce.api.dto.request.ChatReadRequest;
import com.myce.api.dto.response.ChatMessageResponse;
import com.myce.api.dto.response.ChatRoomInfoListResponse;
import com.myce.api.service.ChatReadStatusService;
import com.myce.api.service.ExpoChatService;
import com.myce.common.dto.PageResponse;
import com.myce.common.type.LoginType;
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

/**
 * 박람회 관리자 채팅 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/chats/expos/{expoId}")
@RequiredArgsConstructor
public class ExpoChatController {

    private final ExpoChatService chatService;
    private final ChatReadStatusService chatReadStatusService;

    /**
     * 채팅방 목록 조회 (관리자용)
     */
    @GetMapping("/rooms")
    public ResponseEntity<ChatRoomInfoListResponse> getChatRooms(
            @PathVariable Long expoId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        LoginType loginType = userDetails.getLoginType();
        ChatRoomInfoListResponse response = chatService.getChatRoomsForAdmin(expoId, memberId, loginType);
        return ResponseEntity.ok(response);
    }

    /**
     * 채팅방 메시지 조회
     */
    @GetMapping("/rooms/{roomCode}/messages")
    public ResponseEntity<PageResponse<ChatMessageResponse>> getMessages(
            @PathVariable Long expoId,
            @PathVariable String roomCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        PageResponse<ChatMessageResponse> messages = chatService.getMessages(expoId, roomCode, page, size, memberId);
        return ResponseEntity.ok(messages);
    }

    /**
     * 메시지 읽음 처리
     */
    @PostMapping("/rooms/{roomCode}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long expoId,
            @PathVariable String roomCode,
            @RequestBody ChatReadRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        chatReadStatusService.markAsReadForAdmin(
                expoId,
                roomCode,
                request.getLastReadSeq(),
                memberId,
                Role.fromName(userDetails.getRole()),
                userDetails.getLoginType()
        );
        return ResponseEntity.ok().build();
    }

    /**
     * 안읽은 메시지 수 조회
     */
    @GetMapping("/rooms/{roomCode}/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable Long expoId,
            @PathVariable String roomCode,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        Long unreadCount = chatService.getUnreadCount(
                expoId,
                roomCode,
                memberId,
                userDetails.getLoginType()
        );
        return ResponseEntity.ok(unreadCount);
    }
}
