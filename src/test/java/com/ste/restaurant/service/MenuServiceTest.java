package com.ste.restaurant.service;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.dto.common.StringsDto;
import com.ste.restaurant.dto.common.WarningResponse;
import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.entity.Menu;
import com.ste.restaurant.exception.AlreadyExistsException;
import com.ste.restaurant.exception.NotFoundException;
import com.ste.restaurant.exception.NullValueException;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.FoodItemRepository;
import com.ste.restaurant.repository.MenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private FoodItemRepository foodItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private LanguageService languageService;

    @InjectMocks
    private MenuService menuService;

    private Menu testMenu;
    private MenuDto testMenuDto;
    private MenuDtoBasic testMenuDtoBasic;
    private FoodItem testFoodItem;
    private FoodItemDto testFoodItemDto;
    private Category testCategory;
    private CategoryDto testCategoryDto;

    @BeforeEach
    void setUp() {
        // Test category
        testCategory = new Category();
        testCategory.setCategoryId(1L);
        testCategory.setCategoryName("Main Course");

        // Test food item
        testFoodItem = new FoodItem();
        testFoodItem.setFoodId(1L);
        testFoodItem.setFoodName("Pizza");
        testFoodItem.setDescription("Delicious pizza");
        testFoodItem.setPrice(BigDecimal.valueOf(15.99));
        testFoodItem.setImage("pizza.jpg");
        testFoodItem.setCategories(new HashSet<>(Arrays.asList(testCategory)));

        // Test food item DTO
        testFoodItemDto = new FoodItemDto();
        testFoodItemDto.setFoodName("Pizza");
        testFoodItemDto.setDescription("Delicious pizza");
        testFoodItemDto.setPrice(BigDecimal.valueOf(15.99));
        testFoodItemDto.setImage("pizza.jpg");

        // Test menu
        testMenu = new Menu();
        testMenu.setMenuId(1L);
        testMenu.setMenuName("Lunch Menu");
        testMenu.setDescription("Lunch specials");
        testMenu.setActive(false);
        testMenu.setFoodItems(new HashSet<>(Arrays.asList(testFoodItem)));

        // Test menu DTO basic
        testMenuDtoBasic = new MenuDtoBasic();
        testMenuDtoBasic.setMenuName("Lunch Menu");
        testMenuDtoBasic.setDescription("Lunch specials");

        // Test menu DTO
        testMenuDto = new MenuDto();
        testMenuDto.setMenuName("Lunch Menu");
        testMenuDto.setDescription("Lunch specials");
        testMenuDto.setActive(false);
        testMenuDto.setFoodItems(new HashSet<>(Arrays.asList(testFoodItemDto)));

        // Test category DTO
        testCategoryDto = new CategoryDto();
        testCategoryDto.setCategoryName("Main Course");
        testCategoryDto.setFoodItems(new HashSet<>(Arrays.asList(testFoodItemDto)));
    }

    @Test
    void saveMenu_success() {
        // Arrange
        when(menuRepository.existsMenuByMenuName("Lunch Menu")).thenReturn(false);
        when(orderMapper.menuDtoBasicToMenu(testMenuDtoBasic)).thenReturn(testMenu);
        when(menuRepository.save(testMenu)).thenReturn(testMenu);
        when(orderMapper.menuToMenuDtoBasic(testMenu)).thenReturn(testMenuDtoBasic);

        // Act
        MenuDtoBasic result = menuService.saveMenu(testMenuDtoBasic);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMenuName()).isEqualTo("Lunch Menu");
        verify(menuRepository).existsMenuByMenuName("Lunch Menu");
        verify(orderMapper).menuDtoBasicToMenu(testMenuDtoBasic);
        verify(menuRepository).save(testMenu);
        verify(orderMapper).menuToMenuDtoBasic(testMenu);
    }

    @Test
    void saveMenu_nullMenuName() {
        // Arrange
        testMenuDtoBasic.setMenuName(null);

        // Act & Assert
        assertThatThrownBy(() -> menuService.saveMenu(testMenuDtoBasic))
                .isInstanceOf(NullValueException.class);
        verify(menuRepository, never()).existsMenuByMenuName(any());
        verify(menuRepository, never()).save(any());
    }

    @Test
    void saveMenu_menuAlreadyExists() {
        // Arrange
        when(menuRepository.existsMenuByMenuName("Lunch Menu")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> menuService.saveMenu(testMenuDtoBasic))
                .isInstanceOf(AlreadyExistsException.class);
        verify(menuRepository).existsMenuByMenuName("Lunch Menu");
        verify(menuRepository, never()).save(any());
    }

    @Test
    void getAllMenu_success() {
        // Arrange
        List<Menu> menus = Arrays.asList(testMenu);
        when(menuRepository.findAll()).thenReturn(menus);
        when(orderMapper.menuToMenuDto(testMenu)).thenReturn(testMenuDto);

        // Act
        List<MenuDto> result = menuService.getAllMenu();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMenuName()).isEqualTo("Lunch Menu");
        verify(menuRepository).findAll();
        verify(orderMapper).menuToMenuDto(testMenu);
    }

    @Test
    void getAllMenu_emptyResult() {
        // Arrange
        when(menuRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<MenuDto> result = menuService.getAllMenu();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(menuRepository).findAll();
    }

    @Test
    void getMenuByName_success() {
        // Arrange
        when(menuRepository.findByMenuName("Lunch Menu")).thenReturn(Optional.of(testMenu));
        when(orderMapper.menuToMenuDto(testMenu)).thenReturn(testMenuDto);

        // Act
        MenuDto result = menuService.getMenuByName("Lunch Menu");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMenuName()).isEqualTo("Lunch Menu");
        verify(menuRepository).findByMenuName("Lunch Menu");
        verify(orderMapper).menuToMenuDto(testMenu);
    }

    @Test
    void getMenuByName_notFound() {
        // Arrange
        when(menuRepository.findByMenuName("NonExistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> menuService.getMenuByName("NonExistent"))
                .isInstanceOf(NotFoundException.class);
        verify(menuRepository).findByMenuName("NonExistent");
    }

    @Test
    void deleteMenuByName_success() {
        // Arrange
        when(menuRepository.findByMenuName("Lunch Menu")).thenReturn(Optional.of(testMenu));
        when(orderMapper.menuToMenuDto(testMenu)).thenReturn(testMenuDto);

        // Act
        MenuDto result = menuService.deleteMenuByName("Lunch Menu");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMenuName()).isEqualTo("Lunch Menu");
        verify(menuRepository).findByMenuName("Lunch Menu");
        verify(menuRepository).delete(testMenu);
        verify(orderMapper).menuToMenuDto(testMenu);
    }

    @Test
    void deleteMenuByName_notFound() {
        // Arrange
        when(menuRepository.findByMenuName("NonExistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> menuService.deleteMenuByName("NonExistent"))
                .isInstanceOf(NotFoundException.class);
        verify(menuRepository).findByMenuName("NonExistent");
        verify(menuRepository, never()).delete(any());
    }

    @Test
    void updateMenuByName_success() {
        // Arrange
        MenuDtoBasic updateDto = new MenuDtoBasic();
        updateDto.setMenuName("Updated Lunch Menu");
        updateDto.setDescription("Updated description");

        when(menuRepository.findByMenuName("Lunch Menu")).thenReturn(Optional.of(testMenu));
        when(menuRepository.findByMenuName("Updated Lunch Menu")).thenReturn(Optional.empty());
        when(menuRepository.save(testMenu)).thenReturn(testMenu);
        when(orderMapper.menuToMenuDto(testMenu)).thenReturn(testMenuDto);

        // Act
        MenuDto result = menuService.updateMenuByName("Lunch Menu", updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(menuRepository).findByMenuName("Lunch Menu");
        verify(menuRepository).findByMenuName("Updated Lunch Menu");
        verify(orderMapper).updateMenuFromDto(updateDto, testMenu);
        verify(menuRepository).save(testMenu);
        verify(orderMapper).menuToMenuDto(testMenu);
    }

    @Test
    void updateMenuByName_menuNotFound() {
        // Arrange
        MenuDtoBasic updateDto = new MenuDtoBasic();
        updateDto.setMenuName("Updated Menu");
        when(menuRepository.findByMenuName("NonExistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> menuService.updateMenuByName("NonExistent", updateDto))
                .isInstanceOf(NotFoundException.class);
        verify(menuRepository).findByMenuName("NonExistent");
        verify(menuRepository, never()).save(any());
    }

    @Test
    void updateMenuByName_newNameAlreadyExists() {
        // Arrange
        MenuDtoBasic updateDto = new MenuDtoBasic();
        updateDto.setMenuName("ExistingMenu");
        
        Menu existingMenu = new Menu();
        existingMenu.setMenuName("ExistingMenu");

        when(menuRepository.findByMenuName("Lunch Menu")).thenReturn(Optional.of(testMenu));
        when(menuRepository.findByMenuName("ExistingMenu")).thenReturn(Optional.of(existingMenu));

        // Act & Assert
        assertThatThrownBy(() -> menuService.updateMenuByName("Lunch Menu", updateDto))
                .isInstanceOf(AlreadyExistsException.class);
        verify(menuRepository).findByMenuName("Lunch Menu");
        verify(menuRepository).findByMenuName("ExistingMenu");
        verify(menuRepository, never()).save(any());
    }

    @Test
    void updateMenuByName_sameNameNoChange() {
        // Arrange
        MenuDtoBasic updateDto = new MenuDtoBasic();
        updateDto.setMenuName("Lunch Menu");
        updateDto.setDescription("Updated description");

        when(menuRepository.findByMenuName("Lunch Menu")).thenReturn(Optional.of(testMenu));
        when(menuRepository.save(testMenu)).thenReturn(testMenu);
        when(orderMapper.menuToMenuDto(testMenu)).thenReturn(testMenuDto);

        // Act
        MenuDto result = menuService.updateMenuByName("Lunch Menu", updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(menuRepository).findByMenuName("Lunch Menu");
        // No second check for existing name since it's the same
        verify(orderMapper).updateMenuFromDto(updateDto, testMenu);
        verify(menuRepository).save(testMenu);
    }

    @Test
    void updateMenuByName_nullNewName() {
        // Arrange
        MenuDtoBasic updateDto = new MenuDtoBasic();
        updateDto.setMenuName(null);
        updateDto.setDescription("Updated description");

        when(menuRepository.findByMenuName("Lunch Menu")).thenReturn(Optional.of(testMenu));
        when(menuRepository.save(testMenu)).thenReturn(testMenu);
        when(orderMapper.menuToMenuDto(testMenu)).thenReturn(testMenuDto);

        // Act
        MenuDto result = menuService.updateMenuByName("Lunch Menu", updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(menuRepository).findByMenuName("Lunch Menu");
        // No check for existing name since new name is null
        verify(orderMapper).updateMenuFromDto(updateDto, testMenu);
        verify(menuRepository).save(testMenu);
    }

    @Test
    void setActiveMenu_success() {
        // Arrange
        StringsDto menuNamesDto = new StringsDto();
        menuNamesDto.setNames(new HashSet<>(Arrays.asList("Lunch Menu")));

        when(menuRepository.findByMenuName("Lunch Menu")).thenReturn(Optional.of(testMenu));
        when(menuRepository.save(testMenu)).thenReturn(testMenu);
        when(orderMapper.menuToMenuDto(testMenu)).thenReturn(testMenuDto);

        // Act
        WarningResponse<List<MenuDto>> result = menuService.setActiveMenu(menuNamesDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getWarnings()).isEmpty();
        assertThat(result.getData().get(0).getMenuName()).isEqualTo("Lunch Menu");
        verify(menuRepository).deactivateAll();
        verify(menuRepository).findByMenuName("Lunch Menu");
        verify(menuRepository).save(testMenu);
        verify(orderMapper).menuToMenuDto(testMenu);
    }

    @Test
    void setActiveMenu_withWarnings() {
        // Arrange
        StringsDto menuNamesDto = new StringsDto();
        menuNamesDto.setNames(new HashSet<>(Arrays.asList("Lunch Menu", "NonExistent")));

        when(menuRepository.findByMenuName("Lunch Menu")).thenReturn(Optional.of(testMenu));
        when(menuRepository.findByMenuName("NonExistent")).thenReturn(Optional.empty());
        when(menuRepository.save(testMenu)).thenReturn(testMenu);
        when(orderMapper.menuToMenuDto(testMenu)).thenReturn(testMenuDto);

        // Act
        WarningResponse<List<MenuDto>> result = menuService.setActiveMenu(menuNamesDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0)).isEqualTo("NonExistent");
        verify(menuRepository).deactivateAll();
        verify(menuRepository).findByMenuName("Lunch Menu");
        verify(menuRepository).findByMenuName("NonExistent");
        verify(menuRepository).save(testMenu);
    }

    @Test
    void setActiveMenu_allFailed() {
        // Arrange
        StringsDto menuNamesDto = new StringsDto();
        menuNamesDto.setNames(new HashSet<>(Arrays.asList("NonExistent1", "NonExistent2")));

        when(menuRepository.findByMenuName("NonExistent1")).thenReturn(Optional.empty());
        when(menuRepository.findByMenuName("NonExistent2")).thenReturn(Optional.empty());

        // Act
        WarningResponse<List<MenuDto>> result = menuService.setActiveMenu(menuNamesDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).isEmpty();
        assertThat(result.getWarnings()).hasSize(2);
        verify(menuRepository).deactivateAll();
        verify(menuRepository, never()).save(any());
    }

    @Test
    void getActiveMenu_success() {
        // Arrange
        testMenu.setActive(true);
        List<Menu> activeMenus = Arrays.asList(testMenu);
        when(menuRepository.findAllByActive(true)).thenReturn(activeMenus);
        FoodItemMenuDto foodItemMenuDto = new FoodItemMenuDto();
        when(orderMapper.foodItemToFoodItemMenuDto(testFoodItem)).thenReturn(foodItemMenuDto);
        when(languageService.countDistinctLanguages()).thenReturn(1L);
        when(languageService.existsByLanguageCode("en")).thenReturn(true);

        // Act
        List<CategoryMenuDto> result = menuService.getActiveMenu("en");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryName()).isEqualTo("Main Course");
        assertThat(result.get(0).getFoodItems()).hasSize(1);
        verify(menuRepository).findAllByActive(true);
        verify(orderMapper).foodItemToFoodItemMenuDto(testFoodItem);
        verify(languageService).countDistinctLanguages();
        verify(languageService).existsByLanguageCode("en");
    }

    @Test
    void getActiveMenu_withImagePath() {
        // Arrange
        testMenu.setActive(true);
        testFoodItem.setImage("pizza.jpg");
        List<Menu> activeMenus = Arrays.asList(testMenu);
        
        FoodItemMenuDto foodItemMenuDto = new FoodItemMenuDto();
        
        when(menuRepository.findAllByActive(true)).thenReturn(activeMenus);
        when(orderMapper.foodItemToFoodItemMenuDto(testFoodItem)).thenReturn(foodItemMenuDto);
        when(languageService.countDistinctLanguages()).thenReturn(1L);
        when(languageService.existsByLanguageCode("en")).thenReturn(true);

        // Act
        List<CategoryMenuDto> result = menuService.getActiveMenu("en");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        // The image path should be modified by the service
        verify(menuRepository).findAllByActive(true);
        verify(orderMapper).foodItemToFoodItemMenuDto(testFoodItem);
        verify(languageService).countDistinctLanguages();
        verify(languageService).existsByLanguageCode("en");
    }

    @Test
    void getActiveMenu_noActiveMenus() {
        // Arrange
        when(menuRepository.findAllByActive(true)).thenReturn(Collections.emptyList());
        when(languageService.countDistinctLanguages()).thenReturn(1L);
        when(languageService.existsByLanguageCode("en")).thenReturn(true);

        // Act
        List<CategoryMenuDto> result = menuService.getActiveMenu("en");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(menuRepository).findAllByActive(true);
        verify(languageService).countDistinctLanguages();
        verify(languageService).existsByLanguageCode("en");
    }

    @Test
    void addFoodsToMenu_success() {
        // Arrange
        StringsDto foodNamesDto = new StringsDto();
        foodNamesDto.setNames(new HashSet<>(Arrays.asList("Pizza")));

        when(menuRepository.findByMenuName("Lunch Menu")).thenReturn(Optional.of(testMenu));
        when(foodItemRepository.findByFoodName("Pizza")).thenReturn(Optional.of(testFoodItem));
        when(menuRepository.save(testMenu)).thenReturn(testMenu);
        when(orderMapper.menuToMenuDto(testMenu)).thenReturn(testMenuDto);

        // Act
        WarningResponse<MenuDto> result = menuService.addFoodsToMenu("Lunch Menu", foodNamesDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getWarnings()).isEmpty();
        verify(menuRepository).findByMenuName("Lunch Menu");
        verify(foodItemRepository).findByFoodName("Pizza");
        verify(menuRepository).save(testMenu);
        verify(orderMapper).menuToMenuDto(testMenu);
    }

    @Test
    void addFoodsToMenu_menuNotFound() {
        // Arrange
        StringsDto foodNamesDto = new StringsDto();
        foodNamesDto.setNames(new HashSet<>(Arrays.asList("Pizza")));

        when(menuRepository.findByMenuName("NonExistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> menuService.addFoodsToMenu("NonExistent", foodNamesDto))
                .isInstanceOf(NotFoundException.class);
        verify(menuRepository).findByMenuName("NonExistent");
        verify(foodItemRepository, never()).findByFoodName(any());
    }

    @Test
    void addFoodsToMenu_withWarnings() {
        // Arrange
        StringsDto foodNamesDto = new StringsDto();
        foodNamesDto.setNames(new HashSet<>(Arrays.asList("Pizza", "NonExistentFood")));

        when(menuRepository.findByMenuName("Lunch Menu")).thenReturn(Optional.of(testMenu));
        when(foodItemRepository.findByFoodName("Pizza")).thenReturn(Optional.of(testFoodItem));
        when(foodItemRepository.findByFoodName("NonExistentFood")).thenReturn(Optional.empty());
        when(menuRepository.save(testMenu)).thenReturn(testMenu);
        when(orderMapper.menuToMenuDto(testMenu)).thenReturn(testMenuDto);

        // Act
        WarningResponse<MenuDto> result = menuService.addFoodsToMenu("Lunch Menu", foodNamesDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0)).isEqualTo("NonExistentFood");
        verify(menuRepository).findByMenuName("Lunch Menu");
        verify(foodItemRepository).findByFoodName("Pizza");
        verify(foodItemRepository).findByFoodName("NonExistentFood");
        verify(menuRepository).save(testMenu);
    }

    @Test
    void removeFoodsFromMenu_success() {
        // Arrange
        StringsDto foodNamesDto = new StringsDto();
        foodNamesDto.setNames(new HashSet<>(Arrays.asList("Pizza")));

        when(menuRepository.findByMenuName("Lunch Menu")).thenReturn(Optional.of(testMenu));
        when(foodItemRepository.findByFoodName("Pizza")).thenReturn(Optional.of(testFoodItem));
        when(menuRepository.save(testMenu)).thenReturn(testMenu);
        when(orderMapper.menuToMenuDto(testMenu)).thenReturn(testMenuDto);

        // Act
        WarningResponse<MenuDto> result = menuService.removeFoodsFromMenu("Lunch Menu", foodNamesDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getWarnings()).isEmpty();
        verify(menuRepository).findByMenuName("Lunch Menu");
        verify(foodItemRepository).findByFoodName("Pizza");
        verify(menuRepository).save(testMenu);
        verify(orderMapper).menuToMenuDto(testMenu);
    }

    @Test
    void removeFoodsFromMenu_menuNotFound() {
        // Arrange
        StringsDto foodNamesDto = new StringsDto();
        foodNamesDto.setNames(new HashSet<>(Arrays.asList("Pizza")));

        when(menuRepository.findByMenuName("NonExistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> menuService.removeFoodsFromMenu("NonExistent", foodNamesDto))
                .isInstanceOf(NotFoundException.class);
        verify(menuRepository).findByMenuName("NonExistent");
        verify(foodItemRepository, never()).findByFoodName(any());
    }

    @Test
    void removeFoodsFromMenu_withWarnings() {
        // Arrange
        StringsDto foodNamesDto = new StringsDto();
        foodNamesDto.setNames(new HashSet<>(Arrays.asList("Pizza", "NonExistentFood")));

        when(menuRepository.findByMenuName("Lunch Menu")).thenReturn(Optional.of(testMenu));
        when(foodItemRepository.findByFoodName("Pizza")).thenReturn(Optional.of(testFoodItem));
        when(foodItemRepository.findByFoodName("NonExistentFood")).thenReturn(Optional.empty());
        when(menuRepository.save(testMenu)).thenReturn(testMenu);
        when(orderMapper.menuToMenuDto(testMenu)).thenReturn(testMenuDto);

        // Act
        WarningResponse<MenuDto> result = menuService.removeFoodsFromMenu("Lunch Menu", foodNamesDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0)).isEqualTo("NonExistentFood");
        verify(menuRepository).findByMenuName("Lunch Menu");
        verify(foodItemRepository).findByFoodName("Pizza");
        verify(foodItemRepository).findByFoodName("NonExistentFood");
        verify(menuRepository).save(testMenu);
    }
}
