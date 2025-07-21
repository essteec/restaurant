package com.ste.restaurant.service.impl;

import com.ste.restaurant.dto.userdto.UserDto;
import com.ste.restaurant.dto.userdto.UserDtoIO;
import com.ste.restaurant.entity.User;
import com.ste.restaurant.entity.UserRole;
import com.ste.restaurant.repository.UserRepository;
import com.ste.restaurant.service.IUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    public UserDto getUserByEmail(String email) {
        UserDto userDtoResponse = new UserDto();
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return null;  // exception handle
        }
        BeanUtils.copyProperties(user.get(), userDtoResponse);
        return userDtoResponse;
    }

    public UserDto deleteUserByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User userToDelete = user.get();
        
        try {
            userRepository.delete(userToDelete);
        } catch (Exception e) {
            throw new RuntimeException("Cannot delete user. User may have associated orders or other dependencies.", e);
        }
        
        UserDto userDtoResponse = new UserDto();
        BeanUtils.copyProperties(userToDelete, userDtoResponse);
        return userDtoResponse;
    }

    public UserDto updateUserByEmail(String email, UserDtoIO userDtoIO) {
        Optional<User> userOpt = userRepository.findByEmail(email);
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

    public UserDto updateUserRoleByEmail(String email, String role) {
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null");
        }

        Optional<User> userOpt = userRepository.findByEmail(email);

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
}
