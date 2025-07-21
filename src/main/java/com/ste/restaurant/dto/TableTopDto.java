package com.ste.restaurant.dto;

import com.ste.restaurant.entity.TableStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableTopDto {
    private String tableNumber;

    private TableStatus tableStatus;
}
