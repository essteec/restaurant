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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodItemServiceTest {

    @Mock
    private FoodItemRepository foodItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private FoodItemService foodItemService;

    @TempDir
    Path tempDir;

    private FoodItem testFoodItem;
    private FoodItemDto testFoodItemDto;
    private Category testCategory;
    private CategoryDtoBasic testCategoryDtoBasic;
    private Pageable pageable;
    private String uploadDir;

    @BeforeEach
    void setUp() {
        uploadDir = tempDir.toString() + "/";
        // Use reflection to set the uploadDir field
        try {
            var field = FoodItemService.class.getDeclaredField("uploadDir");
            field.setAccessible(true);
            field.set(foodItemService, uploadDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Test FoodItem entity
        testFoodItem = new FoodItem();
        testFoodItem.setFoodId(1L);
        testFoodItem.setFoodName("Margherita Pizza");
        testFoodItem.setDescription("Classic Italian pizza with tomatoes and mozzarella");
        testFoodItem.setPrice(BigDecimal.valueOf(12.99));
        testFoodItem.setImage("pizza-image.jpg");
        testFoodItem.setCategories(new HashSet<>());

        // Test FoodItemDto
        testFoodItemDto = new FoodItemDto();
        testFoodItemDto.setFoodName("Margherita Pizza");
        testFoodItemDto.setDescription("Classic Italian pizza with tomatoes and mozzarella");
        testFoodItemDto.setPrice(BigDecimal.valueOf(12.99));
        testFoodItemDto.setImage("pizza-image.jpg");

        // Test Category
        testCategory = new Category();
        testCategory.setCategoryId(1L);
        testCategory.setCategoryName("Italian");

        // Test CategoryDtoBasic
        testCategoryDtoBasic = new CategoryDtoBasic();
        testCategoryDtoBasic.setCategoryName("Italian");

        testFoodItem.getCategories().add(testCategory);

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void saveFoodItem_success() {
        // Arrange
        when(foodItemRepository.existsFoodItemByFoodName("Margherita Pizza")).thenReturn(false);
        when(orderMapper.foodItemDtoToFoodItem(testFoodItemDto)).thenReturn(testFoodItem);
        when(foodItemRepository.save(testFoodItem)).thenReturn(testFoodItem);
        when(orderMapper.foodItemToFoodItemDto(testFoodItem)).thenReturn(testFoodItemDto);

        // Act
        FoodItemDto result = foodItemService.saveFoodItem(testFoodItemDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFoodName()).isEqualTo("Margherita Pizza");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(12.99));
        verify(foodItemRepository).existsFoodItemByFoodName("Margherita Pizza");
        verify(orderMapper).foodItemDtoToFoodItem(testFoodItemDto);
        verify(foodItemRepository).save(testFoodItem);
        verify(orderMapper).foodItemToFoodItemDto(testFoodItem);
    }

    @Test
    void saveFoodItem_nullFoodName() {
        // Arrange
        testFoodItemDto.setFoodName(null);

        // Act & Assert
        assertThatThrownBy(() -> foodItemService.saveFoodItem(testFoodItemDto))
                .isInstanceOf(NullValueException.class)
                .hasMessageContaining("Food name cannot be null");
        verify(foodItemRepository, never()).existsFoodItemByFoodName(any());
        verify(foodItemRepository, never()).save(any());
    }

    @Test
    void saveFoodItem_alreadyExists() {
        // Arrange
        when(foodItemRepository.existsFoodItemByFoodName("Margherita Pizza")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> foodItemService.saveFoodItem(testFoodItemDto))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Food already exists");
        verify(foodItemRepository).existsFoodItemByFoodName("Margherita Pizza");
        verify(foodItemRepository, never()).save(any());
    }

    @Test
    void getAllFoodItems_success() {
        // Arrange
        List<FoodItem> foodItems = Arrays.asList(testFoodItem);
        Page<FoodItem> foodItemPage = new PageImpl<>(foodItems, pageable, 1);
        when(foodItemRepository.findAll(pageable)).thenReturn(foodItemPage);
        when(orderMapper.foodItemToFoodItemDto(testFoodItem)).thenReturn(testFoodItemDto);

        // Act
        Page<FoodItemDto> result = foodItemService.getAllFoodItems(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFoodName()).isEqualTo("Margherita Pizza");
        verify(foodItemRepository).findAll(pageable);
    }

    @Test
    void getAllFoodItems_emptyResult() {
        // Arrange
        Page<FoodItem> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(foodItemRepository.findAll(pageable)).thenReturn(emptyPage);

        // Act
        Page<FoodItemDto> result = foodItemService.getAllFoodItems(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(foodItemRepository).findAll(pageable);
    }

    @Test
    void getFoodItemByName_success() {
        // Arrange
        when(foodItemRepository.findByFoodName("Margherita Pizza")).thenReturn(Optional.of(testFoodItem));
        when(orderMapper.foodItemToFoodItemDto(testFoodItem)).thenReturn(testFoodItemDto);

        // Act
        FoodItemDto result = foodItemService.getFoodItemByName("Margherita Pizza");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFoodName()).isEqualTo("Margherita Pizza");
        assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(12.99));
        verify(foodItemRepository).findByFoodName("Margherita Pizza");
        verify(orderMapper).foodItemToFoodItemDto(testFoodItem);
    }

    @Test
    void getFoodItemByName_notFound() {
        // Arrange
        when(foodItemRepository.findByFoodName("NonExistent Pizza")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> foodItemService.getFoodItemByName("NonExistent Pizza"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Food not found");
        verify(foodItemRepository).findByFoodName("NonExistent Pizza");
        verify(orderMapper, never()).foodItemToFoodItemDto(any());
    }

    @Test
    void deleteFoodItemByName_success() {
        // Arrange
        when(foodItemRepository.findByFoodName("Margherita Pizza")).thenReturn(Optional.of(testFoodItem));
        when(orderMapper.foodItemToFoodItemDto(testFoodItem)).thenReturn(testFoodItemDto);

        // Act
        FoodItemDto result = foodItemService.deleteFoodItemByName("Margherita Pizza");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFoodName()).isEqualTo("Margherita Pizza");
        verify(foodItemRepository).findByFoodName("Margherita Pizza");
        verify(foodItemRepository).delete(testFoodItem);
        verify(orderMapper).foodItemToFoodItemDto(testFoodItem);
    }

    @Test
    void deleteFoodItemByName_notFound() {
        // Arrange
        when(foodItemRepository.findByFoodName("NonExistent Pizza")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> foodItemService.deleteFoodItemByName("NonExistent Pizza"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Food not found");
        verify(foodItemRepository).findByFoodName("NonExistent Pizza");
        verify(foodItemRepository, never()).delete(any());
    }

    @Test
    void updateFoodItemByName_success() {
        // Arrange
        FoodItemDto updateDto = new FoodItemDto();
        updateDto.setFoodName("Updated Pizza");
        updateDto.setPrice(BigDecimal.valueOf(15.99));
        updateDto.setDescription("Updated description");

        when(foodItemRepository.findByFoodName("Margherita Pizza")).thenReturn(Optional.of(testFoodItem));
        when(foodItemRepository.findByFoodName("Updated Pizza")).thenReturn(Optional.empty());
        when(foodItemRepository.save(testFoodItem)).thenReturn(testFoodItem);
        when(orderMapper.foodItemToFoodItemDto(testFoodItem)).thenReturn(updateDto);

        // Act
        FoodItemDto result = foodItemService.updateFoodItemByName("Margherita Pizza", updateDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFoodName()).isEqualTo("Updated Pizza");
        verify(foodItemRepository).findByFoodName("Margherita Pizza");
        verify(foodItemRepository).findByFoodName("Updated Pizza");
        verify(orderMapper).updateFoodItemFromDto(updateDto, testFoodItem);
        verify(foodItemRepository).save(testFoodItem);
        verify(orderMapper).foodItemToFoodItemDto(testFoodItem);
    }

    @Test
    void updateFoodItemByName_sameName() {
        // Arrange
        FoodItemDto updateDto = new FoodItemDto();
        updateDto.setFoodName("Margherita Pizza"); // Same name
        updateDto.setPrice(BigDecimal.valueOf(15.99));

        when(foodItemRepository.findByFoodName("Margherita Pizza")).thenReturn(Optional.of(testFoodItem));
        when(foodItemRepository.save(testFoodItem)).thenReturn(testFoodItem);
        when(orderMapper.foodItemToFoodItemDto(testFoodItem)).thenReturn(updateDto);

        // Act
        FoodItemDto result = foodItemService.updateFoodItemByName("Margherita Pizza", updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(foodItemRepository).findByFoodName("Margherita Pizza");
        verify(foodItemRepository, never()).findByFoodName("Updated Pizza"); // Should not check for duplicate
        verify(orderMapper).updateFoodItemFromDto(updateDto, testFoodItem);
        verify(foodItemRepository).save(testFoodItem);
    }

    @Test
    void updateFoodItemByName_originalNotFound() {
        // Arrange
        FoodItemDto updateDto = new FoodItemDto();
        updateDto.setFoodName("Updated Pizza");

        when(foodItemRepository.findByFoodName("NonExistent Pizza")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> foodItemService.updateFoodItemByName("NonExistent Pizza", updateDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Food not found");
        verify(foodItemRepository).findByFoodName("NonExistent Pizza");
        verify(foodItemRepository, never()).save(any());
    }

    @Test
    void updateFoodItemByName_newNameAlreadyExists() {
        // Arrange
        FoodItemDto updateDto = new FoodItemDto();
        updateDto.setFoodName("Existing Pizza");

        FoodItem existingFood = new FoodItem();
        existingFood.setFoodName("Existing Pizza");

        when(foodItemRepository.findByFoodName("Margherita Pizza")).thenReturn(Optional.of(testFoodItem));
        when(foodItemRepository.findByFoodName("Existing Pizza")).thenReturn(Optional.of(existingFood));

        // Act & Assert
        assertThatThrownBy(() -> foodItemService.updateFoodItemByName("Margherita Pizza", updateDto))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Food already exists");
        verify(foodItemRepository).findByFoodName("Margherita Pizza");
        verify(foodItemRepository).findByFoodName("Existing Pizza");
        verify(foodItemRepository, never()).save(any());
    }

    @Test
    void getCategories_success() {
        // Arrange
        Set<CategoryDtoBasic> categoryDtos = Set.of(testCategoryDtoBasic);
        when(foodItemRepository.findByFoodName("Margherita Pizza")).thenReturn(Optional.of(testFoodItem));
        when(orderMapper.categoriesToCategoryDtoBasics(testFoodItem.getCategories())).thenReturn(categoryDtos);

        // Act
        Set<CategoryDtoBasic> result = foodItemService.getCategories("Margherita Pizza");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getCategoryName()).isEqualTo("Italian");
        verify(foodItemRepository).findByFoodName("Margherita Pizza");
        verify(orderMapper).categoriesToCategoryDtoBasics(testFoodItem.getCategories());
    }

    @Test
    void getCategories_foodNotFound() {
        // Arrange
        when(foodItemRepository.findByFoodName("NonExistent Pizza")).thenReturn(Optional.empty());

        // Act
        Set<CategoryDtoBasic> result = foodItemService.getCategories("NonExistent Pizza");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(foodItemRepository).findByFoodName("NonExistent Pizza");
        verify(orderMapper, never()).categoriesToCategoryDtoBasics(any());
    }

    @Test
    void addImageToFood_success() throws IOException {
        // Arrange
        testFoodItem.setImage(null); // No old image
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test-image.jpg");

        when(foodItemRepository.findByFoodName("Margherita Pizza")).thenReturn(Optional.of(testFoodItem));
        when(foodItemRepository.save(testFoodItem)).thenReturn(testFoodItem);
        when(orderMapper.foodItemToFoodItemDto(testFoodItem)).thenReturn(testFoodItemDto);

        try (MockedStatic<ServiceUtil> serviceUtilMock = mockStatic(ServiceUtil.class)) {
            serviceUtilMock.when(() -> ServiceUtil.getFileExtension("test-image.jpg")).thenReturn("jpg");
            serviceUtilMock.when(() -> ServiceUtil.cropAndResizeToSquare(eq(mockFile), any(String.class), eq(600)))
                    .thenReturn(true);

            // Act
            FoodItemDto result = foodItemService.addImageToFood("Margherita Pizza", mockFile);

            // Assert
            assertThat(result).isNotNull();
            verify(foodItemRepository).findByFoodName("Margherita Pizza");
            verify(foodItemRepository).save(testFoodItem);
            verify(orderMapper).foodItemToFoodItemDto(testFoodItem);
            serviceUtilMock.verify(() -> ServiceUtil.cropAndResizeToSquare(eq(mockFile), any(String.class), eq(600)));
        }
    }

    @Test
    void addImageToFood_nullFilename() {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> foodItemService.addImageToFood("Margherita Pizza", mockFile))
                .isInstanceOf(NullValueException.class)
                .hasMessageContaining("Image filename cannot be null");
        verify(foodItemRepository, never()).findByFoodName(any());
    }

    @Test
    void addImageToFood_emptyFilename() {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("");

        // Act & Assert
        assertThatThrownBy(() -> foodItemService.addImageToFood("Margherita Pizza", mockFile))
                .isInstanceOf(NullValueException.class)
                .hasMessageContaining("Image filename cannot be null");
        verify(foodItemRepository, never()).findByFoodName(any());
    }

    @Test
    void addImageToFood_foodNotFound() {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test-image.jpg");
        when(foodItemRepository.findByFoodName("NonExistent Pizza")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> foodItemService.addImageToFood("NonExistent Pizza", mockFile))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Food not found");
        verify(foodItemRepository).findByFoodName("NonExistent Pizza");
        verify(foodItemRepository, never()).save(any());
    }

    @Test
    void addImageToFood_noFileExtension() {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test-image");
        when(foodItemRepository.findByFoodName("Margherita Pizza")).thenReturn(Optional.of(testFoodItem));

        try (MockedStatic<ServiceUtil> serviceUtilMock = mockStatic(ServiceUtil.class)) {
            serviceUtilMock.when(() -> ServiceUtil.getFileExtension("test-image")).thenReturn("");

            // Act & Assert
            assertThatThrownBy(() -> foodItemService.addImageToFood("Margherita Pizza", mockFile))
                    .isInstanceOf(ImageProcessingException.class)
                    .hasMessageContaining("Image extension cannot be null");
            verify(foodItemRepository).findByFoodName("Margherita Pizza");
        }
    }

    @Test
    void addImageToFood_imageProcessingFails() throws IOException {
        // Arrange
        testFoodItem.setImage(null); // No old image
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test-image.jpg");
        when(foodItemRepository.findByFoodName("Margherita Pizza")).thenReturn(Optional.of(testFoodItem));

        try (MockedStatic<ServiceUtil> serviceUtilMock = mockStatic(ServiceUtil.class)) {
            serviceUtilMock.when(() -> ServiceUtil.getFileExtension("test-image.jpg")).thenReturn("jpg");
            serviceUtilMock.when(() -> ServiceUtil.cropAndResizeToSquare(eq(mockFile), any(String.class), eq(600)))
                    .thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> foodItemService.addImageToFood("Margherita Pizza", mockFile))
                    .isInstanceOf(ImageProcessingException.class)
                    .hasMessageContaining("Image crop and resizing failed");
            verify(foodItemRepository).findByFoodName("Margherita Pizza");
        }
    }

    @Test
    void addImageToFood_replacesOldImage() throws IOException {
        // Arrange
        testFoodItem.setImage("old-image.jpg");
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("new-image.jpg");

        // Create the old image file in temp directory
        File oldImageFile = new File(uploadDir + "old-image.jpg");
        oldImageFile.getParentFile().mkdirs();
        oldImageFile.createNewFile();

        when(foodItemRepository.findByFoodName("Margherita Pizza")).thenReturn(Optional.of(testFoodItem));
        when(foodItemRepository.save(testFoodItem)).thenReturn(testFoodItem);
        when(orderMapper.foodItemToFoodItemDto(testFoodItem)).thenReturn(testFoodItemDto);

        try (MockedStatic<ServiceUtil> serviceUtilMock = mockStatic(ServiceUtil.class)) {
            serviceUtilMock.when(() -> ServiceUtil.getFileExtension("new-image.jpg")).thenReturn("jpg");
            serviceUtilMock.when(() -> ServiceUtil.cropAndResizeToSquare(eq(mockFile), any(String.class), eq(600)))
                    .thenReturn(true);

            // Act
            FoodItemDto result = foodItemService.addImageToFood("Margherita Pizza", mockFile);

            // Assert
            assertThat(result).isNotNull();
            assertThat(oldImageFile).doesNotExist(); // Old image should be deleted
            verify(foodItemRepository, times(2)).save(testFoodItem); // Once for deleting old image, once for setting new
        }
    }

    @Test
    void deleteImageFile_byName_success() {
        // Arrange
        testFoodItem.setImage("test-image.jpg");
        
        // Create the image file in temp directory
        File imageFile = new File(uploadDir + "test-image.jpg");
        imageFile.getParentFile().mkdirs();
        try {
            imageFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        when(foodItemRepository.findByFoodName("Margherita Pizza")).thenReturn(Optional.of(testFoodItem));
        when(foodItemRepository.save(testFoodItem)).thenReturn(testFoodItem);

        // Act
        Boolean result = foodItemService.deleteImageFile("Margherita Pizza");

        // Assert
        assertThat(result).isTrue();
        assertThat(imageFile).doesNotExist();
        assertThat(testFoodItem.getImage()).isNull();
        verify(foodItemRepository).findByFoodName("Margherita Pizza");
        verify(foodItemRepository).save(testFoodItem);
    }

    @Test
    void deleteImageFile_byName_foodNotFound() {
        // Arrange
        when(foodItemRepository.findByFoodName("NonExistent Pizza")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> foodItemService.deleteImageFile("NonExistent Pizza"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Food not found");
        verify(foodItemRepository).findByFoodName("NonExistent Pizza");
        verify(foodItemRepository, never()).save(any());
    }

    @Test
    void deleteImageFile_byName_noImage() {
        // Arrange
        testFoodItem.setImage(null);
        when(foodItemRepository.findByFoodName("Margherita Pizza")).thenReturn(Optional.of(testFoodItem));

        // Act
        Boolean result = foodItemService.deleteImageFile("Margherita Pizza");

        // Assert
        assertThat(result).isFalse();
        verify(foodItemRepository).findByFoodName("Margherita Pizza");
        verify(foodItemRepository, never()).save(any());
    }

    @Test
    void deleteImageFile_byEntity_success() {
        // Arrange
        testFoodItem.setImage("test-image.jpg");
        
        // Create the image file in temp directory
        File imageFile = new File(uploadDir + "test-image.jpg");
        imageFile.getParentFile().mkdirs();
        try {
            imageFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        when(foodItemRepository.save(testFoodItem)).thenReturn(testFoodItem);

        // Act
        Boolean result = foodItemService.deleteImageFile(testFoodItem);

        // Assert
        assertThat(result).isTrue();
        assertThat(imageFile).doesNotExist();
        assertThat(testFoodItem.getImage()).isNull();
        verify(foodItemRepository).save(testFoodItem);
    }

    @Test
    void deleteImageFile_byEntity_noImage() {
        // Arrange
        testFoodItem.setImage(null);

        // Act
        Boolean result = foodItemService.deleteImageFile(testFoodItem);

        // Assert
        assertThat(result).isFalse();
        verify(foodItemRepository, never()).save(any());
    }

    @Test
    void deleteImageFile_byEntity_fileNotExists() {
        // Arrange
        testFoodItem.setImage("nonexistent-image.jpg");

        // Act
        Boolean result = foodItemService.deleteImageFile(testFoodItem);

        // Assert
        assertThat(result).isFalse();
        verify(foodItemRepository, never()).save(any());
    }

    @Test
    void updateFoodItemByName_nullNewName() {
        // Arrange
        FoodItemDto updateDto = new FoodItemDto();
        updateDto.setFoodName(null); // Null name should not trigger duplicate check
        updateDto.setPrice(BigDecimal.valueOf(15.99));

        when(foodItemRepository.findByFoodName("Margherita Pizza")).thenReturn(Optional.of(testFoodItem));
        when(foodItemRepository.save(testFoodItem)).thenReturn(testFoodItem);
        when(orderMapper.foodItemToFoodItemDto(testFoodItem)).thenReturn(updateDto);

        // Act
        FoodItemDto result = foodItemService.updateFoodItemByName("Margherita Pizza", updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(foodItemRepository).findByFoodName("Margherita Pizza");
        verify(foodItemRepository, never()).findByFoodName((String) null); // Should not check for duplicate with null name
        verify(orderMapper).updateFoodItemFromDto(updateDto, testFoodItem);
        verify(foodItemRepository).save(testFoodItem);
    }
}