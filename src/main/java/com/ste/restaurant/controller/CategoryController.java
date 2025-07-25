package com.ste.restaurant.controller;

import com.ste.restaurant.dto.CategoryDto;
import com.ste.restaurant.dto.CategoryDtoBasic;
import com.ste.restaurant.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RequestMapping("/rest/api/categories/")
@RestController
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // Category Management
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public CategoryDtoBasic saveCategory(@RequestBody CategoryDtoBasic category) {
        return categoryService.saveCategory(category);
    }

    @GetMapping
    public List<CategoryDto> getAllCategories() {
        return categoryService.listAllCategory();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{name}")
    public CategoryDto deleteCategoryByName(@PathVariable String name) {
        return categoryService.deleteCategoryByName(name);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{name}")
    public CategoryDto updateCategoryById(@PathVariable String name, @RequestBody CategoryDto category) {
        return categoryService.updateCategoryByName(name, category);
    }

    // relation with food item
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{categoryName}/food-items")
    public CategoryDto addFoodItemsToCategory(@PathVariable String categoryName, @RequestBody Set<String> foodNames) {
        return categoryService.addFoodItemsToCategory(categoryName, foodNames);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{categoryName}/food-items")
    public CategoryDto deleteFoodItemFromCategory(@PathVariable String categoryName, @RequestBody Set<String> foodNames) {
        return categoryService.removeFoodItemsFromCategory(categoryName, foodNames);
    }
}