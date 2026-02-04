package com.myce.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExpoInfo {
    private String title;
    private String location;
    private String locationDetail;
    private Long ownerMemberId;
    List<TicketInfo> ticketInfos;
}
