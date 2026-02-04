package com.myce.api.ai.context;

import java.util.ArrayList;
import java.util.List;

/**
 * 공개 플랫폼 정보
 */
public record PublicContext(
        String availableExpos,
        String platformInfo,
        String pricingInfo
) {
    private static final String SUCCESS_PLATFORM_INFO_MESSAGE = """
            MYCE는 박람회 관리 플랫폼입니다.
            - 박람회 예약 및 관리
            - 티켓 구매 시스템
            - 실시간 채팅 상담
            """;

    private static final String SUCCESS_PRICE_INFO_MESSAGE = "요금제 정보는 개별 박람회마다 상이합니다.";
    private static final String FAIL_PLATFORM_INFO_MESSAGE = "플랫폼 정보 로딩 실패";
    private static final String FAIL_PRICE_INFO_MESSAGE = "요금 정보 조회 불가";

    public static PublicContext getSuccessPublicContext(String availableExpos) {
        return new PublicContext(availableExpos, SUCCESS_PLATFORM_INFO_MESSAGE, SUCCESS_PRICE_INFO_MESSAGE);
    }

    public static PublicContext getFailPublicContext() {
        return new PublicContext("", FAIL_PLATFORM_INFO_MESSAGE, FAIL_PRICE_INFO_MESSAGE);
    }


}