package com.ste.restaurant.controller;

import com.ste.restaurant.dto.CategoryDtoBasic;
import com.ste.restaurant.dto.FoodItemDto;
import com.ste.restaurant.service.FoodItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/rest/api/food-items")
@RestController
public class FoodItemController {

    private final FoodItemService foodItemService;

    public FoodItemController(FoodItemService foodItemService) {
        this.foodItemService = foodItemService;
    }

    @PostMapping
    public FoodItemDto saveFoodItem(@Valid @RequestBody FoodItemDto foodItem) {
        return foodItemService.saveFoodItem(foodItem);
    }

    @GetMapping
    public Page<FoodItemDto> getAllFoodItems(@PageableDefault(size = 20) Pageable pageable) {
        return foodItemService.getAllFoodItems(pageable);
    }

    @GetMapping(path = "/{name}")
    public FoodItemDto getFoodItemByName(@PathVariable String name) {
        return foodItemService.getFoodItemByName(name);
    }

    @DeleteMapping(path = "/{name}")
    public FoodItemDto deleteFoodItemByName(@PathVariable String name) {
        return foodItemService.deleteFoodItemByName(name);
    }

    @PutMapping(path = "/{name}")
    public FoodItemDto updateFoodItemById(@PathVariable String name, @Valid @RequestBody FoodItemDto foodItem) {
        return foodItemService.updateFoodItemByName(name, foodItem);
    }

    // relation manyToMany foodItem -> category
    @PreAuthorize("isAuthenticated()")
    @GetMapping(path = "/{name}/categories")
    public Set<CategoryDtoBasic> getCategoriesOfFoodItem(@PathVariable String name) {
        return foodItemService.getCategories(name);
    }

    // get image
    @PostMapping(path = "/{name}/image")
    public FoodItemDto uploadFoodImage(@PathVariable String name, @RequestParam("image") MultipartFile imageFile) {
        return foodItemService.addImageToFood(name, imageFile);
    }

    @DeleteMapping(path = "/{name}/image")
    public ResponseEntity<Boolean> deleteFoodImage(@PathVariable String name) {
        boolean deleted = foodItemService.deleteImageFile(name);
        return deleted
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}