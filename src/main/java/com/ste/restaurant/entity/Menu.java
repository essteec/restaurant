package com.ste.restaurant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Entity
@Data
@Table(name = "menus")
public class Menu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long menuId;

    @Column(name = "menu_name", nullable = false, unique = true)
    private String menuName;

    private String description;

    private boolean active = false;

    @Column(name = "food_items")
    @ManyToMany
    @JoinTable(name = "menu_food_item",
            joinColumns = @JoinColumn(name = "menu_id"),
            inverseJoinColumns = @JoinColumn(name = "food_item_id"))
    private Set<FoodItem> foodItems;
}
