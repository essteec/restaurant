package com.ste.restaurant.controller.impl;

import com.ste.restaurant.dto.MenuDto;
import com.ste.restaurant.dto.MenuDtoBasic;
import com.ste.restaurant.entity.Menu;
import com.ste.restaurant.service.impl.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RequestMapping("/rest/api/menu")
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @PostMapping(path = "/save-menu")
    public MenuDtoBasic saveMenu(@RequestBody MenuDtoBasic menu) {
        return menuService.saveMenu(menu);
    }

    @GetMapping(path = "/menu-list")
    public List<MenuDto> getAllMenus() {
        return menuService.getAllMenu();
    }

    @GetMapping(path = "/menus/by-name")
    public MenuDto getMenuByName(@RequestParam String name) {
        return menuService.getMenuByName(name);
    }

    @DeleteMapping(path = "/menu/{name}")
    public MenuDto deleteMenuByName(@PathVariable String name) {
        return menuService.deleteMenuByName(name);
    }

    @PutMapping(path = "/menu-update/{name}")
    public MenuDto updateMenuByName(@PathVariable String name, @RequestBody MenuDtoBasic menu) {
        return menuService.updateMenuByName(name, menu);
    }

    @GetMapping(path = "/menus/active")
    public List<MenuDto> getActiveMenu() {
        return menuService.getActiveMenu();
    }

    @PostMapping(path = "/menus/active")
    public List<MenuDto> setActiveMenu(@RequestBody List<String> menuNames) {
        return menuService.setActiveMenu(menuNames);
    }


    // relation manyToMany menu -> foodItem
    @PutMapping(path = "/menus/{menuName}/fooditems")
    public MenuDto addFoodsToMenu(@PathVariable String menuName, @RequestBody Set<String> foodNames) {
        return menuService.addFoodsToMenu(menuName, foodNames);
    }

    @DeleteMapping(path = "/menus/{menuName}/fooditems")
    public MenuDto removeFoodItemFromMenu(@PathVariable String menuName, @RequestBody Set<String> foodNames) {
        return menuService.removeFoodsFromMenu(menuName, foodNames);
    }

}