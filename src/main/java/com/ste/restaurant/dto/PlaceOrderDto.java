package com.ste.restaurant.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlaceOrderDto {
    private String notes;

    private Long addressId;

    private String tableNumber;

    private List<OrderItemDtoBasic> orderItems;
}
