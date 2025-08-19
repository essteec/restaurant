package com.ste.restaurant.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusiestTableDto {
    private String tableNumber;
    private long orderCount;
}
