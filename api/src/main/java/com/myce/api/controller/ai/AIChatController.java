package com.myce.api.controller.ai;

import com.myce.api.auth.dto.CustomUserDetails;
import com.myce.api.dto.ChatStatusResponse;
import com.myce.api.dto.ConversationSummaryResponse;
import com.myce.api.service.AIChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 채팅 컨트롤러 - 플랫폼 상담 AI 서비스
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/chat")
public class AIChatController {

    private final AIChatService aiChatService;

    /**
     * 대화 요약 생성 (관리자 인계용)
     */
    @GetMapping("/{roomCode}/summary")
    public ResponseEntity<Object> generateConversationSummary(
            @PathVariable String roomCode,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Receive request for summary chat messages. roomCode={}, memberId={}",
                roomCode, userDetails.getMemberId());

        ConversationSummaryResponse response = aiChatService.getConversationSummaryForAdmin(
                roomCode,
                userDetails.getRole()
        );

        return ResponseEntity.ok(response);
    }
    
    /**
     * AI 채팅 활성화 상태 확인
     */
    @GetMapping("/{roomCode}/status")
    public ResponseEntity<Object> getAIChatStatus(@PathVariable String roomCode) {
        ChatStatusResponse response = aiChatService.getAiChatStatus(roomCode);
        return ResponseEntity.ok(response);
    }
}