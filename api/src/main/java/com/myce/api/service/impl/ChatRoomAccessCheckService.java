package com.myce.api.service.impl;

import com.myce.api.service.client.ExpoClient;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import com.myce.common.type.LoginType;
import com.myce.common.type.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomAccessCheckService {

    private final ExpoClient expoClient;

    public boolean isValidAccess(LoginType loginType, Long expoId, Long memberId, Long participantId, Role role) {
        if (LoginType.ADMIN_CODE.equals(loginType)) {
            return isValidAccessForAdminCode(expoId, memberId);
        }

        return isValidAccessForMember(expoId, memberId, participantId, role);
    }

    private boolean isValidAccessForAdminCode(Long expoId, Long memberId) {
        log.debug("[AccessCheck] validate access for adminCode. expoId={}, memberId={}",
                expoId, memberId);
        return expoClient.checkAdminExpoAccessible(expoId, memberId);
    }

    private boolean isValidAccessForMember(Long expoId, Long memberId, Long participantId, Role role) {
        log.debug("[AccessCheck] validate access for member. expoId={}, memberId={}, role={}",
                expoId, memberId, role);

        if (Role.EXPO_ADMIN.equals(role)) {
            return memberId.equals(participantId) || expoClient.checkMemberExpoOwner(expoId, memberId);
        } else if (Role.USER.equals(role)) {
            return memberId.equals(participantId);
        } else return false;
    }

    /**
     * 사용자가 특정 채팅방에 접근할 권한이 있는지 검증합니다.
     */
    public void validateAccess(boolean isPlatformRoom, Long chatRoomMemberId,
            Long expoId, Long memberId, Role role) {

        if (isPlatformRoom) {
            // 관리자나 본인이 생성한 채팅방일 경우 항상 접근 가능
            if (!(Role.PLATFORM_ADMIN.equals(role) || chatRoomMemberId.equals(memberId))) {
                throw new CustomException(CustomErrorCode.CHAT_ROOM_ACCESS_DENIED);
            }
        } else {
            if (!isValidExpoChatAccess(expoId, chatRoomMemberId, memberId, role)) {
                throw new CustomException(CustomErrorCode.CHAT_ROOM_ACCESS_DENIED);
            }
        }
    }

    /**
     * 박람회 채팅방에 대한 접근 권한을 검증합니다.
     * AdminCode 기반 권한과 Expo Owner 권한을 모두 확인합니다.
     */
    private boolean isValidExpoChatAccess(Long expoId, Long chatRoomMemberId, Long memberId, Role role) {
        log.debug("[AccessCheck] validate expo chat permission. expoId={}, memberId={}, role={}",
                expoId, memberId, role);

        // 일반 사용자: 본인이 생성한 채팅방만 접근 가능
        if (chatRoomMemberId.equals(memberId)) {
            log.debug("[AccessCheck] Validate expo chat permission. expoId={}, memberId={}, isValid={}",
                    expoId, memberId, true);
        }

        if (Role.EXPO_ADMIN.equals(role)) {
            // EXPO_ADMIN: AdminCode 권한 확인
            boolean isOwner = expoClient.checkMemberExpoOwner(expoId, memberId);
            boolean isAdmin = expoClient.checkAdminExpoAccessible(expoId, memberId);
            log.debug("[AccessCheck] Validate expo chat permission. expoId={}, memberId={}, isValid={}",
                    expoId, memberId, isAdmin || isOwner);
            return isAdmin || isOwner || chatRoomMemberId.equals(memberId);
        }

        return false;
    }

    /**
     * 관리자 권한 검증
     */
    public void validateAdminPermission(Long expoId, Long memberId, LoginType loginType) {
        log.debug("[AccessCheck] validate admin permission. expoId={}, memberId={}, loginType={}",
                expoId, memberId, loginType);
        if (LoginType.ADMIN_CODE.equals(loginType)) {
            boolean isAdmin = expoClient.checkAdminExpoAccessible(expoId, memberId);
            if (!isAdmin) {
                throw new CustomException(CustomErrorCode.CHAT_ROOM_ACCESS_DENIED);
            }
        } else {
            boolean isOwner = expoClient.checkMemberExpoOwner(expoId, memberId);
            if (!isOwner) {
                throw new CustomException(CustomErrorCode.CHAT_ROOM_ACCESS_DENIED);
            }
        }

        log.trace("[AccessCheck] allow validate admin permission. expoId={}, memberId={}, loginType={}",
                expoId, memberId, loginType);
    }
}
