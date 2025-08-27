package com.ste.restaurant.service;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.dto.common.StringsDto;
import com.ste.restaurant.dto.common.WarningResponse;
import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.CategoryTranslation;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.exception.AlreadyExistsException;
import com.ste.restaurant.exception.InvalidValueException;
import com.ste.restaurant.exception.NotFoundException;
import com.ste.restaurant.exception.NullValueException;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.CategoryRepository;
import com.ste.restaurant.repository.CategoryTranslationRepository;
import com.ste.restaurant.repository.FoodItemRepository;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryTranslationRepository categoryTranslationRepository;
    private final FoodItemRepository foodItemRepository;
    private final OrderMapper orderMapper;

    public CategoryService(CategoryRepository categoryRepo, FoodItemRepository foodItemRepo, 
                           CategoryTranslationRepository categoryTranslationRepo, OrderMapper orderMapper) {
        this.categoryRepository = categoryRepo;
        this.foodItemRepository = foodItemRepo;
        this.categoryTranslationRepository = categoryTranslationRepo;
        this.orderMapper = orderMapper;
    }

    public CategoryDtoBasic saveCategory(CategoryDtoBasic category) {
        if (category.getCategoryName() == null) {
            throw new NullValueException("Category", "name");
        }
        if (categoryRepository.existsCategoryByCategoryName(category.getCategoryName())) {
            throw new AlreadyExistsException("Category", category.getCategoryName());
        }

        Category savedCategory = categoryRepository.save(orderMapper.categoryDtoBasicToCategory(category));
        return orderMapper.categoryToCategoryDtoBasic(savedCategory);
    }

    public Page<CategoryDto> listAllCategory(Pageable pageable) {
        Page<Category> categories = categoryRepository.findAll(pageable);
        return categories.map(orderMapper::categoryToCategoryDto);
    }

    public CategoryDto deleteCategoryByName(String name) {
        Category category = categoryRepository.findByCategoryName(name)
                .orElseThrow(() -> new NotFoundException("Category", name));

        CategoryDto categoryDto = orderMapper.categoryToCategoryDto(category);
        categoryDto.setFoodItems(new HashSet<>());
        for (FoodItem food : category.getFoodItems()) {
            categoryDto.getFoodItems().add(
                    orderMapper.foodItemToFoodItemDto(food)
            );
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
        orderMapper.updateCategoryFromDto(category, categoryOld);

        Category savedCategory = categoryRepository.save(categoryOld);

        return orderMapper.categoryToCategoryDto(savedCategory);
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

        CategoryDto categoryResponse = orderMapper.categoryToCategoryDto(savedCategory);
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

        CategoryDto categoryResponse = orderMapper.categoryToCategoryDto(savedCategory);
        return new WarningResponse<>(categoryResponse, failedNames);
    }

    public List<CategoryTranslationDto> getCategoryTranslations(String name) {
        Category category = categoryRepository.findByCategoryName(name)
                .orElseThrow(() -> new NotFoundException("Category", name));

        return orderMapper.categoryTranslationsToCategoryTranslationDtos(
                new ArrayList<>(category.getTranslations().values())
        );
    }

    public CategoryTranslationDto addCategoryTranslation(String name, CategoryTranslationDto translationDto) {
        Category category = categoryRepository.findByCategoryName(name)
                .orElseThrow(() -> new NotFoundException("Category", name));

        CategoryTranslation translation = orderMapper.categoryTranslationDtoToCategoryTranslation(translationDto);
        translation.setCategory(category);
        translation.setId(translationDto.getLanguageCode(), category);
        categoryTranslationRepository.save(translation);

        return orderMapper.categoryTranslationToCategoryTranslationDto(translation);
    }

    public boolean deleteCategoryTranslation(String name, String langCode) {
        Category category = categoryRepository.findByCategoryName(name)
                .orElseThrow(() -> new NotFoundException("Category", name));

        if (!category.getTranslations().containsKey(langCode)) {
            throw new InvalidValueException("Category", "language code", langCode);
        }

        CategoryTranslation translation = category.getTranslations().get(langCode);
        if (translation == null) {
            throw new NotFoundException("CategoryTranslation", langCode);
        }
        
        category.getTranslations().remove(langCode);
        categoryRepository.save(category);
        return true;
    }

    public CategoryTranslationDto updateCategoryTranslation(String name, String langCode, 
                                                            CategoryTranslationDto translationDto) {
        Category category = categoryRepository.findByCategoryName(name)
                .orElseThrow(() -> new NotFoundException("Category", name));

        if (!category.getTranslations().containsKey(langCode)) {
            throw new InvalidValueException("Category", "language code", langCode);
        }

        CategoryTranslation translation = category.getTranslations().get(langCode);
        if (translation == null) {
            throw new NotFoundException("CategoryTranslation", langCode);
        }

        orderMapper.updateCategoryTranslationFromDto(translationDto, translation);
        categoryTranslationRepository.save(translation);
        return orderMapper.categoryTranslationToCategoryTranslationDto(translation);
    }
}
