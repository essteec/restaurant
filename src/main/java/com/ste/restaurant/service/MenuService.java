package com.ste.restaurant.service;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.entity.Menu;
import com.ste.restaurant.exception.AlreadyExistsException;
import com.ste.restaurant.exception.NotFoundException;
import com.ste.restaurant.exception.NullValueException;
import com.ste.restaurant.repository.CategoryRepository;
import com.ste.restaurant.repository.FoodItemRepository;
import com.ste.restaurant.repository.MenuRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    public MenuDtoBasic saveMenu(MenuDtoBasic menu) {
        if (menu.getMenuName() == null) {
            throw new NullValueException("Menu", "name");
        }

        if (menuRepository.existsMenuByMenuName(menu.getMenuName())) {
            throw new AlreadyExistsException("Menu", menu.getMenuName());
        }
        Menu menuSave = new Menu();
        BeanUtils.copyProperties(menu, menuSave);
        menuRepository.save(menuSave);
        return menu;
    }

    public List<MenuDto> getAllMenu() {
        List<Menu> menus = menuRepository.findAll();
        List<MenuDto> menuDtos = new ArrayList<>();
        for (Menu menu : menus) {
            MenuDto menuDto = new MenuDto();
            BeanUtils.copyProperties(menu, menuDto);
            menuDtos.add(menuDto);
            menuDto.setFoodItems(new HashSet<>());
            for (FoodItem foodItem : menu.getFoodItems()) {
                FoodItemDto foodItemDto = new FoodItemDto();
                BeanUtils.copyProperties(foodItem, foodItemDto);
                menuDto.getFoodItems().add(foodItemDto);
            }
        }
        return menuDtos;
    }

    public MenuDto getMenuByName(String name) {
        Menu menu = menuRepository.findByMenuName(name)
                .orElseThrow(() -> new NotFoundException("Menu", name));

        MenuDto menuDto = new MenuDto();
        BeanUtils.copyProperties(menu, menuDto);
        menuDto.setFoodItems(new HashSet<>());
        for (FoodItem foodItem : menu.getFoodItems()) {
            FoodItemDto foodItemDto = new FoodItemDto();
            BeanUtils.copyProperties(foodItem, foodItemDto);
            menuDto.getFoodItems().add(foodItemDto);
        }
        return menuDto;
    }

    public MenuDto deleteMenuByName(String name) {
        Menu menuDel = menuRepository.findByMenuName(name)
                .orElseThrow(() -> new NotFoundException("Menu", name));

        MenuDto menuDto = new MenuDto();
        BeanUtils.copyProperties(menuDel, menuDto);
        menuDto.setFoodItems(new HashSet<>());
        for (FoodItem foodItem : menuDel.getFoodItems()) {
            FoodItemDto foodItemDto = new FoodItemDto();
            BeanUtils.copyProperties(foodItem, foodItemDto);
            menuDto.getFoodItems().add(foodItemDto);
        }
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
        BeanUtils.copyProperties(menu, menuOld,
                ServiceUtil.getNullPropertyNames(menu));

        Menu savedMenu = menuRepository.save(menuOld);

        MenuDto menuResponse = new MenuDto();
        BeanUtils.copyProperties(savedMenu, menuResponse);
        menuResponse.setFoodItems(new HashSet<>());
        for (FoodItem foodItem : savedMenu.getFoodItems()) {
            FoodItemDto foodItemDto = new FoodItemDto();
            BeanUtils.copyProperties(foodItem, foodItemDto);
            menuResponse.getFoodItems().add(foodItemDto);
        }
        return menuResponse;
    }

    public List<MenuDto> setActiveMenu(StringsDto menuNamesDto) {
        Set<String> menuNames = menuNamesDto.getNames();

        List<Menu> avtiveMenus = menuRepository.findAllByActive(true);
        for  (Menu activeMenu : avtiveMenus) {
            activeMenu.setActive(false);
            menuRepository.save(activeMenu);
        }

        List<MenuDto> menuDtos = new ArrayList<>();
        for (String menuName : menuNames) {
            Menu menu = menuRepository.findByMenuName(menuName).orElse(null);
            if (menu != null) {
                menu.setActive(true);
                Menu savedMenu = menuRepository.save(menu);
                MenuDto menuDto = new MenuDto();
                BeanUtils.copyProperties(savedMenu, menuDto);
                menuDtos.add(menuDto);
            }
        }
        return menuDtos;
    }

    public List<MenuDto> getActiveMenu() {
        List<Menu> menus = menuRepository.findAllByActive(true);
        List<MenuDto> menuDtos = new ArrayList<>();
        for (Menu menu : menus) {
            MenuDto menuDto = new MenuDto();
            BeanUtils.copyProperties(menu, menuDto);
            menuDto.setFoodItems(new HashSet<>());
            for (FoodItem foodItem : menu.getFoodItems()) {
                FoodItemDto foodItemDto = new FoodItemDto();
                BeanUtils.copyProperties(foodItem, foodItemDto);
                menuDto.getFoodItems().add(foodItemDto);
            }
            menuDtos.add(menuDto);
        }
        return menuDtos;
    }

    public List<MenuDto> getActiveMenuByCategory(String categoryName) {
        Category category = categoryRepository.getCategoriesByCategoryName(categoryName);
        if (category == null) {
            return Collections.emptyList();
        }
        Set<FoodItem> categoryFoodItems = category.getFoodItems();

        List<Menu> menus = menuRepository.findAllByActive(true);
        List<MenuDto> menuDtos = new ArrayList<>();

        for (Menu menu : menus) {
            Set<FoodItem> filteredFoodItems = new HashSet<>();
            for (FoodItem foodItem : menu.getFoodItems()) {
                if (categoryFoodItems.contains(foodItem)) {
                    filteredFoodItems.add(foodItem);
                }
            }
            if (!filteredFoodItems.isEmpty()) {
                MenuDto menuDto = new MenuDto();
                BeanUtils.copyProperties(menu, menuDto);
                menuDto.setFoodItems(new HashSet<>());
                for (FoodItem foodItem : filteredFoodItems) {
                    FoodItemDto foodItemDto = new FoodItemDto();
                    BeanUtils.copyProperties(foodItem, foodItemDto);
                    menuDto.getFoodItems().add(foodItemDto);
                }
                menuDtos.add(menuDto);
            }
        }
        return menuDtos;
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

        MenuDto menuResponse = new MenuDto();
        BeanUtils.copyProperties(menuOld, menuResponse);
        menuResponse.setFoodItems(new HashSet<>());

        for (FoodItem foodItem : savedMenu.getFoodItems()) {
            FoodItemDto foodItemDto = new FoodItemDto();
            BeanUtils.copyProperties(foodItem, foodItemDto);
            menuResponse.getFoodItems().add(foodItemDto);
        }
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

        MenuDto menuResponse = new MenuDto();
        BeanUtils.copyProperties(menuOld, menuResponse);
        menuResponse.setFoodItems(new HashSet<>());
        for (FoodItem foodItem : savedMenu.getFoodItems()) {
            FoodItemDto foodItemDto = new FoodItemDto();
            BeanUtils.copyProperties(foodItem, foodItemDto);
            menuResponse.getFoodItems().add(foodItemDto);
        }
        return new WarningResponse<>(menuResponse, failedNames);
    }
}
