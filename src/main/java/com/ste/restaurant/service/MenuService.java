package com.ste.restaurant.service;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.dto.common.StringsDto;
import com.ste.restaurant.dto.common.WarningResponse;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.exception.AlreadyExistsException;
import com.ste.restaurant.exception.NotFoundException;
import com.ste.restaurant.exception.NullValueException;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.FoodItemRepository;
import com.ste.restaurant.repository.MenuRepository;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final FoodItemRepository foodItemRepository;
    private final OrderMapper orderMapper;
    private final LanguageService languageService;

    public MenuService(MenuRepository menuRepo, FoodItemRepository foodItemRepo, 
                       OrderMapper orderMapper, LanguageService languageService) {
        this.menuRepository = menuRepo;
        this.foodItemRepository = foodItemRepo;
        this.orderMapper = orderMapper;
        this.languageService = languageService;
    }

    public MenuDtoBasic saveMenu(MenuDtoBasic menu) {
        if (menu.getMenuName() == null) {
            throw new NullValueException("Menu", "name");
        }

        if (menuRepository.existsMenuByMenuName(menu.getMenuName())) {
            throw new AlreadyExistsException("Menu", menu.getMenuName());
        }
        Menu savedMenu = menuRepository.save(orderMapper.menuDtoBasicToMenu(menu));
        return orderMapper.menuToMenuDtoBasic(savedMenu);
    }

    public List<MenuDto> getAllMenu() {
        List<Menu> menus = menuRepository.findAll();
        List<MenuDto> menuDtos = new ArrayList<>();
        for (Menu menu : menus) {
            MenuDto menuDto = orderMapper.menuToMenuDto(menu);
            menuDtos.add(menuDto);
        }
        return menuDtos;
    }

    public MenuDto getMenuByName(String name) {
        Menu menu = menuRepository.findByMenuName(name)
                .orElseThrow(() -> new NotFoundException("Menu", name));

        return orderMapper.menuToMenuDto(menu);
    }

    public MenuDto deleteMenuByName(String name) {
        Menu menuDel = menuRepository.findByMenuName(name)
                .orElseThrow(() -> new NotFoundException("Menu", name));

        MenuDto menuDto = orderMapper.menuToMenuDto(menuDel);

        menuRepository.delete(menuDel);
        return menuDto;
    }

    public MenuDto updateMenuByName(String name, MenuDtoBasic menu) {
        Menu menuOld = menuRepository.findByMenuName(name)
                .orElseThrow(() -> new NotFoundException("Menu", name));

        if  (menu.getMenuName() != null && !menu.getMenuName().equals(name)) {
            if (menuRepository.findByMenuName(menu.getMenuName()).isPresent()) {
                throw new AlreadyExistsException("Menu", menu.getMenuName());
            }
        }
        orderMapper.updateMenuFromDto(menu, menuOld);

        Menu savedMenu = menuRepository.save(menuOld);
        return orderMapper.menuToMenuDto(savedMenu);
    }

    @Transactional
    public WarningResponse<List<MenuDto>> setActiveMenu(StringsDto menuNamesDto) {
        Set<String> menuNames = menuNamesDto.getNames();

        menuRepository.deactivateAll();

        List<MenuDto> menuDtos = new ArrayList<>();
        List<String> failedMenuNames = new ArrayList<>();
        for (String menuName : menuNames) {
            Menu menu = menuRepository.findByMenuName(menuName).orElse(null);
            if (menu == null) {
                failedMenuNames.add(menuName);
                continue;
            }

            menu.setActive(true);
            Menu savedMenu = menuRepository.save(menu);
            menuDtos.add(orderMapper.menuToMenuDto(savedMenu));
        }
        return new WarningResponse<>(menuDtos, failedMenuNames);
    }

    public List<CategoryMenuDto> getActiveMenu(String langCode) {
        // Fetch active menus
        List<Menu> menus = menuRepository.findAllByActive(true);

        // Validate/fallback language
        if (languageService.countDistinctLanguages() > 0 && !languageService.existsByLanguageCode(langCode)) {
            langCode = "en";
        }

        // Build categories -> foodItems map, deduping foods by ID to avoid cycles and equals/hashCode on entities
        Map<String, Set<FoodItemMenuDto>> categoryMap = new LinkedHashMap<>();
        Set<Long> seenFoodIds = new HashSet<>();

        for (Menu menu : menus) {
            if (menu == null || menu.getFoodItems() == null) continue;
            for (FoodItem food : menu.getFoodItems()) {
                if (food == null) continue;

                Long fid = food.getFoodId();
                if (fid != null && !seenFoodIds.add(fid)) {
                    // already processed this food for another menu
                    // continue to map to its categories again (food can appear in multiple categories)
                }

                // map food -> dto with translation overlay
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

                // Place this food into all of its categories
                Set<Category> categories = food.getCategories();
                if (categories == null) continue;
                for (Category category : categories) {
                    if (category == null) continue;
                    String catName = category.getCategoryName();
                    if (catName == null || catName.isBlank()) continue;

                    Map<String, CategoryTranslation> catTrans = category.getTranslations();
                    if (catTrans != null) {
                        CategoryTranslation ctr = catTrans.get(langCode);
                        if (ctr != null) {
                            if (ctr.getName() != null && !ctr.getName().isBlank()) {
                                catName = ctr.getName();
                            }
                        }
                    }

                    categoryMap.computeIfAbsent(catName, k -> new HashSet<>()).add(foodItemDto);
                }
            }
        }

        return categoryMap.entrySet().stream()
                .map(e -> new CategoryMenuDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public WarningResponse<MenuDto> addFoodsToMenu(String menuName, StringsDto foodItemNamesDto) {
        Set<String> foodItemNames = foodItemNamesDto.getNames();

        Menu menuOld = menuRepository.findByMenuName(menuName)
                .orElseThrow(() -> new NotFoundException("Menu", menuName));

        List<String> failedNames = new ArrayList<>();
        for (String foodName : foodItemNames) {
            Optional<FoodItem> foodItemOpt = foodItemRepository.findByFoodName(foodName);
            if (foodItemOpt.isEmpty()) {
                failedNames.add(foodName);
            } else {
                menuOld.getFoodItems().add(foodItemOpt.get());
            }
        }
        Menu savedMenu = menuRepository.save(menuOld);

        MenuDto menuResponse = orderMapper.menuToMenuDto(savedMenu);
        return new WarningResponse<>(menuResponse, failedNames);
    }

    public WarningResponse<MenuDto> removeFoodsFromMenu(String menuName, StringsDto foodItemNamesDto) {
        Set<String> foodItemNames = foodItemNamesDto.getNames();

        Menu menuOld = menuRepository.findByMenuName(menuName)
                .orElseThrow(() -> new NotFoundException("Menu", menuName));

        List<String> failedNames = new ArrayList<>();
        for (String foodName : foodItemNames) {
            Optional<FoodItem> foodItemOpt = foodItemRepository.findByFoodName(foodName);
            if (foodItemOpt.isEmpty()) {
                failedNames.add(foodName);
            } else {
                menuOld.getFoodItems().remove(foodItemOpt.get());
            }
        }

        Menu savedMenu = menuRepository.save(menuOld);

        MenuDto menuResponse = orderMapper.menuToMenuDto(savedMenu);
        return new WarningResponse<>(menuResponse, failedNames);
    }
}
