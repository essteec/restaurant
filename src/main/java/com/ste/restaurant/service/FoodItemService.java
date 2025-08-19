package com.ste.restaurant.service;

import com.ste.restaurant.dto.CategoryDtoBasic;
import com.ste.restaurant.dto.FoodItemDto;
import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.exception.AlreadyExistsException;
import com.ste.restaurant.exception.ImageProcessingException;
import com.ste.restaurant.exception.NotFoundException;
import com.ste.restaurant.exception.NullValueException;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.FoodItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FoodItemService {

    private final FoodItemRepository foodItemRepository;
    private final OrderMapper orderMapper;
    private final String uploadDir;

    public FoodItemService(FoodItemRepository foodItemRepo, OrderMapper orderMapper,
                           @Value("${app.image.upload-dir}") String uploadDir) {
        this.foodItemRepository = foodItemRepo;
        this.orderMapper = orderMapper;
        this.uploadDir = uploadDir;
    }

    public FoodItemDto saveFoodItem(FoodItemDto foodItem) {
        if (foodItem.getFoodName() == null) {
            throw new NullValueException("Food", "name");
        }
        if (foodItemRepository.existsFoodItemByFoodName(foodItem.getFoodName())) {
            throw new AlreadyExistsException("Food", foodItem.getFoodName());
        }

        FoodItem savedFood = foodItemRepository.save(orderMapper.foodItemDtoToFoodItem(foodItem));
        return orderMapper.foodItemToFoodItemDto(savedFood);
    }

    public Page<FoodItemDto> getAllFoodItems(Pageable pageable) {
        Page<FoodItem> foodItems = foodItemRepository.findAll(pageable);
        return foodItems.map(orderMapper::foodItemToFoodItemDto);
    }

    public FoodItemDto getFoodItemByName(String name) {
        FoodItem foodItem = foodItemRepository.findByFoodName(name)
                .orElseThrow(() -> new NotFoundException("Food", name));

        return orderMapper.foodItemToFoodItemDto(foodItem);
    }

    public FoodItemDto deleteFoodItemByName(String name) {
        FoodItem food = foodItemRepository.findByFoodName(name)
                        .orElseThrow(() -> new NotFoundException("Food", name));
        foodItemRepository.delete(food);
        return orderMapper.foodItemToFoodItemDto(food);
    }

    public FoodItemDto updateFoodItemByName(String name, FoodItemDto foodItem) {
        FoodItem foodItemOld = foodItemRepository.findByFoodName(name)
                .orElseThrow(() -> new NotFoundException("Food", name));

        if (foodItem.getFoodName() != null && !foodItem.getFoodName().equals(name)) {
            if (foodItemRepository.findByFoodName(foodItem.getFoodName()).isPresent()) {
                throw new AlreadyExistsException("Food", foodItem.getFoodName());
            }
        }
        orderMapper.updateFoodItemFromDto(foodItem, foodItemOld);

        FoodItem savedFoodItem = foodItemRepository.save(foodItemOld);

        return orderMapper.foodItemToFoodItemDto(savedFoodItem);
    }

    public Set<CategoryDtoBasic> getCategories(String name) {
        Optional<FoodItem> foodItemOpt = foodItemRepository.findByFoodName(name);
        if (foodItemOpt.isEmpty()) {
            return Collections.emptySet();
        }
        FoodItem foodItem = foodItemOpt.get();

        return orderMapper.categoriesToCategoryDtoBasics(foodItem.getCategories());
    }

    // process image
    public FoodItemDto addImageToFood(String name, MultipartFile imageFile) {
        String originalFilename = imageFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new NullValueException("Image", "filename");
        }

        FoodItem food = foodItemRepository.findByFoodName(name)
                .orElseThrow(() -> new NotFoundException("Food", name));
        String oldImage = food.getImage();

        try {
            String extension = ServiceUtil.getFileExtension(originalFilename);
            if (extension.isEmpty()) throw new NullValueException("Image", "extension");
            String fileName = name.replaceAll("\\s+", "-") + LocalDateTime.now().format(DateTimeFormatter.ofPattern("_yyyy-MM-dd_HH-mm-ss.")) +
                    extension;
            java.io.File dirFile = new java.io.File(uploadDir);
            if (!dirFile.exists()) {
                if (!dirFile.mkdirs()) {
                    throw new ImageProcessingException("Image folder not created!");
                }
            }
            String filePath = uploadDir + fileName;

            if (!ServiceUtil.cropAndResizeToSquare(imageFile, filePath, 600)){
                throw new ImageProcessingException("Image crop and resizing failed");
            }

            // delete old image
            if (oldImage != null) {
                deleteImageFile(food);
            }

            food.setImage(fileName);

            FoodItem savedFood = foodItemRepository.save(food);
            return orderMapper.foodItemToFoodItemDto(savedFood);

        } catch (Exception e) {
            throw new ImageProcessingException(e.getMessage());
        }
    }

    public Boolean deleteImageFile(String name) {
        FoodItem food = foodItemRepository.findByFoodName(name)
                .orElseThrow(() -> new NotFoundException("Food", name));

        return deleteImageFile(food);
    }

    public Boolean deleteImageFile(FoodItem food) {
        String oldImage = food.getImage();
        if (oldImage != null) {
            File oldImageFile = new File(uploadDir + oldImage);
            if (oldImageFile.exists() && oldImageFile.delete()) {  // after delete operations
                food.setImage(null);
                foodItemRepository.save(food);
                return true;
            }
        }
        return false;
    }
}