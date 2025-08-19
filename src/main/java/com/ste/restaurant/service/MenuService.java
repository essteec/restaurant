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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final FoodItemRepository foodItemRepository;
    private final OrderMapper orderMapper;

    public MenuService(MenuRepository menuRepo, FoodItemRepository foodItemRepo, OrderMapper orderMapper) {
        this.menuRepository = menuRepo;
        this.foodItemRepository = foodItemRepo;
        this.orderMapper = orderMapper;
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

    public List<CategoryDto> getActiveMenu() {
        List<Menu> menus = menuRepository.findAllByActive(true);
        Set<FoodItem> foodItems = new HashSet<>();
        for (Menu menu : menus) {
            foodItems.addAll(menu.getFoodItems());
        }

        Map<String, Set<FoodItemDto>> categoryMap = new LinkedHashMap<>();

        for (FoodItem food : foodItems) {
            for (Category category : food.getCategories()) {
                FoodItemDto foodItemDto = orderMapper.foodItemToFoodItemDto(food);

                if (food.getImage() != null) {
                    foodItemDto.setImage("/images/" + food.getImage());
                }

                categoryMap.computeIfAbsent(category.getCategoryName(),
                        k -> new HashSet<>()).add(foodItemDto);
            }
        }

        return categoryMap.entrySet().stream()
                .map(entry -> new CategoryDto(entry.getKey(), entry.getValue()))
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
