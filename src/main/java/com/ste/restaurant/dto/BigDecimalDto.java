package com.ste.restaurant.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BigDecimalDto {
    @NotNull(message = "Value is required")
    @Positive(message = "Value must be positive")
    private BigDecimal decimal;
}
