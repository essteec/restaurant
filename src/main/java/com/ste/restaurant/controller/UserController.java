package com.ste.restaurant.controller;

import com.ste.restaurant.dto.AddressDto;
import com.ste.restaurant.dto.common.BigDecimalDto;
import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.dto.userdto.*;
import com.ste.restaurant.service.AddressService;
import com.ste.restaurant.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequestMapping("rest/api/users/")
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final AddressService addressService;

    public UserController(UserService userService, AddressService addressService) {
        this.userService = userService;
        this.addressService = addressService;
    }

    // Admin
    @PostMapping
    public UserDto saveUser(@Valid @RequestBody UserDtoIO userDto) {
        return userService.saveUser(userDto);
    }

    @GetMapping
    public Page<UserDto> getAllUsers(@RequestParam(required = false) String role, @PageableDefault(size = 20) Pageable pageable) {
        if (role == null) {
            return userService.getAllUsers(pageable);
        }
        return userService.getAllUsersByRole(role, pageable);
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
    public UserDto updateUserById(@PathVariable Long id, @Valid @RequestBody UserDtoEmployee userDto, Authentication auth) {
        return userService.updateUserById(id, userDto, auth.getName());
    }

    @PatchMapping(path = "/{id}/role")
    public UserDto updateUserRoleById(@PathVariable Long id, @Valid @RequestBody StringDto role, Authentication auth) {
        return userService.updateUserRoleById(id, role, auth.getName());
    }

    @PatchMapping(path = "/{id}/salary")
    public UserDto updateEmployeeSalaryById(@PathVariable Long id, @Valid @RequestBody BigDecimalDto salary) {
        return userService.updateEmployeeSalaryById(id, salary);
    }

    // admin address
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/{id}/addresses")
    public List<AddressDto> getAddressesByUserId(@PathVariable Long id){
        return addressService.getAddressesOfUser(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}/addresses/{addressId}")
    public AddressDto deleteAddressById(@PathVariable Long id,  @PathVariable Long addressId){
        return addressService.deleteAddressById(id, addressId);
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

    @PreAuthorize("isAuthenticated()")
    @PostMapping(path = "/me/password")
    public UserDtoCustomer changeUserPassword(@RequestBody @Valid PasswordChangeDto passwordData, Authentication auth) {
        return userService.changePassword(auth.getName(), passwordData);
    }

    @GetMapping(path = "/search")
    public Page<UserDto> searchUsers(@RequestParam String query, @PageableDefault(size = 20) Pageable pageable) {
        return userService.searchUsers(query, pageable);
    }
}