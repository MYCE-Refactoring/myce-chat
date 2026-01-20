package com.myce.api.service.client;

import com.myce.api.dto.MemberInfo;
import com.myce.common.exception.CustomErrorCode;
import com.myce.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberClient {

    private static final String PREFIX_MEMBER_INTERNAL_URI = "/internal/members";
    private static final String GET_MEMBER_INFO_URI =
            PREFIX_MEMBER_INTERNAL_URI + "/%d";

    private final RestClient client;

    public MemberInfo getMemberInfo(Long memberId) {
        ResponseEntity<MemberInfo> response = client.get()
                .uri(String.format(GET_MEMBER_INFO_URI, memberId))
                .retrieve()
                .toEntity(MemberInfo.class);

        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.info("Member not found. memberId={}", memberId);
            return null;
        }

        return response.getBody();
    }


}
