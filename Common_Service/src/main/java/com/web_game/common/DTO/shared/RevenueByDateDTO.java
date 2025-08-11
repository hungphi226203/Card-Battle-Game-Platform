package com.web_game.common.DTO.shared;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RevenueByDateDTO {
    private LocalDate date;
    private BigDecimal totalRevenue;

    public RevenueByDateDTO(LocalDate date, BigDecimal totalRevenue) {
        this.date = date;
        this.totalRevenue = totalRevenue;
    }
}
