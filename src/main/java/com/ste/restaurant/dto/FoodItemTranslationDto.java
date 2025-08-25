package com.ste.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodItemTranslationDto {
    
    @NotBlank
    @Size(max = 5)
    private String languageCode;

    @NotBlank
    private String name;
    
    private String description;
}
