package com.ste.restaurant.service;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.exception.AlreadyExistsException;
import com.ste.restaurant.exception.NotFoundException;
import com.ste.restaurant.exception.NullValueException;
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
            throw new NullValueException("Category", "name");
        }
        if (categoryRepository.existsCategoryByCategoryName(category.getCategoryName())) {
            throw new AlreadyExistsException("Category", category.getCategoryName());
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
        Category category = categoryRepository.findByCategoryName(name)
                .orElseThrow(() -> new NotFoundException("Category", name));

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

    public CategoryDto updateCategoryByName(String name, CategoryDtoBasic category) {
        Category categoryOld = categoryRepository.findByCategoryName(name)
                .orElseThrow(() -> new NotFoundException("Category", name));

        if (category.getCategoryName() != null && !category.getCategoryName().equals(name)) {
            if (categoryRepository.findByCategoryName(category.getCategoryName()).isPresent()) {
                throw new AlreadyExistsException("Category", category.getCategoryName());
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
    public WarningResponse<CategoryDto> addFoodItemsToCategory(String categoryName, StringsDto foodNamesDto) {
        Set<String> foodNames = foodNamesDto.getNames();

        Category category = categoryRepository.findByCategoryName(categoryName)
                .orElseThrow(() -> new NotFoundException("Category", categoryName));

        List<String> failedNames =  new ArrayList<>();
        for (String foodName : foodNames) {
            Optional<FoodItem> foodItem = foodItemRepository.findByFoodName(foodName);
            if (foodItem.isEmpty()) {
                failedNames.add(foodName);
            } else {
                category.getFoodItems().add(foodItem.get());
            }
        }
        Category savedCategory = categoryRepository.save(category);

        CategoryDto categoryResponse = new CategoryDto();
        BeanUtils.copyProperties(savedCategory, categoryResponse);
        categoryResponse.setFoods(new HashSet<>());

        for (FoodItem food : savedCategory.getFoodItems()) {
            FoodItemDto foodItemDto = new FoodItemDto();
            BeanUtils.copyProperties(food, foodItemDto);
            categoryResponse.getFoods().add(foodItemDto);
        }
        return new WarningResponse<>(categoryResponse, failedNames);
    }

    public WarningResponse<CategoryDto> removeFoodItemsFromCategory(String categoryName, StringsDto foodNamesDto) {
        Set<String> foodNames = foodNamesDto.getNames();

        Category category = categoryRepository.findByCategoryName(categoryName)
                .orElseThrow(() -> new NotFoundException("Category", categoryName));

        List<String> failedNames = new  ArrayList<>();
        for (String foodName : foodNames) {
            Optional<FoodItem> foodItem = foodItemRepository.findByFoodName(foodName);
            if (foodItem.isEmpty()) {
                failedNames.add(foodName);
            } else {
                category.getFoodItems().remove(foodItem.get());
            }
        }

        Category savedCategory = categoryRepository.save(category);

        CategoryDto categoryResponse = new CategoryDto();
        BeanUtils.copyProperties(savedCategory, categoryResponse);
        categoryResponse.setFoods(new HashSet<>());
        for (FoodItem food : savedCategory.getFoodItems()) {
            FoodItemDto foodItemDto = new FoodItemDto();
            BeanUtils.copyProperties(food, foodItemDto);
            categoryResponse.getFoods().add(foodItemDto);
        }
        return new WarningResponse<>(categoryResponse, failedNames);
    }
}
