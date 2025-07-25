package com.ste.restaurant.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsDto {
    private long totalOrders;
    private BigDecimal totalSales;
    private long totalFoodItemSold;
}
