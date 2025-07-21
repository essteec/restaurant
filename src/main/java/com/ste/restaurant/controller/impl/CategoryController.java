package com.ste.restaurant.controller.impl;

import com.ste.restaurant.dto.CategoryDto;
import com.ste.restaurant.dto.CategoryDtoBasic;
import com.ste.restaurant.service.impl.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RequestMapping("/rest/api/category/")
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // Category Management
    @PostMapping(path = "/save-category")
    public CategoryDtoBasic saveCategory(@RequestBody CategoryDtoBasic category) {
        return categoryService.saveCategory(category);
    }

    @GetMapping(path = "/category-list")
    public List<CategoryDto> getAllCategories() {
        return categoryService.listAllCategory();
    }

    @DeleteMapping(path = "/category/{name}")
    public CategoryDto deleteCategoryByName(@PathVariable String name) {
        return categoryService.deleteCategoryByName(name);
    }

    @PutMapping(path = "/category-update/{name}")
    public CategoryDto updateCategoryById(@PathVariable String name, @RequestBody CategoryDto category) {
        return categoryService.updateCategoryByName(name, category);
    }

    // relation with food item
    @PutMapping(path = "/categories/{categoryName}/fooditems")
    public CategoryDto addFoodItemsToCategory(@PathVariable String categoryName, @RequestBody Set<String> foodNames) {
        return categoryService.addFoodItemsToCategory(categoryName, foodNames);
    }

    @DeleteMapping(path = "/categories/{categoryName}/fooditems")
    public CategoryDto deleteFoodItemFromCategory(@PathVariable String categoryName, @RequestBody Set<String> foodNames) {
        return categoryService.removeFoodItemsFromCategory(categoryName, foodNames);
    }
}