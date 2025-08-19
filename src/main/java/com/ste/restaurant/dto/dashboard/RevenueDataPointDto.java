package com.ste.restaurant.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevenueDataPointDto {
    private String label; // E.g., "2025-08-01" or "14:00"
    private BigDecimal revenue;
}
