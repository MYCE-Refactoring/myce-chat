package com.myce.api.service.ai;

import com.myce.api.ai.context.PublicContext;
import com.myce.api.ai.context.UserContext;
import com.myce.api.dto.ExpoInfo;
import com.myce.api.dto.ExpoInfos;
import com.myce.api.dto.MemberInfo;
import com.myce.api.dto.TicketInfo;
import com.myce.api.service.AIChatContextService;
import com.myce.api.service.client.ExpoClient;
import com.myce.api.service.client.MemberClient;
import com.myce.api.util.RoomCodeSupporter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatContextServiceImpl implements AIChatContextService {

    private final MemberClient memberClient;
    private final ExpoClient expoClient;

    private static final int RECENT_EXPO_COUNT = 5;
    private static final String LOCATION_INFO_FORMAT = " • 위치: %s %s";
    private static final String TICKET_INFO_FORMAT = "  - %s: %,d원 (판매: %s~%s) %s";
    private static final String PREPARE_TICKET_INFO_MESSAGE = "• %s: 티켓 정보 준비 중";
    private static final String SOLD_OUT_MESSAGE = "(매진)";
    private static final String REMAINING_QUANTITY = "(예약가능 - 잔여: %d매)";

    /**
     * 사용자별 컨텍스트 구성 (격리된 정보만 제공)
     */
    public UserContext buildUserContext(String roomCode) {
        Long memberId = RoomCodeSupporter.extractMemberIdFromPlatformRoomCode(roomCode);

        // 사용자 기본 정보 조회
        MemberInfo memberInfo = memberClient.getMemberInfo(memberId);

        // 프론트에서 처리?
        // return new UserContext("사용자", "일반", List.of(), "정보 없음", userId);

        // TODO: 예약 정보, 결제 상태 등은 추후 구현
        List<String> recentReservations = List.of("예약 정보 조회 예정");
        String paymentStatus = "결제 상태 조회 예정";

        return new UserContext(
                memberInfo.getName(),
                memberInfo.getGrade(),
                recentReservations,
                paymentStatus,
                memberId
        );
    }

    /**
     * 공개 플랫폼 정보 구성
     */
    public PublicContext buildPublicContext() {
        ExpoInfos expoInfos = expoClient.getRecentExpoInfos(RECENT_EXPO_COUNT);
        if (expoInfos == null || expoInfos.getExpoInfos() == null) {
            return PublicContext.getFailPublicContext();
        }

        // 박람회 기본 정보
        StringBuilder totalExpoInfo = new StringBuilder();

        for (ExpoInfo expoInfo : expoInfos.getExpoInfos()) {
            String title = expoInfo.getTitle();
            totalExpoInfo.append(title).append("\n");

            String location = getLocationInfoStr(expoInfo.getLocation(), expoInfo.getLocationDetail());
            totalExpoInfo.append(location).append("\n");

            List<TicketInfo> ticketInfos = expoInfo.getTicketInfos();
            if (ticketInfos.isEmpty()) {
                totalExpoInfo.append(PREPARE_TICKET_INFO_MESSAGE).append("\n");
            } else {
                for (TicketInfo ticketInfo : ticketInfos) {
                    totalExpoInfo.append(getTicketInfoStr(ticketInfo)).append("\n");
                }
            }

            totalExpoInfo.append("\n");
        }

        return PublicContext.getSuccessPublicContext(totalExpoInfo.toString());
    }

    private String getTicketInfoStr(TicketInfo ticketInfo) {
        return String.format(
                TICKET_INFO_FORMAT,
                ticketInfo.getName(),
                ticketInfo.getPrice(),
                ticketInfo.getStartDate(),
                ticketInfo.getEndDate(),
                getRemainingStatus(ticketInfo.getRemainingQuantity())
        );
    }

    private String getRemainingStatus(int quantity) {
        if (quantity <= 0) return SOLD_OUT_MESSAGE;

        return String.format(REMAINING_QUANTITY, quantity);
    }

    private String getLocationInfoStr(String location, String locationDetail) {
        return String.format(LOCATION_INFO_FORMAT, location, locationDetail);
    }
}
