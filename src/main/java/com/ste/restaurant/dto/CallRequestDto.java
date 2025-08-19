package com.ste.restaurant.dto;

import com.ste.restaurant.dto.userdto.UserDto;
import com.ste.restaurant.entity.RequestType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallRequestDto {
    private Long callRequestId;

    private RequestType type;

    private String message;

    private boolean active;

    private TableTopDto table;

    private UserDto customer;

    private LocalDateTime createdAt;
}
