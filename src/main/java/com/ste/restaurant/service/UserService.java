package com.ste.restaurant.service;

import com.ste.restaurant.dto.common.BigDecimalDto;
import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.dto.userdto.*;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.exception.*;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.AddressRepository;
import com.ste.restaurant.repository.OrderRepository;
import com.ste.restaurant.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderMapper orderMapper;

    public UserService(UserRepository userRepository, OrderRepository orderRepository,
                       AddressRepository addressRepository, PasswordEncoder passwordEncoder, OrderMapper orderMapper) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.passwordEncoder = passwordEncoder;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public UserDto saveUser(UserDtoIO userDtoIO) {
        Optional<User> existUser = userRepository.findByEmail(userDtoIO.getEmail());
        if (existUser.isPresent()) {
            throw new AlreadyExistsException("User", userDtoIO.getEmail());
        }

        User user = orderMapper.userDtoIOToUser(userDtoIO);

        user.setPassword(passwordEncoder.encode(userDtoIO.getPassword()));

        User savedUser = userRepository.save(user);
        return orderMapper.userToUserDto(savedUser);
    }

    public Page<UserDto> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(orderMapper::userToUserDto);
    }

    public Page<UserDto> getAllUsersByRole(String role, Pageable pageable) {
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            throw new InvalidValueException("User", "role", role);
        }

        Page<User> users = userRepository.findAllByRole(userRole, pageable);
        return users.map(orderMapper::userToUserDto);
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));
        return orderMapper.userToUserDto(user);
    }

    @Transactional
    public UserDto deleteUserById(Long id) {
        User userDel = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));

        UserDto userDto = orderMapper.userToUserDto(userDel);

        orderRepository.updateCustomerAndAddressToNull(userDel);

        if (userDel.getAddresses() != null) addressRepository.deleteAll(userDel.getAddresses());

        userRepository.delete(userDel);

        return userDto;
    }

    @Transactional
    public UserDto updateUserById(Long id, UserDtoEmployee userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));

        if (userDto.getEmail() != null && !userDto.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
                throw new AlreadyExistsException("User", userDto.getEmail());
            }
        }
        orderMapper.updateUserFromDtoEmployee(userDto, user);

        User savedUser = userRepository.save(user);
        return orderMapper.userToUserDto(savedUser);
    }

    @Transactional
    public UserDto updateUserRoleById(Long id, StringDto roleDto) {
        if (roleDto.getName() == null) {
            throw new NullValueException("User", "role");
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

        if ((user.getRole() == UserRole.CHEF || user.getRole() == UserRole.WAITER) &&
                (newRole != UserRole.CHEF && newRole != UserRole.WAITER)) {
            user.setSalary(null);
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

    @Transactional
    public UserDtoCustomer deleteCustomerByEmail(String email) {
        User userDel = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        UserDtoCustomer userDtoResponse = orderMapper.userToUserDtoCustomer(userDel);
        deleteUserById(userDel.getUserId());
        return userDtoResponse;
    }

    @Transactional
    public UserDtoCustomer updateCustomerByEmail(String email, UserDtoIO userDto) {
        userDto.setPassword(null);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        if (userDto.getEmail() != null && !userDto.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
                throw new AlreadyExistsException("User", userDto.getEmail());
            }
        }

        orderMapper.updateUserFromDtoIO(userDto, user);

        User savedUser = userRepository.save(user);
        return orderMapper.userToUserDtoCustomer(savedUser);
    }

    @Transactional
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

    @Transactional
    public UserDtoCustomer changePassword(String name, PasswordChangeDto passwordData) {
        User user = userRepository.findByEmail(name)
                .orElseThrow(() -> new NotFoundException("User", name));

        String currentPassword = passwordData.getPassword();
        String newPassword = passwordData.getNewPassword();

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidValueException("User", "password", "Invalid password");
        }

        if (currentPassword.equals(newPassword)) {
            throw new InvalidValueException("User", "password", "Same password with previous one");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        User savedUser = userRepository.save(user);
        return orderMapper.userToUserDtoCustomer(savedUser);
    }
}
