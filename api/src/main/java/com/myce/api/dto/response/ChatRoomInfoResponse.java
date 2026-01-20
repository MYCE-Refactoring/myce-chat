package com.myce.api.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomInfoResponse {

    /**
     * 채팅방 고유 ID
     */
    private String id;

    /**
     * 채팅방 코드 (admin-{expoId}-{userId} 형식)
     */
    private String roomCode;

    /**
     * 박람회 ID
     */
    private Long expoId;

    /**
     * 박람회 제목
     */
    private String expoTitle;

    /**
     * 상대방 회원 ID
     */
    private Long otherMemberId;

    /**
     * 상대방 이름
     */
    private String otherMemberName;

    /**
     * 상대방 역할 (ChatMemberRole enum 값)
     */
    private String otherMemberRole;

    /**
     * 마지막 메시지 내용
     */
    private String lastMessage;

    /**
     * 마지막 메시지 시간
     */
    private LocalDateTime lastMessageAt;

    /**
     * 읽지 않은 메시지 개수
     */
    private long unreadCount;

    /**
     * 채팅방 활성화 상태
     */
    private Boolean isActive;

    /**
     * 현재 담당 관리자 코드
     */
    private String currentAdminCode;

    /**
     * 담당 관리자 표시명 (관리자용)
     */
    private String adminDisplayName;

    /**
     * 현재 채팅방 상태 (AI_ACTIVE, WAITING_FOR_ADMIN, ADMIN_ACTIVE)
     */
    private String currentState;

    @Builder
    public ChatRoomInfoResponse(String id, String roomCode, Long expoId, String expoTitle,
            Long otherMemberId, String otherMemberName, String otherMemberRole,
            String lastMessage, LocalDateTime lastMessageAt,
            long unreadCount, Boolean isActive,
            String currentAdminCode, String adminDisplayName, String currentState) {
        this.id = id;
        this.roomCode = roomCode;
        this.expoId = expoId;
        this.expoTitle = expoTitle;
        this.otherMemberId = otherMemberId;
        this.otherMemberName = otherMemberName;
        this.otherMemberRole = otherMemberRole;
        this.lastMessage = lastMessage;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount;
        this.isActive = isActive != null ? isActive : true;
        this.currentAdminCode = currentAdminCode;
        this.adminDisplayName = adminDisplayName;
        this.currentState = currentState;
    }
}
