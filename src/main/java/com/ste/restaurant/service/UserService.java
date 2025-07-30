package com.ste.restaurant.service;

import com.ste.restaurant.dto.BigDecimalDto;
import com.ste.restaurant.dto.StringDto;
import com.ste.restaurant.dto.userdto.UserDto;
import com.ste.restaurant.dto.userdto.UserDtoCustomer;
import com.ste.restaurant.dto.userdto.UserDtoEmployee;
import com.ste.restaurant.dto.userdto.UserDtoIO;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.exception.*;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.AddressRepository;
import com.ste.restaurant.repository.OrderItemRepository;
import com.ste.restaurant.repository.OrderRepository;
import com.ste.restaurant.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        if (existUser.isPresent()) {
            throw new AlreadyExistsException("User", userDtoIO.getEmail());
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
            throw new InvalidValueException("User", "role", role);
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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));
        UserDto userDtoResponse = new UserDto();
        BeanUtils.copyProperties(user, userDtoResponse);
        return userDtoResponse;
    }

    public UserDto deleteUserById(Long id) {
        User userDel = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));

        List<Order> orders = orderRepository.findByCustomer(userDel);
        for (Order order : orders) {
            List<OrderItem> orderItems = new ArrayList<>(order.getOrderItems());
            orderItemRepository.deleteAll(orderItems);
            orderRepository.delete(order);
        }

        List<Address> addresses = new ArrayList<>(userDel.getAddresses());
        addressRepository.deleteAll(addresses);

        userRepository.delete(userDel);
        
        UserDto userDtoResponse = new UserDto();
        BeanUtils.copyProperties(userDel, userDtoResponse);
        return userDtoResponse;
    }

    public UserDto updateUserById(Long id, UserDtoIO userDtoIO) {
        userDtoIO.setPassword(null);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));

        if (userDtoIO.getEmail() != null) {
            if (userRepository.findByEmail(userDtoIO.getEmail()).isPresent()) {
                throw new AlreadyExistsException("User", userDtoIO.getEmail());
            }
        }
        BeanUtils.copyProperties(userDtoIO, user,
                ServiceUtil.getNullPropertyNames(userDtoIO));

        User savedUser = userRepository.save(user);

        UserDto userDtoResponse = new UserDto();
        BeanUtils.copyProperties(savedUser, userDtoResponse);
        return userDtoResponse;
    }

    public UserDto updateUserRoleById(Long id, StringDto roleDto) {
        if (roleDto.getName() == null) {
            throw new NullValueException("User", "email");
        }
        String role = roleDto.getName();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));

        UserRole newRole;
        try {
            newRole = UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("User", "role", role);
        }

        if (user.getRole().equals(newRole)) {
            throw new AlreadyHasException("User", "role", role);
        }
        user.setRole(newRole);
        userRepository.save(user);
        return orderMapper.userToUserDto(user);
    }

    public UserDtoCustomer getCustomerByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));
        return orderMapper.userToUserDtoCustomer(user);
    }

    public UserDtoCustomer deleteCustomerByEmail(String email) {
        User userDel = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        UserDtoCustomer userDtoResponse = orderMapper.userToUserDtoCustomer(userDel);
        deleteUserById(userDel.getUserId());
        return userDtoResponse;
    }

    public UserDtoCustomer updateCustomerByEmail(String email, UserDtoIO userDto) {
        userDto.setPassword(null);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        if (userDto.getEmail() != null) {
            if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
                throw new AlreadyExistsException("User", userDto.getEmail());
            }
        }
        BeanUtils.copyProperties(userDto, user,
                ServiceUtil.getNullPropertyNames(userDto));

        User savedUser = userRepository.save(user);

        return orderMapper.userToUserDtoCustomer(savedUser);
    }

    public UserDto updateEmployeeSalaryById(Long id, BigDecimalDto salaryDto) {
        if (salaryDto.getDecimal() == null) {
            throw new NullValueException("Employee", "salary");
        }
        BigDecimal salary = salaryDto.getDecimal();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));

        if (!(user.getRole().equals(UserRole.CHEF) || user.getRole().equals(UserRole.WAITER))) {
            throw new InvalidOperationException("User", "Customers or Admins cannot have salary");
        }
        user.setSalary(salary);
        userRepository.save(user);
        return orderMapper.userToUserDto(user);
    }

    public UserDtoEmployee getEmployeeByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));
        
        return orderMapper.userToUserDtoEmployee(user);
    }
}
