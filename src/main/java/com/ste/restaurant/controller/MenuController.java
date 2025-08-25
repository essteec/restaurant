package com.ste.restaurant.controller;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.dto.common.StringsDto;
import com.ste.restaurant.dto.common.WarningResponse;
import com.ste.restaurant.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/rest/api/menus")
@RestController
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    // by admin
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public MenuDtoBasic saveMenu(@Valid @RequestBody MenuDtoBasic menu) {
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
    public MenuDto updateMenuByName(@PathVariable String name, @Valid @RequestBody MenuDtoBasic menu) {
        return menuService.updateMenuByName(name, menu);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/active")
    public WarningResponse<List<MenuDto>> setActiveMenu(@Valid @RequestBody StringsDto menuNames) {
        return menuService.setActiveMenu(menuNames);
    }

    // relation manyToMany menu -> foodItem
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{name}/food-items")
    public WarningResponse<MenuDto> addFoodsToMenu(@PathVariable String name, @Valid @RequestBody StringsDto foodNames) {
        return menuService.addFoodsToMenu(name, foodNames);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{name}/food-items")
    public WarningResponse<MenuDto> removeFoodItemFromMenu(@PathVariable String name, @Valid @RequestBody StringsDto foodNames) {
        return menuService.removeFoodsFromMenu(name, foodNames);
    }
    // TODO restaurant name, email, ourStory, mainLine, line, openingHours, phone, foodIds, galleryImages, address, location, social media links,
    // by customer // todo get menu that named "website menu" which has 3 exact food for showcase in main page
    @PreAuthorize("permitAll()")
    @GetMapping(path = "/active")
    public List<CategoryDto> getActiveMenu(@RequestHeader(value = "Accept-Language", defaultValue = "en") String langCode) {
        return menuService.getActiveMenu(langCode);
    }
}