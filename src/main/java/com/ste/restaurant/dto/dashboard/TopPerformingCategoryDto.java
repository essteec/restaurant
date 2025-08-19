package com.ste.restaurant.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopPerformingCategoryDto {
    private String categoryName;
    private BigDecimal totalRevenue;
}
