package com.ste.restaurant.controller;

import com.ste.restaurant.dto.AddressDto;
import com.ste.restaurant.dto.userdto.UserDto;
import com.ste.restaurant.dto.userdto.UserDtoIO;
import com.ste.restaurant.service.AddressService;
import com.ste.restaurant.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("rest/api/users/")
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private AddressService addressService;

    // Admin
    @PostMapping
    public UserDto saveUser(@RequestBody UserDtoIO userDto) {
        return userService.saveUser(userDto);
    }

    @GetMapping
    public List<UserDto> getAllUsers(@RequestParam(required = false) String role) {
        if (role == null) {
            return userService.getAllUsers();
        }
        return userService.getAllUsersByRole(role);
    }

    @GetMapping(path = "/{id}")
    public UserDto getUserById(@PathVariable Long id) {
        return  userService.getUserById(id);
    }

    @DeleteMapping("/{id}")
    public UserDto deleteUserById(@PathVariable Long id) {
        return userService.deleteUserById(id);
    }

    @PutMapping(path = "/{id}")
    public UserDto updateUserById(@PathVariable Long id, @RequestBody UserDtoIO userDto) {
        return userService.updateUserById(id, userDto);
    }

    @PatchMapping(path = "/{id}/role")
    public UserDto updateUserRoleById(@PathVariable Long id, @RequestBody String userRole) {
        return userService.updateUserRoleById(id, userRole);
    }

    // admin address
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/{userId}/addresses")
    public List<AddressDto> getAddressesByUserId(@PathVariable Long userId){
        return addressService.getAddressesOfUser(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{userId}/addresses/{addressId}")
    public AddressDto deleteAddressById(@PathVariable Long userId,  @PathVariable Long addressId){
        return addressService.deleteAddressById(userId, addressId);
    }

    // PROFILE
    @PreAuthorize("isAuthenticated()")
    @GetMapping(path = "/profile")
    public UserDto getProfile(Authentication auth) {
        return userService.getUserByEmail(auth.getName());
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping(path = "/profile")
    public UserDto deleteProfile(Authentication auth) {
        return userService.deleteUserByEmail(auth.getName());
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping(path = "/profile")
    public UserDto updateProfile(@RequestBody UserDtoIO userDto, Authentication auth) {
        return userService.updateUserByEmail(auth.getName(), userDto);
    }
}