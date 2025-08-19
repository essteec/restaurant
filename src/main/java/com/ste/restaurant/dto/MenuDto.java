package com.ste.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuDto {
    private String menuName;

    private String description;

    private boolean active;

    private Set<FoodItemDto> foodItems;
}
