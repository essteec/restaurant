package com.ste.restaurant.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Data
@Table(name = "food_items")
public class FoodItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long foodId;

    @Column(nullable = false, unique = true)
    private String foodName;

    private String image;

    private String description;

    private BigDecimal price;

    @OneToMany(mappedBy = "foodItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @MapKey(name = "languageCode")
    private Map<String, FoodItemTranslation> translations = new HashMap<>();

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToMany(mappedBy = "foodItems")
    private Set<Category> categories = new HashSet<>();

    public void addTranslation(FoodItemTranslation translation) {
        translation.setFoodItem(this);
        this.translations.put(translation.getLanguageCode(), translation);
    }
}
