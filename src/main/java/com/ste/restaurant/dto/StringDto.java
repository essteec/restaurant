package com.ste.restaurant.dto;

import com.ste.restaurant.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StringDto {

    @NotBlank(message = "Name is required")
    private String name;
}
