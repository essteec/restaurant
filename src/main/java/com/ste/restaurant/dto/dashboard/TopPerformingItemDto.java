package com.ste.restaurant.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopPerformingItemDto {
    private String foodName;
    private long quantitySold;
    private BigDecimal totalRevenue;

    public TopPerformingItemDto add(TopPerformingItemDto item) {
        if (!Objects.equals(item.foodName, this.foodName)) {
            return this;
        }
        TopPerformingItemDto dto = new TopPerformingItemDto();
        dto.setFoodName(this.foodName);
        dto.setQuantitySold(this.quantitySold + item.getQuantitySold());
        dto.setTotalRevenue(this.totalRevenue.add(item.getTotalRevenue()));
        return dto;
    }
}
