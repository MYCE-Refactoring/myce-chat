package com.myce.api.service.client;

import com.myce.api.auth.filter.InternalHeaderKey;
import com.myce.api.dto.AdminCodeInfo;
import com.myce.api.dto.ExpoInfo;
import com.myce.api.dto.ExpoInfos;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpoClient {

    private static final String PREFIX_EXPO_INTERNAL_URI = "/internal/expos";
    private static final String GET_RECENT_EXPOS_URI =
            PREFIX_EXPO_INTERNAL_URI + "/recent?count=%d";
    private static final String GET_EXPO_INFO_URI =
            PREFIX_EXPO_INTERNAL_URI + "/%d";
    private static final String GET_ADMIN_EXPO_ACCESSIBLE_URI =
            PREFIX_EXPO_INTERNAL_URI + "/admin/access/check?expoId=%d&adminId=%d";
    private static final String GET_MEMBER_EXPO_OWNER_URI =
            PREFIX_EXPO_INTERNAL_URI + "/owner/check?expoId=%d&memberId=%d";
    private static final String GET_ADMIN_INFO_URI =
            PREFIX_EXPO_INTERNAL_URI + "/admin/%d";

    private final RestClient client;

    public ExpoInfos getRecentExpoInfos(int count) {
        String uri = String.format(GET_RECENT_EXPOS_URI, count);
        ResponseEntity<ExpoInfos> response = get(uri, ExpoInfos.class);

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            return null;
        }

        return response.getBody();
    }

    public ExpoInfo getExpoInfo(Long expoId) {
        String uri = String.format(GET_EXPO_INFO_URI, expoId);
        ResponseEntity<ExpoInfo> response = get(uri, ExpoInfo.class);

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new CustomException(CustomErrorCode.EXPO_NOT_EXIST);
        }

        return response.getBody();
    }

    public boolean checkAdminExpoAccessible(Long expoId, Long adminId) {
        String uri = String.format(GET_ADMIN_EXPO_ACCESSIBLE_URI, expoId, adminId);
        return get(uri);
    }

    public boolean checkMemberExpoOwner(Long expoId, Long memberId) {
        String uri = String.format(GET_MEMBER_EXPO_OWNER_URI, expoId, memberId);
        return get(uri);
    }

    public AdminCodeInfo getAdminCodeInto(Long adminId) {
        String uri = String.format(GET_ADMIN_INFO_URI, adminId);
        ResponseEntity<AdminCodeInfo> response = get(uri, AdminCodeInfo.class);

        HttpStatusCode status = response.getStatusCode();
        log.debug("[ExpoClient] Receive response from core. uri={}, status={}", uri, status);

        if (!status.equals(HttpStatus.OK)) {
            throw new CustomException(CustomErrorCode.MEMBER_NOT_EXIST);
        }

        return response.getBody();
    }

    private <T> ResponseEntity<T> get(String uri, Class<T> responseType) {
        log.debug("[ExpoClient] Send request to core. uri={}", uri);

        return client.get()
                .uri(uri)
                .retrieve()
                .toEntity(responseType);
    }

    private boolean get(String uri) {
        log.debug("[ExpoClient] Send request to core. uri={}", uri);

        return Boolean.TRUE.equals(client.get()
                .uri(uri)
                .exchange((req, res) -> {
                    log.debug("[ExpoClient-bodiless] res status={}", res.getStatusCode());
                    return res.getStatusCode().equals(HttpStatus.OK);
                }));
    }
}
