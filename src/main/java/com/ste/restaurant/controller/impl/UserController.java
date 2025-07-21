package com.ste.restaurant.controller.impl;

import com.ste.restaurant.dto.userdto.UserDto;
import com.ste.restaurant.dto.userdto.UserDtoIO;
import com.ste.restaurant.service.impl.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("rest/api/user/")
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    @Autowired
    private UserService userService;

    // Admin
    @PostMapping(path = "/save-user")
    public UserDto saveUser(@RequestBody UserDtoIO userDto) {
        return userService.saveUser(userDto);
    }

    @GetMapping(path = "/user-list")
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping(path = "/users")
    public UserDto getUserByEmail(@RequestParam String email) {
        return  userService.getUserByEmail(email);
    }

    @DeleteMapping("/users")
    public UserDto deleteUserByEmail(@RequestParam String email) {
        return userService.deleteUserByEmail(email);
    }

    @PostMapping(path = "/user-update")
    public UserDto updateUserByEmail(@RequestParam String email, @RequestBody UserDtoIO userDto) {
        return userService.updateUserByEmail(email, userDto);
    }

    @PatchMapping(path = "/users/role")
    public UserDto updateUserRoleByEmail(@RequestParam String email, @RequestBody String userRole) {
        return userService.updateUserRoleByEmail(email, userRole);
    }

    // user own info access TODO

}