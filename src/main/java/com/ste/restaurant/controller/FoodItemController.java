package com.ste.restaurant.controller;

import com.ste.restaurant.dto.CategoryDtoBasic;
import com.ste.restaurant.dto.FoodItemDto;
import com.ste.restaurant.dto.FoodItemMenuDto;
import com.ste.restaurant.dto.FoodItemTranslationDto;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.service.FoodItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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

    // CRUD language
    @GetMapping(path = "/{name}/languages")
    public List<FoodItemTranslationDto> getFoodItemTranslations(@PathVariable String name) {
        return foodItemService.getFoodItemTranslations(name);
    }

    @PostMapping(path = "/{name}/languages")
    public FoodItemTranslationDto addFoodItemTranslation(@PathVariable String name, @Valid @RequestBody FoodItemTranslationDto translationDto) {
        return foodItemService.addFoodItemTranslation(name, translationDto);
    }

    @DeleteMapping(path = "/{name}/languages/{lang}")
    public ResponseEntity<Boolean> deleteFoodItemTranslation(@PathVariable String name, @PathVariable String lang) {
        boolean deleted = foodItemService.deleteFoodItemTranslation(name, lang);
        return deleted
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @PutMapping(path = "/{name}/languages/{lang}")
    public FoodItemTranslationDto updateFoodItemTranslation(@PathVariable String name, @PathVariable String lang, @Valid @RequestBody FoodItemTranslationDto translationDto) {
        return foodItemService.updateFoodItemTranslation(name, lang, translationDto);
    }

    @GetMapping(path = "/search")
    public Page<FoodItemDto> searchFoodItems(@RequestParam String query, @PageableDefault(size = 20) Pageable pageable) {
        return foodItemService.searchFoodItems(query, pageable);
    }

    @PreAuthorize("permitAll()")
    @GetMapping(path = "/most-popular")
    public List<FoodItemMenuDto> getPopularFoodItems(@RequestHeader(value = "Accept-Language", defaultValue = "en") String langCode) {
        return foodItemService.getPopularFoodItems(langCode);
    }

    // public landing page
    @GetMapping(path = "/landing")
    @PreAuthorize("permitAll()")
    public List<FoodItemDto> getLandingPageFoodItems() {
        return foodItemService.getLandingPageFoodItems();
    }
}