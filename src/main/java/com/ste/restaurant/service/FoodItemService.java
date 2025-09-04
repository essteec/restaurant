package com.ste.restaurant.service;

import com.ste.restaurant.dto.CategoryDtoBasic;
import com.ste.restaurant.dto.FoodItemDto;
import com.ste.restaurant.dto.FoodItemMenuDto;
import com.ste.restaurant.dto.FoodItemTranslationDto;
import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.entity.FoodItemTranslation;
import com.ste.restaurant.exception.AlreadyExistsException;
import com.ste.restaurant.exception.ImageProcessingException;
import com.ste.restaurant.exception.InvalidValueException;
import com.ste.restaurant.exception.NotFoundException;
import com.ste.restaurant.exception.NullValueException;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.FoodItemRepository;
import com.ste.restaurant.repository.FoodItemTranslationRepository;

import io.micrometer.core.instrument.config.validate.Validated.Invalid;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FoodItemService {

    private final FoodItemRepository foodItemRepository;
    private final FoodItemTranslationRepository foodItemTranslationRepository;
    private final LanguageService languageService;
    private final OrderMapper orderMapper;
    private final String uploadDir;

    public FoodItemService(FoodItemRepository foodItemRepo,
                           FoodItemTranslationRepository foodItemTranslationRepo,
                           LanguageService languageService, OrderMapper orderMapper,
                           @Value("${app.image.upload-dir}") String uploadDir) {
        this.foodItemRepository = foodItemRepo;
        this.foodItemTranslationRepository = foodItemTranslationRepo;
        this.languageService = languageService;
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
            String fileName = name.replaceAll("\s+", "-") + LocalDateTime.now().format(DateTimeFormatter.ofPattern("_yyyy-MM-dd_HH-mm-ss.")) +
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

    public List<FoodItemTranslationDto> getFoodItemTranslations(String name) {
        FoodItem food = foodItemRepository.findByFoodName(name)
                .orElseThrow(() -> new NotFoundException("Food", name));

        
        return orderMapper.foodItemTranslationsToFoodItemTranslationDtos(
                new ArrayList<>(food.getTranslations().values())
        );
    }

    public FoodItemTranslationDto addFoodItemTranslation(String name, FoodItemTranslationDto translationDto) {
        FoodItem food = foodItemRepository.findByFoodName(name)
                .orElseThrow(() -> new NotFoundException("Food", name));

        FoodItemTranslation translation = orderMapper.foodItemTranslationDtoToFoodItemTranslation(translationDto);
        translation.setFoodItem(food);  
        translation.setId(translationDto.getLanguageCode(), food);
        foodItemTranslationRepository.save(translation);

        return orderMapper.foodItemTranslationToFoodItemTranslationDto(translation);
    }

    public boolean deleteFoodItemTranslation(String name, String langCode) {
        FoodItem food = foodItemRepository.findByFoodName(name)
                .orElseThrow(() -> new NotFoundException("Food", name));

        if (!food.getTranslations().containsKey(langCode)) {
            throw new InvalidValueException("Food", "language code", langCode);
        }

        FoodItemTranslation translation = food.getTranslations().get(langCode);
        if (translation == null) {
            throw new NotFoundException("FoodItemTranslation", langCode);
        }

        food.getTranslations().remove(langCode);
        foodItemRepository.save(food);
        return true;
    }

    public FoodItemTranslationDto updateFoodItemTranslation(String name, String langCode,
                                                            FoodItemTranslationDto translationDto) {
        FoodItem food = foodItemRepository.findByFoodName(name)
                .orElseThrow(() -> new NotFoundException("Food", name));

        if (!food.getTranslations().containsKey(langCode)) {
            throw new InvalidValueException("FoodItemTranslation", "language code", langCode);
        }

        FoodItemTranslation translation = food.getTranslations().get(langCode);
        if (translation == null) {
            throw new NotFoundException("FoodItemTranslation", langCode);
        }

        orderMapper.updateFoodItemTranslationFromDto(translationDto, translation);
        foodItemTranslationRepository.save(translation);
        return orderMapper.foodItemTranslationToFoodItemTranslationDto(translation);
    }

    public Page<FoodItemDto> searchFoodItems(String query, Pageable pageable) {
        Page<FoodItem> foodItems = foodItemRepository.findAllByFoodNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query, pageable);
        return foodItems.map(orderMapper::foodItemToFoodItemDto);
    }
    
    public List<FoodItemDto> getLandingPageFoodItems() {
        List<FoodItem> foodItems = foodItemRepository.findPopularFoodItems(PageRequest.of(0, 6));
        return orderMapper.foodItemsToFoodItemDtos(
            foodItems.stream().filter(foodItem -> foodItem.getImage() != null).toList()
        );
    }

    public List<FoodItemMenuDto> getPopularFoodItems(String langCode) {
        // Validate
        if (languageService.countDistinctLanguages() > 0 && !languageService.existsByLanguageCode(langCode)) {
            langCode = "en";
        }

        Set<FoodItem> foodItems = foodItemRepository.findPopularFoodItems(PageRequest.of(0, 8)).stream().collect(Collectors.toSet());
        List<FoodItemMenuDto> foodItemDtos = new ArrayList<>();

        for (FoodItem food : foodItems) {
            FoodItemMenuDto foodItemDto = orderMapper.foodItemToFoodItemMenuDto(food);
            foodItemDto.setOriginalFoodName(food.getFoodName());

            Map<String, FoodItemTranslation> translations = food.getTranslations();

            if (translations != null) {
                FoodItemTranslation tr = translations.get(langCode);
                if (tr != null) {
                    if (tr.getName() != null && !tr.getName().isBlank()) {
                        foodItemDto.setFoodName(tr.getName());
                    }
                    if (tr.getDescription() != null && !tr.getDescription().isBlank()) {
                        foodItemDto.setDescription(tr.getDescription());
                    }
                }
            }
            if (food.getImage() != null && !food.getImage().isBlank()) {
                foodItemDto.setImage(food.getImage());
            }
            foodItemDtos.add(foodItemDto);
        }

        return foodItemDtos;
    }
}
