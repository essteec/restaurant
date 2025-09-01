package com.ste.restaurant.dto;

import com.ste.restaurant.entity.enums.RequestType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallRequestDtoBasic {
    @NotNull(message = "Request type is required")
    private RequestType type;

    private String message;
}
