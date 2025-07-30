package com.ste.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WarningResponse<T> {
    private T data;
    private List<String> warnings;
}
