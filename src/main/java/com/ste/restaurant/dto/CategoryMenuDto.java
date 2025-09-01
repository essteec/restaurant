package com.ste.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryMenuDto {
    private String categoryName;
    private Set<FoodItemMenuDto> foodItems = new HashSet<>();
}