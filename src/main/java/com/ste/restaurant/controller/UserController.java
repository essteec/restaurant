package com.ste.restaurant.controller;

import com.ste.restaurant.dto.AddressDto;
import com.ste.restaurant.dto.BigDecimalDto;
import com.ste.restaurant.dto.StringDto;
import com.ste.restaurant.dto.userdto.UserDto;
import com.ste.restaurant.dto.userdto.UserDtoCustomer;
import com.ste.restaurant.dto.userdto.UserDtoEmployee;
import com.ste.restaurant.dto.userdto.UserDtoIO;
import com.ste.restaurant.service.AddressService;
import com.ste.restaurant.service.UserService;
import jakarta.validation.Valid;
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
    public UserDto saveUser(@Valid @RequestBody UserDtoIO userDto) {
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
    public UserDto updateUserById(@PathVariable Long id, @Valid @RequestBody UserDtoIO userDto) {
        return userService.updateUserById(id, userDto);
    }

    @PatchMapping(path = "/{id}/role")
    public UserDto updateUserRoleById(@PathVariable Long id, @Valid @RequestBody StringDto role) {
        return userService.updateUserRoleById(id, role);
    }

    @PatchMapping(path = "/{id}/salary")
    public UserDto updateEmployeeSalaryById(@PathVariable Long id, @Valid @RequestBody BigDecimalDto salary) {
        return userService.updateEmployeeSalaryById(id, salary);
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
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping(path = "/profile")
    public UserDtoCustomer getCustomerProfile(Authentication auth) {
        return userService.getCustomerByEmail(auth.getName());
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @DeleteMapping(path = "/profile")
    public UserDtoCustomer deleteCustomerProfile(Authentication auth) {
        return userService.deleteCustomerByEmail(auth.getName());
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping(path = "/profile")
    public UserDtoCustomer updateCustomerProfile(@Valid @RequestBody UserDtoIO userDto, Authentication auth) {
        return userService.updateCustomerByEmail(auth.getName(), userDto);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER', 'CHEF')")
    @GetMapping(path = "/employee/profile")
    public UserDtoEmployee getEmployeeProfile(Authentication auth) {
        return userService.getEmployeeByEmail(auth.getName());
    }
}