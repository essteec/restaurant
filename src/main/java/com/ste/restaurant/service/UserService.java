package com.ste.restaurant.service;

import com.ste.restaurant.dto.userdto.UserDto;
import com.ste.restaurant.dto.userdto.UserDtoIO;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.AddressRepository;
import com.ste.restaurant.repository.OrderItemRepository;
import com.ste.restaurant.repository.OrderRepository;
import com.ste.restaurant.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private OrderMapper orderMapper;

    public UserDto saveUser(UserDtoIO userDtoIO) {
        Optional<User> existUser = userRepository.findByEmail(userDtoIO.getEmail());
        if (!existUser.isEmpty()) {
            return null;  // exception
        }
        UserDto userDtoResponse = new UserDto();
        User user = new User();

        BeanUtils.copyProperties(userDtoIO, user);
        BeanUtils.copyProperties(userDtoIO, userDtoResponse);

        user.setPassword(passwordEncoder.encode(userDtoIO.getPassword()));

        userRepository.save(user);
        return userDtoResponse;
    }

    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserDto> userDtos = new ArrayList<>();

        for (User user : users) {
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(user, userDto);
            userDtos.add(userDto);
        }
        return userDtos;
    }

    public List<UserDto> getAllUsersByRole(String role) {
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user role: " + role);
        }

        List<User> users = userRepository.findAllByRole(userRole);
        List<UserDto> userDtos = new ArrayList<>();

        for (User user : users) {
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(user, userDto);
            userDtos.add(userDto);
        }
        return userDtos;
    }

    public UserDto getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            return null;  // exception handle
        }
        UserDto userDtoResponse = new UserDto();
        BeanUtils.copyProperties(user.get(), userDtoResponse);
        return userDtoResponse;
    }

    public UserDto deleteUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        User userDel = user.get();

        List<Order> orders = orderRepository.findByCustomer(userDel);
        for (Order order : orders) {
            List<OrderItem> orderItems = new ArrayList<>(order.getOrderItems());
            for (OrderItem orderItem : orderItems) {
                orderItemRepository.delete(orderItem);
            }
            orderRepository.delete(order);
        }

        List<Address> addresses = new ArrayList<>(userDel.getAddresses());
        for (Address address : addresses) {
            addressRepository.delete(address);
        }

        userRepository.delete(userDel);
        
        UserDto userDtoResponse = new UserDto();
        BeanUtils.copyProperties(userDel, userDtoResponse);
        return userDtoResponse;
    }

    public UserDto updateUserById(Long id, UserDtoIO userDtoIO) {
        userDtoIO.setPassword(null);
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return null;  // exception
        }
        User user = userOpt.get();

        if (userDtoIO.getEmail() != null) {
            if (userRepository.findByEmail(userDtoIO.getEmail()).isPresent()) {
                return null;  // exception
            }
        }
        BeanUtils.copyProperties(userDtoIO, user,
                ServiceUtil.getNullPropertyNames(userDtoIO));

        User savedUser = userRepository.save(user);

        UserDto userDtoResponse = new UserDto();
        BeanUtils.copyProperties(savedUser, userDtoResponse);
        return userDtoResponse;
    }

    public UserDto updateUserRoleById(Long id, String role) {
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null");
        }

        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            return null;  // exception
        }
        User user = userOpt.get();

        UserRole newRole;
        try {
            newRole = UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        if (user.getRole() != null && user.getRole().equals(newRole)) {
            return null;  // exception
        }
        //  if (user.getRole().equals("admin")) {} TODO
        user.setRole(newRole);
        userRepository.save(user);
        UserDto userDtoResponse = new UserDto();
        BeanUtils.copyProperties(user, userDtoResponse);
        return userDtoResponse;
    }

    public UserDto getUserByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return null;  // exception
        }
        UserDto userDtoResponse = orderMapper.userToUserDto(user.get());
        userDtoResponse.setAddressList(user.get().getAddresses());
        return userDtoResponse;
    }

    public UserDto deleteUserByEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        User userDel = userOpt.get();

        return deleteUserById(userDel.getUserId());
    }

    public UserDto updateUserByEmail(String email, UserDtoIO userDtoIO) {
        userDtoIO.setPassword(null);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();

        if (userDtoIO.getEmail() != null) {
            if (userRepository.findByEmail(userDtoIO.getEmail()).isPresent()) {
                return null;
            }
        }
        BeanUtils.copyProperties(userDtoIO, user,
                ServiceUtil.getNullPropertyNames(userDtoIO));

        User savedUser = userRepository.save(user);

        UserDto userDtoResponse = new UserDto();
        BeanUtils.copyProperties(savedUser, userDtoResponse);
        return userDtoResponse;
    }

}
