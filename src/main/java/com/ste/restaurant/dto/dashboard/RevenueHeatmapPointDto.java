package com.ste.restaurant.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevenueHeatmapPointDto {
    private String dayOfWeek; // E.g., "Monday"
    private int hourOfDay;    // 0-23
    private BigDecimal revenue;
}
