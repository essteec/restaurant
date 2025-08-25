package com.ste.restaurant.service;


import com.ste.restaurant.dto.AddressDto;
import com.ste.restaurant.entity.Address;
import com.ste.restaurant.entity.Order;
import com.ste.restaurant.entity.User;
import com.ste.restaurant.exception.NotFoundException;
import com.ste.restaurant.exception.NullValueException;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.AddressRepository;
import com.ste.restaurant.repository.OrderRepository;
import com.ste.restaurant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository,
                          OrderMapper orderMapper, OrderRepository orderRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
        this.orderRepository = orderRepository;
    }

    // admin
    public List<AddressDto> getAddressesOfUser(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        return getAddresses(user.getEmail());
    }

    public AddressDto deleteAddressById(Long userId, Long addressId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new NotFoundException("Address", addressId));

        if (!user.getAddresses().contains(address)) {
            throw new NotFoundException("Address", addressId);
        }

        List<Order> ordersUsingAddress = orderRepository.findByAddress(address);
        ordersUsingAddress.forEach(order -> order.setAddress(null));
        orderRepository.saveAll(ordersUsingAddress);

        AddressDto addressDto = orderMapper.addressToAddressDto(address);

        user.getAddresses().remove(address);
        userRepository.save(user);

        return addressDto;
    }

    // customer
    @Transactional
    public AddressDto saveAddress(AddressDto addressDto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));
        Address address = orderMapper.addressDtoToAddress(addressDto);

        Address savedAddress = addressRepository.save(address);

        user.getAddresses().add(savedAddress);
        userRepository.save(user);

        return orderMapper.addressToAddressDto(savedAddress);
    }

    public List<AddressDto> getAddresses(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        List<Address> addresses = user.getAddresses();
        return orderMapper.addressesToAddressDtos(addresses);
    }

    @Transactional
    public AddressDto deleteAddressById(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Address", id));

        if (!user.getAddresses().contains(address)) {
            throw new NotFoundException("Address", id);  // this address doesn't belong to them
        }

        user.getAddresses().remove(address);

        userRepository.save(user);
        return orderMapper.addressToAddressDto(address);
    }

    @Transactional
    public AddressDto updateAddressByEmail(AddressDto addressDto, String email) {
        Long addressId = addressDto.getAddressId();
        if (addressId == null) {
            throw new NullValueException("Address", "id");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new NotFoundException("Address", addressId));

        if (!user.getAddresses().contains(address)) {
            throw new NotFoundException("Address", addressId);  // this address doesn't belong to them
        }
        orderMapper.updateAddressFromDto(addressDto, address);

        Address savedAddress = addressRepository.save(address);
        return orderMapper.addressToAddressDto(savedAddress);
    }
}
