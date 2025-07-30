package com.ste.restaurant.dto;

import com.ste.restaurant.entity.RequestType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallRequestDtoBasic {
    @NotNull(message = "Request type is required")
    private RequestType type;

    private String message;
}
