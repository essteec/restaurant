package com.ste.restaurant.controller;

import com.ste.restaurant.dto.MenuDto;
import com.ste.restaurant.dto.MenuDtoBasic;
import com.ste.restaurant.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RequestMapping("/rest/api/menus")
@RestController
public class MenuController {

    @Autowired
    private MenuService menuService;

    // by admin
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public MenuDtoBasic saveMenu(@RequestBody MenuDtoBasic menu) {
        return menuService.saveMenu(menu);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<MenuDto> getAllMenus() {
        return menuService.getAllMenu();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/by-name")
    public MenuDto getMenuByName(@RequestParam String name) {
        return menuService.getMenuByName(name);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{name}")
    public MenuDto deleteMenuByName(@PathVariable String name) {
        return menuService.deleteMenuByName(name);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{name}")
    public MenuDto updateMenuByName(@PathVariable String name, @RequestBody MenuDtoBasic menu) {
        return menuService.updateMenuByName(name, menu);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/active")
    public List<MenuDto> setActiveMenu(@RequestBody List<String> menuNames) {
        return menuService.setActiveMenu(menuNames);
    }

    // relation manyToMany menu -> foodItem
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{menuName}/food-items")
    public MenuDto addFoodsToMenu(@PathVariable String menuName, @RequestBody Set<String> foodNames) {
        return menuService.addFoodsToMenu(menuName, foodNames);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{menuName}/food-items")
    public MenuDto removeFoodItemFromMenu(@PathVariable String menuName, @RequestBody Set<String> foodNames) {
        return menuService.removeFoodsFromMenu(menuName, foodNames);
    }

    // by customer
    @PreAuthorize("permitAll()")
    @GetMapping(path = "/active")
    public List<MenuDto> getActiveMenu() {
        return menuService.getActiveMenu();
    }

    @PreAuthorize("permitAll()")
    @GetMapping(path = "/active/by-category")
    public List<MenuDto> getActiveMenuByCategory(@RequestParam String categoryName) {
        return menuService.getActiveMenuByCategory(categoryName);
    }
}