package com.ste.restaurant.dto;

import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDto {
    private Long orderItemId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private String note;

    private FoodItemDto foodItem;
}
