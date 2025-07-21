package com.ste.restaurant.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlaceOrderDto {
    private String notes;

    private AddressDto address;

    private List<OrderItemDto> orderItems;
}
