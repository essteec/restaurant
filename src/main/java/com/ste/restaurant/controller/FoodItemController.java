package com.ste.restaurant.controller;

import com.ste.restaurant.dto.CategoryDtoBasic;
import com.ste.restaurant.dto.FoodItemDto;
import com.ste.restaurant.service.FoodItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/rest/api/food-items/")
@RestController
public class FoodItemController {
    @Autowired
    private FoodItemService foodItemService;

    @PostMapping
    public FoodItemDto saveFoodItem(@RequestBody FoodItemDto foodItem) {
        return foodItemService.saveFoodItem(foodItem);
    }

    @GetMapping
    public List<FoodItemDto> getAllFoodItems() {
        return foodItemService.getAllFoodItems();
    }

    @GetMapping(path = "/by-name")
    public FoodItemDto getFoodItemByName(@RequestParam String name) {
        return foodItemService.getFoodItemByName(name);
    }

    @DeleteMapping(path = "/{name}")
    public FoodItemDto deleteFoodItemByName(@PathVariable String name) {
        return foodItemService.deleteFoodItemByName(name);
    }

    @PutMapping(path = "/{name}")
    public FoodItemDto updateFoodItemById(@PathVariable String name, @RequestBody FoodItemDto foodItem) {
        return foodItemService.updateFoodItemByName(name, foodItem);
    }

    // relation manyToMany foodItem -> category
    @PreAuthorize("permitAll()")
    @GetMapping(path = "/{name}/categories")
    public List<CategoryDtoBasic> getCategoriesOfFoodItem(@PathVariable String name) {
        return foodItemService.getCategories(name);
    }

    // get image
    @PostMapping(path = "/{name}/image")
    public FoodItemDto uploadFoodImage(@PathVariable String name, @RequestParam("image") MultipartFile imageFile) {
        return foodItemService.addImageToFood(name, imageFile);
    }
}