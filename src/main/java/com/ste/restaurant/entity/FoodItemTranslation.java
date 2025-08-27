package com.ste.restaurant.entity;

import com.ste.restaurant.entity.id.FoodItemTranslationId;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@Table(name = "food_item_translations")
public class FoodItemTranslation {

    @EmbeddedId
    private FoodItemTranslationId foodItemTranslationId;

    private String name;

    private String description;

    @MapsId("foodItemId")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    public void setId(String languageCode, FoodItem foodItem) {
        FoodItemTranslationId id = new FoodItemTranslationId();
        id.setLanguageCode(languageCode);
        id.setFoodItemId(foodItem.getFoodId());
        this.setFoodItemTranslationId(id);
        this.setFoodItem(foodItem);
    }
}
