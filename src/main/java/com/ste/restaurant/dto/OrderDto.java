package com.ste.restaurant.dto;

import com.ste.restaurant.dto.userdto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
    private Long orderId;

    private LocalDateTime orderTime;

    private String status;

    private BigDecimal totalPrice;

    private String notes;

    private UserDto customer;

    private AddressDto address;

    private List<OrderItemDto> orderItems;

    private TableTopDto table;
}
