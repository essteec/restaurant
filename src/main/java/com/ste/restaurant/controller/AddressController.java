package com.ste.restaurant.controller;

import com.ste.restaurant.dto.AddressDto;
import com.ste.restaurant.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasRole('CUSTOMER')")
@RequestMapping("/rest/api/addresses")
@RestController
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService){
        this.addressService = addressService;
    }

    // customer
    @PostMapping
    public AddressDto saveAddress(@Valid @RequestBody AddressDto addressDto, Authentication auth) {
        return addressService.saveAddress(addressDto, auth.getName());
    }

    @GetMapping
    public List<AddressDto> getAddresses(Authentication auth) {
        return addressService.getAddresses(auth.getName());
    }

    @DeleteMapping(path = "/{id}")
    public AddressDto deleteAddress(@PathVariable Long id, Authentication auth) {
        return addressService.deleteAddressById(id, auth.getName());
    }

    @PutMapping(path = "/{id}")
    public AddressDto updateAddress(@Valid @RequestBody AddressDto addressDto, @PathVariable Long id, Authentication auth) {
        addressDto.setAddressId(id);
        return addressService.updateAddressByEmail(addressDto, auth.getName());
    }
}
