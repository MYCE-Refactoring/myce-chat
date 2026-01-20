package com.myce.api.controller;

import com.myce.api.auth.dto.CustomUserDetails;
import com.myce.api.dto.response.ChatRoomInfoListResponse;
import com.myce.api.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM-186: 채팅방 목록 조회 API
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats/rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /**
     * 사용자별 채팅방 목록 조회 (USER: 본인 참여, ADMIN: 관리 박람회 전체)
     */
    @GetMapping
    public ResponseEntity<ChatRoomInfoListResponse> getChatRooms(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long memberId = customUserDetails.getMemberId();
        String role = customUserDetails.getRole();

        log.info("[Request] Get chat rooms. memberId={}, role={}", memberId, role);

        ChatRoomInfoListResponse response = chatRoomService.getChatRooms(
                memberId, customUserDetails.getUsername(), role
        );

        log.info("[Response] Get chat rooms. memberId={}, count={}", memberId, response.getChatRooms().size());
        return ResponseEntity.ok(response);
    }

    // TODO 프론트 연결해야됨
    @GetMapping("/platform")
    public ResponseEntity<ChatRoomInfoListResponse> getPlatformChatRooms(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        ChatRoomInfoListResponse response = chatRoomService.getChatRooms(
                customUserDetails.getMemberId(),
                customUserDetails.getUsername(),
                customUserDetails.getRole()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 박람회별 채팅방 목록 조회 (관리자 전용, 권한 검증 포함)
     */
    @GetMapping("/expo/{expo-id}")
    public ResponseEntity<ChatRoomInfoListResponse> getChatRoomsByExpo(
            @PathVariable("expo-id") Long expoId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long memberId = customUserDetails.getMemberId();
        log.info("[Request] Get chat rooms by expo. expoId={}, memberId={}", expoId, memberId);

        ChatRoomInfoListResponse response = chatRoomService
                .getChatRoomsByExpo(expoId, customUserDetails.getMemberId());

        log.info("[Response] Get chat rooms by expo. expoId={}, memberId={}", expoId, memberId);
        return ResponseEntity.ok(response);
    }
}