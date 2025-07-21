package com.ste.restaurant.service.impl;


import com.ste.restaurant.dto.AddressDto;
import com.ste.restaurant.entity.Address;
import com.ste.restaurant.entity.User;
import com.ste.restaurant.repository.AddressRepository;
import com.ste.restaurant.repository.UserRepository;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AddressService {

    @Autowired
    AddressRepository addressRepository;

    @Autowired
    UserRepository userRepository;

    @Transactional
    public AddressDto saveAddress(AddressDto addressDto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Address address = new Address();
        BeanUtils.copyProperties(addressDto, address);

        Address savedAddress = addressRepository.save(address);

        user.getAddresses().add(savedAddress);
        userRepository.save(user);

        AddressDto responseDto = new AddressDto();
        BeanUtils.copyProperties(savedAddress, responseDto);
        return responseDto;
    }

    public List<AddressDto> getAddresses(String email) {
        List<AddressDto> addressDtos = new ArrayList<>();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Address> addresses = user.getAddresses();
        for (Address address : addresses) {
            AddressDto addressDto = new AddressDto();
            BeanUtils.copyProperties(address, addressDto);
            addressDtos.add(addressDto);
        }
        return addressDtos;
    }

    @Transactional
    public AddressDto deleteAddressById(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!user.getAddresses().contains(address)) {
            throw new RuntimeException("Address not found");
        }

        user.getAddresses().remove(address);
        userRepository.save(user);

        AddressDto addressDto = new AddressDto();
        BeanUtils.copyProperties(address, addressDto);
        return addressDto;
    }

    @Transactional
    public AddressDto updateAddressByEmail(AddressDto addressDto, String email) {
        if (addressDto.getAddressId() == null) {
            throw new RuntimeException("Address id is null");
        }

        User  user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Address address = addressRepository.findById(addressDto.getAddressId())
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!user.getAddresses().contains(address)) {
            throw new RuntimeException("Address not found");
        }

        BeanUtils.copyProperties(addressDto, address,
                ServiceUtil.getNullPropertyNames(addressDto));

        Address savedAddress = addressRepository.save(address);

        AddressDto addressResponse = new AddressDto();
        BeanUtils.copyProperties(savedAddress, addressResponse);
        return addressResponse;
    }
}
