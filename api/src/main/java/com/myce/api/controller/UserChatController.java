package com.myce.api.controller;

import com.myce.api.auth.dto.CustomUserDetails;
import com.myce.api.dto.response.ChatUnreadCountResponse;
import com.myce.api.service.ExpoChatService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자용 채팅 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class UserChatController {
    
    private final ExpoChatService chatService;
    
    /**
     * FAB용 전체 읽지 않은 메시지 수 조회
     */
    @GetMapping("/rooms/unread-counts")
    public ResponseEntity<ChatUnreadCountResponse> getAllUnreadCounts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getMemberId();
        ChatUnreadCountResponse result = chatService.getAllUnreadCountsForUser(userId);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 박람회 채팅방 생성 또는 조회
     * 유저가 박람회 상세 페이지에서 1:1 채팅을 시작할 때 사용
     */
    @PostMapping("/expo/{expoId}/room")
    public ResponseEntity<Map<String, Object>> getOrCreateExpoChatRoom(
            @PathVariable Long expoId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        Map<String, Object> chatRoom = chatService.getOrCreateExpoChatRoom(expoId, memberId);
        
        return ResponseEntity.ok(chatRoom);
    }
}