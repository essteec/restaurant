package com.ste.restaurant.service;

import com.ste.restaurant.dto.CategoryDto;
import com.ste.restaurant.dto.CategoryDtoBasic;
import com.ste.restaurant.dto.FoodItemDto;
import com.ste.restaurant.dto.common.StringsDto;
import com.ste.restaurant.dto.common.WarningResponse;
import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.exception.AlreadyExistsException;
import com.ste.restaurant.exception.NotFoundException;
import com.ste.restaurant.exception.NullValueException;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.CategoryRepository;
import com.ste.restaurant.repository.FoodItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FoodItemRepository foodItemRepository; // Although not directly used by saveCategory, it's a.java dependency

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private CategoryService categoryService;

    private CategoryDtoBasic categoryDtoBasic;
    private Category category;
    private CategoryDto categoryDto;
    private FoodItem testFoodItem1;
    private FoodItem testFoodItem2;
    private FoodItemDto testFoodItemDto1;
    private FoodItemDto testFoodItemDto2;

    @BeforeEach
    void setUp() {
        categoryDtoBasic = new CategoryDtoBasic("TestCategory");
        category = new Category();
        category.setCategoryName("TestCategory");
        category.setCategoryId(1L);
        category.setFoodItems(new HashSet<>());

        categoryDto = new CategoryDto();
        categoryDto.setCategoryName("TestCategory");
        categoryDto.setFoodItems(new HashSet<>());

        testFoodItem1 = new FoodItem();
        testFoodItem1.setFoodId(1L);
        testFoodItem1.setFoodName("Pizza");
        testFoodItem1.setPrice(BigDecimal.valueOf(10.00));

        testFoodItem2 = new FoodItem();
        testFoodItem2.setFoodId(2L);
        testFoodItem2.setFoodName("Pasta");
        testFoodItem2.setPrice(BigDecimal.valueOf(15.00));

        testFoodItemDto1 = new FoodItemDto();
        testFoodItemDto1.setFoodName("Pizza");
        testFoodItemDto1.setPrice(BigDecimal.valueOf(10.00));

        testFoodItemDto2 = new FoodItemDto();
        testFoodItemDto2.setFoodName("Pasta");
        testFoodItemDto2.setPrice(BigDecimal.valueOf(15.00));
    }

        @Test
    void saveCategory_success() {
        // Arrange
        when(categoryRepository.existsCategoryByCategoryName("TestCategory")).thenReturn(false);
        when(orderMapper.categoryDtoBasicToCategory(categoryDtoBasic)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(orderMapper.categoryToCategoryDtoBasic(category)).thenReturn(categoryDtoBasic);

        // Act
        CategoryDtoBasic result = categoryService.saveCategory(categoryDtoBasic);

        // Assert
        assertNotNull(result);
        assertEquals("TestCategory", result.getCategoryName());
        verify(categoryRepository, times(1)).existsCategoryByCategoryName("TestCategory");
        verify(orderMapper, times(1)).categoryDtoBasicToCategory(categoryDtoBasic);
        verify(categoryRepository, times(1)).save(category);
        verify(orderMapper, times(1)).categoryToCategoryDtoBasic(category);
    }

    @Test
    void saveCategory_categoryAlreadyExists() {
        // Arrange
        when(categoryRepository.existsCategoryByCategoryName("TestCategory")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.saveCategory(categoryDtoBasic))
                .isInstanceOf(AlreadyExistsException.class);
        verify(categoryRepository, times(1)).existsCategoryByCategoryName("TestCategory");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void saveCategory_nullCategoryName() {
        // Arrange
        CategoryDtoBasic nullNameCategory = new CategoryDtoBasic(null);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.saveCategory(nullNameCategory))
                .isInstanceOf(NullValueException.class);
        verify(categoryRepository, never()).existsCategoryByCategoryName(anyString());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void listAllCategory_success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Category> categories = Arrays.asList(category);
        Page<Category> categoryPage = new PageImpl<>(categories, pageable, 1);
        when(categoryRepository.findAll(pageable)).thenReturn(categoryPage);
        when(orderMapper.categoryToCategoryDto(category)).thenReturn(categoryDto);

        // Act
        Page<CategoryDto> result = categoryService.listAllCategory(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategoryName()).isEqualTo("TestCategory");
        verify(categoryRepository).findAll(pageable);
        verify(orderMapper).categoryToCategoryDto(category);
    }

    @Test
    void listAllCategory_emptyResult() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(categoryRepository.findAll(pageable)).thenReturn(emptyPage);

        // Act
        Page<CategoryDto> result = categoryService.listAllCategory(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(categoryRepository).findAll(pageable);
    }

    @Test
    void deleteCategoryByName_success() {
        // Arrange
        category.getFoodItems().add(testFoodItem1);
        when(categoryRepository.findByCategoryName("TestCategory")).thenReturn(Optional.of(category));
        when(orderMapper.categoryToCategoryDto(category)).thenReturn(categoryDto);
        when(orderMapper.foodItemToFoodItemDto(testFoodItem1)).thenReturn(testFoodItemDto1);

        // Act
        CategoryDto result = categoryService.deleteCategoryByName("TestCategory");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCategoryName()).isEqualTo("TestCategory");
        assertThat(result.getFoodItems()).hasSize(1);
        verify(categoryRepository).findByCategoryName("TestCategory");
        verify(categoryRepository).delete(category);
        verify(orderMapper).categoryToCategoryDto(category);
        verify(orderMapper).foodItemToFoodItemDto(testFoodItem1);
    }

    @Test
    void deleteCategoryByName_notFound() {
        // Arrange
        when(categoryRepository.findByCategoryName("NonExistentCategory")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.deleteCategoryByName("NonExistentCategory"))
                .isInstanceOf(NotFoundException.class);
        verify(categoryRepository).findByCategoryName("NonExistentCategory");
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void updateCategoryByName_success() {
        // Arrange
        CategoryDtoBasic updateDto = new CategoryDtoBasic("UpdatedCategory");
        Category updatedCategory = new Category();
        updatedCategory.setCategoryName("UpdatedCategory");
        updatedCategory.setCategoryId(1L);

        when(categoryRepository.findByCategoryName("TestCategory")).thenReturn(Optional.of(category));
        when(categoryRepository.findByCategoryName("UpdatedCategory")).thenReturn(Optional.empty());
        when(categoryRepository.save(category)).thenReturn(updatedCategory);
        when(orderMapper.categoryToCategoryDto(updatedCategory)).thenReturn(categoryDto);

        // Act
        CategoryDto result = categoryService.updateCategoryByName("TestCategory", updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(categoryRepository).findByCategoryName("TestCategory");
        verify(categoryRepository).findByCategoryName("UpdatedCategory");
        verify(orderMapper).updateCategoryFromDto(updateDto, category);
        verify(categoryRepository).save(category);
        verify(orderMapper).categoryToCategoryDto(updatedCategory);
    }

    @Test
    void updateCategoryByName_categoryNotFound() {
        // Arrange
        CategoryDtoBasic updateDto = new CategoryDtoBasic("UpdatedCategory");
        when(categoryRepository.findByCategoryName("NonExistentCategory")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.updateCategoryByName("NonExistentCategory", updateDto))
                .isInstanceOf(NotFoundException.class);
        verify(categoryRepository).findByCategoryName("NonExistentCategory");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategoryByName_newNameAlreadyExists() {
        // Arrange
        CategoryDtoBasic updateDto = new CategoryDtoBasic("ExistingCategory");
        Category existingCategory = new Category();
        existingCategory.setCategoryName("ExistingCategory");

        when(categoryRepository.findByCategoryName("TestCategory")).thenReturn(Optional.of(category));
        when(categoryRepository.findByCategoryName("ExistingCategory")).thenReturn(Optional.of(existingCategory));

        // Act & Assert
        assertThatThrownBy(() -> categoryService.updateCategoryByName("TestCategory", updateDto))
                .isInstanceOf(AlreadyExistsException.class);
        verify(categoryRepository).findByCategoryName("TestCategory");
        verify(categoryRepository).findByCategoryName("ExistingCategory");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategoryByName_sameNameNoChange() {
        // Arrange
        CategoryDtoBasic updateDto = new CategoryDtoBasic("TestCategory");
        when(categoryRepository.findByCategoryName("TestCategory")).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);
        when(orderMapper.categoryToCategoryDto(category)).thenReturn(categoryDto);

        // Act
        CategoryDto result = categoryService.updateCategoryByName("TestCategory", updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(categoryRepository).findByCategoryName("TestCategory");
        // No second check because the name is the same
        verify(orderMapper).updateCategoryFromDto(updateDto, category);
        verify(categoryRepository).save(category);
    }

    @Test
    void addFoodItemsToCategory_success() {
        // Arrange
        Set<String> foodNames = new HashSet<>(Arrays.asList("Pizza", "Pasta"));
        StringsDto foodItemNames = new StringsDto(foodNames);
        when(categoryRepository.findByCategoryName("TestCategory")).thenReturn(Optional.of(category));
        when(foodItemRepository.findByFoodName("Pizza")).thenReturn(Optional.of(testFoodItem1));
        when(foodItemRepository.findByFoodName("Pasta")).thenReturn(Optional.of(testFoodItem2));
        when(categoryRepository.save(category)).thenReturn(category);
        when(orderMapper.categoryToCategoryDto(category)).thenReturn(categoryDto);

        // Act
        WarningResponse<CategoryDto> result = categoryService.addFoodItemsToCategory("TestCategory", foodItemNames);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getWarnings()).isEmpty();
        verify(categoryRepository).findByCategoryName("TestCategory");
        verify(foodItemRepository).findByFoodName("Pizza");
        verify(foodItemRepository).findByFoodName("Pasta");
        verify(categoryRepository).save(category);
        verify(orderMapper).categoryToCategoryDto(category);
    }

    @Test
    void addFoodItemsToCategory_categoryNotFound() {
        // Arrange
        Set<String> foodNames = new HashSet<>(Arrays.asList("Pizza"));
        StringsDto foodItemNames = new StringsDto(foodNames);
        when(categoryRepository.findByCategoryName("NonExistentCategory")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.addFoodItemsToCategory("NonExistentCategory", foodItemNames))
                .isInstanceOf(NotFoundException.class);
        verify(categoryRepository).findByCategoryName("NonExistentCategory");
        verify(foodItemRepository, never()).findByFoodName(anyString());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void addFoodItemsToCategory_someFoodItemsNotFound() {
        // Arrange
        Set<String> foodNames = new HashSet<>(Arrays.asList("Pizza", "NonExistentFood"));
        StringsDto foodItemNames = new StringsDto(foodNames);
        when(categoryRepository.findByCategoryName("TestCategory")).thenReturn(Optional.of(category));
        when(foodItemRepository.findByFoodName("Pizza")).thenReturn(Optional.of(testFoodItem1));
        when(foodItemRepository.findByFoodName("NonExistentFood")).thenReturn(Optional.empty());
        when(categoryRepository.save(category)).thenReturn(category);
        when(orderMapper.categoryToCategoryDto(category)).thenReturn(categoryDto);

        // Act
        WarningResponse<CategoryDto> result = categoryService.addFoodItemsToCategory("TestCategory", foodItemNames);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings()).contains("NonExistentFood");
        verify(categoryRepository).findByCategoryName("TestCategory");
        verify(foodItemRepository).findByFoodName("Pizza");
        verify(foodItemRepository).findByFoodName("NonExistentFood");
        verify(categoryRepository).save(category);
    }

    @Test
    void removeFoodItemsFromCategory_success() {
        // Arrange
        category.getFoodItems().add(testFoodItem1);
        Set<String> foodNames = new HashSet<>(Arrays.asList("Pizza"));
        StringsDto foodItemNames = new StringsDto(foodNames);
        when(categoryRepository.findByCategoryName("TestCategory")).thenReturn(Optional.of(category));
        when(foodItemRepository.findByFoodName("Pizza")).thenReturn(Optional.of(testFoodItem1));
        when(categoryRepository.save(category)).thenReturn(category);
        when(orderMapper.categoryToCategoryDto(category)).thenReturn(categoryDto);

        // Act
        WarningResponse<CategoryDto> result = categoryService.removeFoodItemsFromCategory("TestCategory", foodItemNames);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getWarnings()).isEmpty();
        verify(categoryRepository).findByCategoryName("TestCategory");
        verify(foodItemRepository).findByFoodName("Pizza");
        verify(categoryRepository).save(category);
        verify(orderMapper).categoryToCategoryDto(category);
    }

    @Test
    void removeFoodItemsFromCategory_categoryNotFound() {
        // Arrange
        Set<String> foodNames = new HashSet<>(Arrays.asList("Pizza"));
        StringsDto foodItemNames = new StringsDto(foodNames);
        when(categoryRepository.findByCategoryName("NonExistentCategory")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.removeFoodItemsFromCategory("NonExistentCategory", foodItemNames))
                .isInstanceOf(NotFoundException.class);
        verify(categoryRepository).findByCategoryName("NonExistentCategory");
        verify(foodItemRepository, never()).findByFoodName(anyString());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void removeFoodItemsFromCategory_someFoodItemsNotFound() {
        // Arrange
        Set<String> foodNames = new HashSet<>(Arrays.asList("Pizza", "NonExistentFood"));
        StringsDto foodItemNames = new StringsDto(foodNames);
        when(categoryRepository.findByCategoryName("TestCategory")).thenReturn(Optional.of(category));
        when(foodItemRepository.findByFoodName("Pizza")).thenReturn(Optional.of(testFoodItem1));
        when(foodItemRepository.findByFoodName("NonExistentFood")).thenReturn(Optional.empty());
        when(categoryRepository.save(category)).thenReturn(category);
        when(orderMapper.categoryToCategoryDto(category)).thenReturn(categoryDto);

        // Act
        WarningResponse<CategoryDto> result = categoryService.removeFoodItemsFromCategory("TestCategory", foodItemNames);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings()).contains("NonExistentFood");
        verify(categoryRepository).findByCategoryName("TestCategory");
        verify(foodItemRepository).findByFoodName("Pizza");
        verify(foodItemRepository).findByFoodName("NonExistentFood");
        verify(categoryRepository).save(category);
    }

    @Test
    void saveCategory_alreadyExists() {
        // Arrange
        when(categoryRepository.existsCategoryByCategoryName("TestCategory")).thenReturn(true);

        // Act & Assert (expect an exception)
        AlreadyExistsException thrown = assertThrows(AlreadyExistsException.class, () -> categoryService.saveCategory(categoryDtoBasic));

        assertEquals("Category already exists", thrown.getMessage());


        // Verify that save was NOT called
        verify(categoryRepository, never()).save(any(Category.class));
        verify(orderMapper, never()).categoryDtoBasicToCategory(any(CategoryDtoBasic.class));
        verify(orderMapper, never()).categoryToCategoryDtoBasic(any(Category.class));
    }
}
