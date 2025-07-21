package com.ste.restaurant.service.impl;

import com.ste.restaurant.dto.CategoryDto;
import com.ste.restaurant.dto.CategoryDtoBasic;
import com.ste.restaurant.dto.FoodItemDto;
import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.repository.CategoryRepository;
import com.ste.restaurant.repository.FoodItemRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CategoryService {

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    public CategoryDtoBasic saveCategory(CategoryDtoBasic category) {
        if (category.getCategoryName() == null) {
            return null;
        }
        if (categoryRepository.existsCategoryByCategoryName(category.getCategoryName())) {
            return null;  // already have same name
        }

        Category categorySave = new Category();
        BeanUtils.copyProperties(category, categorySave);
        categoryRepository.save(categorySave);
        return category;
    }

    public List<CategoryDto> listAllCategory() {
        List<Category> categories = categoryRepository.findAll();
        List<CategoryDto> categoryDtos = new ArrayList<>();

        for (Category category : categories) {
            CategoryDto categoryDto = new CategoryDto();
            BeanUtils.copyProperties(category, categoryDto);

            categoryDto.setFoods(new HashSet<>());
            for (FoodItem food : category.getFoodItems()) {
                FoodItemDto foodItemDto = new FoodItemDto();
                BeanUtils.copyProperties(food, foodItemDto);
                categoryDto.getFoods().add(foodItemDto);
            }
            categoryDtos.add(categoryDto);
        }
        return categoryDtos;
    }

    public CategoryDto deleteCategoryByName(String name) {
        Optional<Category> categoryOpt = categoryRepository.findByCategoryName(name);
        if (categoryOpt.isEmpty()) {
            return null;  // exception
        }
        Category category = categoryOpt.get();

        CategoryDto categoryDto = new CategoryDto();
        BeanUtils.copyProperties(category, categoryDto);
        categoryDto.setFoods(new HashSet<>());
        for (FoodItem food : category.getFoodItems()) {
            FoodItemDto foodItemDto = new FoodItemDto();
            BeanUtils.copyProperties(food, foodItemDto);
            categoryDto.getFoods().add(foodItemDto);
        }
        categoryRepository.delete(category);
        return categoryDto;
    }

    public CategoryDto updateCategoryByName(String name, CategoryDto category) {
        Optional<Category> categoryOpt = categoryRepository.findByCategoryName(name);
        if (categoryOpt.isEmpty()) {
            return null;
        }
        Category categoryOld = categoryOpt.get();

        if (category.getCategoryName() != null && !category.getCategoryName().equals(name)) {
            if (categoryRepository.findByCategoryName(category.getCategoryName()).isPresent()) {
                return null;
            }
        }
        BeanUtils.copyProperties(category, categoryOld,
                ServiceUtil.getNullPropertyNames(category));

        Category savedCategory = categoryRepository.save(categoryOld);

        CategoryDto categoryResponse = new CategoryDto();
        BeanUtils.copyProperties(savedCategory, categoryResponse);

        categoryResponse.setFoods(new HashSet<>());
        if (savedCategory.getFoodItems() != null) {
            for (FoodItem food : savedCategory.getFoodItems()) {
                FoodItemDto foodItemDto = new FoodItemDto();
                BeanUtils.copyProperties(food, foodItemDto);
                categoryResponse.getFoods().add(foodItemDto);
            }
        }
        return categoryResponse;
    }

    // relation with food item
    public CategoryDto addFoodItemsToCategory(String categoryName, Set<String> foodNames) {
        Optional<Category> categoryOpt = categoryRepository.findByCategoryName(categoryName);
        if (categoryOpt.isEmpty()) {
            return null;
        }
        Category category = categoryOpt.get();

        Set<FoodItem> foodItems = new HashSet<>();
        for (String foodName : foodNames) {
            Optional<FoodItem> foodItem = foodItemRepository.findByFoodName(foodName);
            if (foodItem.isEmpty()) {
                continue;
            }
            foodItems.add(foodItem.get());
        }
        category.getFoodItems().addAll(foodItems);
        Category savedCategory = categoryRepository.save(category);

        CategoryDto categoryResponse = new CategoryDto();
        BeanUtils.copyProperties(savedCategory, categoryResponse);
        categoryResponse.setFoods(new HashSet<>());
        if (savedCategory.getFoodItems() != null) {
            for (FoodItem food : savedCategory.getFoodItems()) {
                FoodItemDto foodItemDto = new FoodItemDto();
                BeanUtils.copyProperties(food, foodItemDto);
                categoryResponse.getFoods().add(foodItemDto);
            }
        }
        return categoryResponse;
    }

    public CategoryDto removeFoodItemsFromCategory(String categoryName, Set<String> foodNames) {
        Optional<Category> categoryOpt = categoryRepository.findByCategoryName(categoryName);
        if (categoryOpt.isEmpty()) {
            return null;
        }
        Category category = categoryOpt.get();

        Set<FoodItem> foodItems = new HashSet<>();
        for (String foodName : foodNames) {
            Optional<FoodItem> foodItem = foodItemRepository.findByFoodName(foodName);
            if (foodItem.isEmpty()) {
                continue;
            }
            foodItems.add(foodItem.get());
        }
        category.getFoodItems().removeAll(foodItems);
        Category savedCategory = categoryRepository.save(category);

        CategoryDto categoryResponse = new CategoryDto();
        BeanUtils.copyProperties(savedCategory, categoryResponse);
        categoryResponse.setFoods(new HashSet<>());
        if (savedCategory.getFoodItems() != null) {
            for (FoodItem food : savedCategory.getFoodItems()) {
                FoodItemDto foodItemDto = new FoodItemDto();
                BeanUtils.copyProperties(food, foodItemDto);
                categoryResponse.getFoods().add(foodItemDto);
            }
        }
        return categoryResponse;
    }
}
