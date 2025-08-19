package com.ste.restaurant.controller;

import com.ste.restaurant.dto.CategoryDto;
import com.ste.restaurant.dto.CategoryDtoBasic;
import com.ste.restaurant.dto.common.StringsDto;
import com.ste.restaurant.dto.common.WarningResponse;
import com.ste.restaurant.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RequestMapping("/rest/api/categories")
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // Category Management
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public CategoryDtoBasic saveCategory(@Valid @RequestBody CategoryDtoBasic category) {
        return categoryService.saveCategory(category);
    }

    @GetMapping
    public Page<CategoryDto> getAllCategories(
            @PageableDefault(size = 24) Pageable pageable) {
        return categoryService.listAllCategory(pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{name}")
    public CategoryDto deleteCategoryByName(@PathVariable String name) {
        return categoryService.deleteCategoryByName(name);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{name}")
    public CategoryDto updateCategoryById(@PathVariable String name, @Valid @RequestBody CategoryDtoBasic category) {
        return categoryService.updateCategoryByName(name, category);
    }

    // relation with food item
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{name}/food-items")
    public WarningResponse<CategoryDto> addFoodItemsToCategory(@PathVariable String name, @Valid @RequestBody StringsDto foodNames) {
        return categoryService.addFoodItemsToCategory(name, foodNames);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{name}/food-items")
    public WarningResponse<CategoryDto> deleteFoodItemFromCategory(@PathVariable String name, @Valid @RequestBody StringsDto foodNames) {
        return categoryService.removeFoodItemsFromCategory(name, foodNames);
    }
}