package com.ste.restaurant.controller.impl;

import com.ste.restaurant.dto.CategoryDto;
import com.ste.restaurant.dto.CategoryDtoBasic;
import com.ste.restaurant.dto.FoodItemDto;
import com.ste.restaurant.service.impl.FoodItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RequestMapping("/rest/api/food/")
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class FoodItemController {
    @Autowired
    private FoodItemService foodItemService;

    @PostMapping(path = "/save-fooditem")
    public FoodItemDto saveFoodItem(@RequestBody FoodItemDto foodItem) {
        return foodItemService.saveFoodItem(foodItem);
    }

    @GetMapping(path = "/fooditem-list")
    public List<FoodItemDto> getAllFoodItems() {
        return foodItemService.getAllFoodItems();
    }

    @GetMapping(path = "/fooditems/by-name")
    public FoodItemDto getFoodItemByName(@RequestParam String name) {
        return foodItemService.getFoodItemByName(name);
    }

    @DeleteMapping(path = "/fooditem/{name}")
    public FoodItemDto deleteFoodItemByName(@PathVariable String name) {
        return foodItemService.deleteFoodItemByName(name);
    }

    @PutMapping(path = "/fooditem-update/{name}")
    public FoodItemDto updateFoodItemById(@PathVariable String name, @RequestBody FoodItemDto foodItem) {
        return foodItemService.updateFoodItemByName(name, foodItem);
    }

    // relation manyToMany foodItem -> category
    @GetMapping(path = "/fooditems/{foodName}/categories")
    public List<CategoryDtoBasic> getCategoriesOfFoodItem(@PathVariable String foodName) {
        return foodItemService.getCategories(foodName);
    }
}