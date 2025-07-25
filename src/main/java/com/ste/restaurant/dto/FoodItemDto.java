package com.ste.restaurant.dto;

import com.ste.restaurant.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodItemDto {
    private String foodName;

    private String image;

    private String description;

    private BigDecimal price;
}
