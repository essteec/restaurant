package com.ste.restaurant.entity.id;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
public class FoodItemTranslationId implements Serializable {
    private String languageCode;
    private Long foodItemId;
}
