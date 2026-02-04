package com.myce.api.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TicketInfo {
    private String name;
    private int price;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalQuantity;
    private int remainingQuantity;

}
