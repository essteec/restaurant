package com.ste.restaurant.service;

import com.ste.restaurant.dto.FoodItemDto;
import com.ste.restaurant.dto.MenuDto;
import com.ste.restaurant.dto.MenuDtoBasic;
import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.entity.Menu;
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
            return null;
        }

        if (menuRepository.existsMenuByMenuName(menu.getMenuName())) {
            return null;
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
        Optional<Menu> menu = menuRepository.findByMenuName(name);
        if (menu.isEmpty()) {
            return null;
        }
        MenuDto menuDto = new MenuDto();
        BeanUtils.copyProperties(menu.get(), menuDto);
        menuDto.setFoodItems(new HashSet<>());
        for (FoodItem foodItem : menu.get().getFoodItems()) {
            FoodItemDto foodItemDto = new FoodItemDto();
            BeanUtils.copyProperties(foodItem, foodItemDto);
            menuDto.getFoodItems().add(foodItemDto);
        }
        return menuDto;
    }

    public MenuDto deleteMenuByName(String name) {
        Optional<Menu> menu = menuRepository.findByMenuName(name);
        if (menu.isEmpty()) {
            return null;
        }
        Menu menuDel = menu.get();

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
        Optional<Menu> menuOpt = menuRepository.findByMenuName(name);
        if (menuOpt.isEmpty()) {
            return null;
        }
        Menu menuOld = menuOpt.get();

        if  (menu.getMenuName() != null && !menu.getMenuName().equals(name)) {
            if (menuRepository.findByMenuName(menu.getMenuName()).isPresent()) {
                return null;
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

    public List<MenuDto> setActiveMenu(List<String> menuNames) {
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

    public MenuDto addFoodsToMenu(String menuName, Set<String> foodItemNames) {
        Optional<Menu> menuOpt = menuRepository.findByMenuName(menuName);
        if (menuOpt.isEmpty()) {
            return null;
        }
        Menu menuOld = menuOpt.get();

        for (String foodItemName : foodItemNames) {
            Optional<FoodItem> foodItemOpt = foodItemRepository.findByFoodName(foodItemName);
            if (foodItemOpt.isEmpty()) {
                continue;
            }
            menuOld.getFoodItems().add(foodItemOpt.get());
        }
        Menu savedMenu = menuRepository.save(menuOld);

        MenuDto menuDto = new MenuDto();
        BeanUtils.copyProperties(menuOld, menuDto);
        menuDto.setFoodItems(new HashSet<>());
        for (FoodItem foodItem : savedMenu.getFoodItems()) {
            FoodItemDto foodItemDto = new FoodItemDto();
            BeanUtils.copyProperties(foodItem, foodItemDto);
            menuDto.getFoodItems().add(foodItemDto);
        }
        return menuDto;
    }

    public MenuDto removeFoodsFromMenu(String menuName, Set<String> foodItemNames) {
        Optional<Menu> menuOpt = menuRepository.findByMenuName(menuName);
        if (menuOpt.isEmpty()) {
            return null;
        }
        Menu menuOld = menuOpt.get();

        for (String foodItemName : foodItemNames) {
            Optional<FoodItem> foodItemOpt = foodItemRepository.findByFoodName(foodItemName);
            if (foodItemOpt.isEmpty()) {
                continue;
            }
            menuOld.getFoodItems().remove(foodItemOpt.get());
        }
        Menu savedMenu = menuRepository.save(menuOld);

        MenuDto menuDto = new MenuDto();
        BeanUtils.copyProperties(menuOld, menuDto);
        menuDto.setFoodItems(new HashSet<>());
        for (FoodItem foodItem : savedMenu.getFoodItems()) {
            FoodItemDto foodItemDto = new FoodItemDto();
            BeanUtils.copyProperties(foodItem, foodItemDto);
            menuDto.getFoodItems().add(foodItemDto);
        }
        return menuDto;
    }
}
