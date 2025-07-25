package com.ste.restaurant.service;

import com.ste.restaurant.dto.CategoryDtoBasic;
import com.ste.restaurant.dto.FoodItemDto;
import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.repository.CategoryRepository;
import com.ste.restaurant.repository.FoodItemRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FoodItemService {

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public FoodItemDto saveFoodItem(FoodItemDto foodItem) {
        if (foodItem.getFoodName() == null) {
            return null;
        }
        if (foodItemRepository.existsFoodItemByFoodName(foodItem.getFoodName())) {
            return null;
        }
        FoodItem foodItemSave = new FoodItem();
        BeanUtils.copyProperties(foodItem, foodItemSave);
        foodItemRepository.save(foodItemSave);
        return foodItem;
    }

    public List<FoodItemDto> getAllFoodItems() {
        List<FoodItem> foodItems = foodItemRepository.findAll();
        List<FoodItemDto> foodItemDtos = new ArrayList<>();
        for (FoodItem foodItem : foodItems) {
            FoodItemDto foodItemDto = new FoodItemDto();
            BeanUtils.copyProperties(foodItem, foodItemDto);
            foodItemDtos.add(foodItemDto);
        }
        return foodItemDtos;
    }

    public FoodItemDto getFoodItemByName(String name) {
        Optional<FoodItem> foodItem = foodItemRepository.findByFoodName(name);
        if (foodItem.isEmpty()) {
            return null;  // exception
        }
        FoodItemDto foodItemDto = new FoodItemDto();
        BeanUtils.copyProperties(foodItem.get(), foodItemDto);
        return foodItemDto;
    }

    public FoodItemDto deleteFoodItemByName(String name) {
        Optional<FoodItem> foodItem = foodItemRepository.findByFoodName(name);
        if (foodItem.isEmpty()) {
            return null;
        }
        FoodItem food = foodItem.get();
        foodItemRepository.delete(food);
        FoodItemDto foodItemDto = new FoodItemDto();
        BeanUtils.copyProperties(foodItem.get(), foodItemDto);
        return foodItemDto;
    }

    public FoodItemDto updateFoodItemByName(String name, FoodItemDto foodItem) {
        Optional<FoodItem> foodItemOpt = foodItemRepository.findByFoodName(name);
        if (foodItemOpt.isEmpty()) {
            return null;
        }
        FoodItem foodItemOld = foodItemOpt.get();
        if (foodItem.getFoodName() != null && !foodItem.getFoodName().equals(name)) {
            if (foodItemRepository.findByFoodName(foodItem.getFoodName()).isPresent()) {
                return null;  // exception
            }
        }
        BeanUtils.copyProperties(foodItem, foodItemOld,
                ServiceUtil.getNullPropertyNames(foodItem));

        FoodItem savedFoodItem = foodItemRepository.save(foodItemOld);

        FoodItemDto foodItemResponse = new FoodItemDto();
        BeanUtils.copyProperties(savedFoodItem, foodItemResponse);
        return foodItemResponse;
    }

    public List<CategoryDtoBasic> getCategories(String name) {
        Optional<FoodItem> foodItemOpt = foodItemRepository.findByFoodName(name);
        if (foodItemOpt.isEmpty()) {
            return Collections.emptyList();
        }
        FoodItem foodItem = foodItemOpt.get();

        List<Category> categories = categoryRepository.getCategoriesByFoodItems(Set.of(foodItem));
        List<CategoryDtoBasic> categoryDtos = new ArrayList<>();
        for (Category category : categories) {
            CategoryDtoBasic categoryDto = new CategoryDtoBasic();
            BeanUtils.copyProperties(category, categoryDto);
            categoryDtos.add(categoryDto);
        }
        return categoryDtos;
    }

    // process image
    public FoodItemDto addImageToFood(String name, MultipartFile imageFile) {
        FoodItem food = foodItemRepository.findByFoodName(name)
                .orElseThrow(() -> new RuntimeException("Food not found"));
        String oldImage = food.getImage();

        try {
            String fileName = name.replaceAll("\\s+", "-") + LocalDateTime.now().format(DateTimeFormatter.ofPattern("_yyyy-MM-dd_HH-mm-ss.")) +
                    ServiceUtil.getFileExtension(imageFile.getOriginalFilename());
            String dir = "src/main/resources/static/images/";
            java.io.File dirFile = new java.io.File(dir);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
            String filePath = dir + fileName;

            if (!ServiceUtil.cropAndResizeToSquare(imageFile, filePath, 600)){
                throw new  RuntimeException("Image processing failed");
            }

            // delete old image
            if (oldImage != null && !oldImage.equals(imageFile.getOriginalFilename())) {
                deleteImageFile(food);
            }

            food.setImage(fileName);

            FoodItem savedFood = foodItemRepository.save(food);

            // update food item
            FoodItemDto foodDto = new FoodItemDto();
            BeanUtils.copyProperties(savedFood, foodDto);

            return foodDto;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteImageFile(String name) {
        FoodItem food = foodItemRepository.findByFoodName(name)
                .orElseThrow(() -> new RuntimeException("Food not found"));

        return deleteImageFile(food);
    }

    public boolean deleteImageFile(FoodItem food) {
        String oldImage = food.getImage();
        if (oldImage != null) {
            java.io.File oldImageFile = new java.io.File(
                    "src/main/resources/static/images/" + oldImage);
            if (oldImageFile.exists()) {
                if (oldImageFile.delete()) {  // after delete operations
                    food.setImage(null);
                    foodItemRepository.save(food);
                    return true;
                }
                else {  // failed to delete
                    return false;
                }
            }
        }
        return false;
    }
}