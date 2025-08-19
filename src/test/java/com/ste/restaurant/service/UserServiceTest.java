package com.ste.restaurant.service;

import com.ste.restaurant.dto.common.BigDecimalDto;
import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.dto.userdto.*;
import com.ste.restaurant.entity.Address;
import com.ste.restaurant.entity.Order;
import com.ste.restaurant.entity.User;
import com.ste.restaurant.entity.UserRole;
import com.ste.restaurant.exception.*;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.AddressRepository;
import com.ste.restaurant.repository.OrderRepository;
import com.ste.restaurant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDto testUserDto;
    private UserDtoIO testUserDtoIO;
    private UserDtoCustomer testUserDtoCustomer;
    private UserDtoEmployee testUserDtoEmployee;
    private PasswordChangeDto testPasswordChangeDto;
    private StringDto testStringDto;
    private BigDecimalDto testBigDecimalDto;
    private Address testAddress;
    private Order testOrder;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Test user entity
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPassword("encodedPassword123");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setBirthday(LocalDate.of(1990, 1, 1));
        testUser.setLoyaltyPoints(100);
        testUser.setSalary(null);
        testUser.setAddresses(new ArrayList<>());
        testUser.setOrders(new ArrayList<>());

        // Test user DTO
        testUserDto = new UserDto();
        testUserDto.setFirstName("John");
        testUserDto.setLastName("Doe");
        testUserDto.setEmail("john.doe@example.com");
        testUserDto.setRole(UserRole.CUSTOMER);
        testUserDto.setBirthday(LocalDate.of(1990, 1, 1));
        testUserDto.setLoyaltyPoints(100);

        // Test user DTO IO
        testUserDtoIO = new UserDtoIO();
        testUserDtoIO.setFirstName("John");
        testUserDtoIO.setLastName("Doe");
        testUserDtoIO.setEmail("john.doe@example.com");
        testUserDtoIO.setPassword("Password123");
        testUserDtoIO.setBirthday(LocalDate.of(1990, 1, 1));

        // Test user DTO customer
        testUserDtoCustomer = new UserDtoCustomer();
        testUserDtoCustomer.setFirstName("John");
        testUserDtoCustomer.setLastName("Doe");
        testUserDtoCustomer.setEmail("john.doe@example.com");
        testUserDtoCustomer.setBirthday(LocalDate.of(1990, 1, 1));
        testUserDtoCustomer.setLoyaltyPoints(100);

        // Test user DTO employee
        testUserDtoEmployee = new UserDtoEmployee();
        testUserDtoEmployee.setFirstName("John");
        testUserDtoEmployee.setLastName("Doe");
        testUserDtoEmployee.setEmail("john.doe@example.com");
        testUserDtoEmployee.setRole(UserRole.WAITER);
        testUserDtoEmployee.setSalary(BigDecimal.valueOf(2500.00));

        // Test password change DTO
        testPasswordChangeDto = new PasswordChangeDto();
        testPasswordChangeDto.setPassword("OldPassword123");
        testPasswordChangeDto.setNewPassword("NewPassword123");

        // Test string DTO
        testStringDto = new StringDto();
        testStringDto.setName("WAITER");

        // Test BigDecimal DTO
        testBigDecimalDto = new BigDecimalDto();
        testBigDecimalDto.setDecimal(BigDecimal.valueOf(3000.00));

        // Test address
        testAddress = new Address();
        testAddress.setAddressId(1L);
        testAddress.setStreet("123 Main St");

        // Test order
        testOrder = new Order();
        testOrder.setOrderId(1L);

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void saveUser_success() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());
        when(orderMapper.userDtoIOToUser(testUserDtoIO)).thenReturn(testUser);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword123");
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.saveUser(testUserDtoIO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(orderMapper).userDtoIOToUser(testUserDtoIO);
        verify(passwordEncoder).encode("Password123");
        verify(userRepository).save(testUser);
        verify(orderMapper).userToUserDto(testUser);
        assertThat(testUser.getPassword()).isEqualTo("encodedPassword123");
    }

    @Test
    void saveUser_userAlreadyExists() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.saveUser(testUserDtoIO))
                .isInstanceOf(AlreadyExistsException.class);
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void getAllUsers_success() {
        // Arrange
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto);

        // Act
        Page<UserDto> result = userService.getAllUsers(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findAll(pageable);
    }

    @Test
    void getAllUsers_emptyResult() {
        // Arrange
        Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(userRepository.findAll(pageable)).thenReturn(emptyPage);

        // Act
        Page<UserDto> result = userService.getAllUsers(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(userRepository).findAll(pageable);
    }

    @Test
    void getAllUsersByRole_success() {
        // Arrange
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);
        when(userRepository.findAllByRole(UserRole.CUSTOMER, pageable)).thenReturn(userPage);
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto);

        // Act
        Page<UserDto> result = userService.getAllUsersByRole("CUSTOMER", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRole()).isEqualTo(UserRole.CUSTOMER);
        verify(userRepository).findAllByRole(UserRole.CUSTOMER, pageable);
    }

    @Test
    void getAllUsersByRole_invalidRole() {
        // Act & Assert
        assertThatThrownBy(() -> userService.getAllUsersByRole("INVALID_ROLE", pageable))
                .isInstanceOf(InvalidValueException.class);
        verify(userRepository, never()).findAllByRole(any(), any());
    }

    @Test
    void getAllUsersByRole_caseInsensitive() {
        // Arrange
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);
        when(userRepository.findAllByRole(UserRole.CUSTOMER, pageable)).thenReturn(userPage);
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto);

        // Act
        Page<UserDto> result = userService.getAllUsersByRole("customer", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findAllByRole(UserRole.CUSTOMER, pageable);
    }

    @Test
    void getUserById_success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.getUserById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findById(1L);
        verify(orderMapper).userToUserDto(testUser);
    }

    @Test
    void getUserById_notFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findById(999L);
    }

    @Test
    void deleteUserById_success() {
        // Arrange
        testUser.setAddresses(Arrays.asList(testAddress));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.deleteUserById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findById(1L);
        verify(orderRepository).updateCustomerAndAddressToNull(testUser);
        verify(addressRepository).deleteAll(testUser.getAddresses());
        verify(userRepository).delete(testUser);
        verify(orderMapper).userToUserDto(testUser);
    }

    @Test
    void deleteUserById_notFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUserById(999L))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findById(999L);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void updateUserById_success() {
        // Arrange
        UserDtoEmployee updateDto = new UserDtoEmployee();
        updateDto.setFirstName("Jane");
        updateDto.setEmail("jane.doe@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.updateUserById(1L, updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).findByEmail("jane.doe@example.com");
        verify(orderMapper).updateUserFromDtoEmployee(updateDto, testUser);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserById_userNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserById(999L, testUserDtoEmployee))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserById_emailAlreadyExists() {
        // Arrange
        UserDtoEmployee updateDto = new UserDtoEmployee();
        updateDto.setEmail("existing@example.com");
        
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserById(1L, updateDto))
                .isInstanceOf(AlreadyExistsException.class);
        verify(userRepository).findById(1L);
        verify(userRepository).findByEmail("existing@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserById_sameEmail() {
        // Arrange
        UserDtoEmployee updateDto = new UserDtoEmployee();
        updateDto.setEmail("john.doe@example.com"); // Same email
        updateDto.setFirstName("UpdatedJohn");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.updateUserById(1L, updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        // No check for existing email since it's the same
        verify(orderMapper).updateUserFromDtoEmployee(updateDto, testUser);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserRoleById_success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.updateUserRoleById(1L, testStringDto);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        verify(orderMapper).userToUserDto(testUser);
        assertThat(testUser.getRole()).isEqualTo(UserRole.WAITER);
    }

    @Test
    void updateUserRoleById_nullRole() {
        // Arrange
        testStringDto.setName(null);

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserRoleById(1L, testStringDto))
                .isInstanceOf(NullValueException.class);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void updateUserRoleById_userNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserRoleById(999L, testStringDto))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRoleById_invalidRole() {
        // Arrange
        testStringDto.setName("INVALID_ROLE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserRoleById(1L, testStringDto))
                .isInstanceOf(InvalidValueException.class);
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRoleById_sameRole() {
        // Arrange
        testStringDto.setName("CUSTOMER"); // Same as the current role
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserRoleById(1L, testStringDto))
                .isInstanceOf(AlreadyHasException.class);
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRoleById_fromEmployeeToCustomer() {
        // Arrange
        testUser.setRole(UserRole.CHEF);
        testUser.setSalary(BigDecimal.valueOf(3000.00));
        testStringDto.setName("CUSTOMER");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.updateUserRoleById(1L, testStringDto);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        assertThat(testUser.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(testUser.getSalary()).isNull(); // Salary should be cleared
    }

    @Test
    void updateUserRoleById_fromEmployeeToEmployee() {
        // Arrange
        testUser.setRole(UserRole.CHEF);
        testUser.setSalary(BigDecimal.valueOf(3000.00));
        testStringDto.setName("WAITER");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.updateUserRoleById(1L, testStringDto);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        assertThat(testUser.getRole()).isEqualTo(UserRole.WAITER);
        assertThat(testUser.getSalary()).isEqualTo(BigDecimal.valueOf(3000.00)); // Salary should remain
    }

    @Test
    void getCustomerByEmail_success() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(orderMapper.userToUserDtoCustomer(testUser)).thenReturn(testUserDtoCustomer);

        // Act
        UserDtoCustomer result = userService.getCustomerByEmail("john.doe@example.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(orderMapper).userToUserDtoCustomer(testUser);
    }

    @Test
    void getCustomerByEmail_notFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getCustomerByEmail("nonexistent@example.com"))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void deleteCustomerByEmail_success() {
        // Arrange
        testUser.setAddresses(Arrays.asList(testAddress));
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser)); // For deleteUserById call
        when(orderMapper.userToUserDtoCustomer(testUser)).thenReturn(testUserDtoCustomer);
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto); // For deleteUserById

        // Act
        UserDtoCustomer result = userService.deleteCustomerByEmail("john.doe@example.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(userRepository).findById(1L); // Called by deleteUserById
        verify(orderMapper).userToUserDtoCustomer(testUser);
        verify(orderMapper).userToUserDto(testUser); // Called by deleteUserById
        // Verify deleteUserById was called (which includes the delete operations)
        verify(orderRepository).updateCustomerAndAddressToNull(testUser);
        verify(addressRepository).deleteAll(testUser.getAddresses());
        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteCustomerByEmail_notFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteCustomerByEmail("nonexistent@example.com"))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(userRepository, never()).delete(any());
    }

    @Test
    void updateCustomerByEmail_success() {
        // Arrange
        UserDtoIO updateDto = new UserDtoIO();
        updateDto.setFirstName("Jane");
        updateDto.setEmail("jane.doe@example.com");
        updateDto.setPassword("SomePassword123"); // Should be ignored

        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("jane.doe@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(orderMapper.userToUserDtoCustomer(testUser)).thenReturn(testUserDtoCustomer);

        // Act
        UserDtoCustomer result = userService.updateCustomerByEmail("john.doe@example.com", updateDto);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(userRepository).findByEmail("jane.doe@example.com");
        verify(orderMapper).updateUserFromDtoIO(updateDto, testUser);
        verify(userRepository).save(testUser);
        // Verify password is set to null before update
        assertThat(updateDto.getPassword()).isNull();
    }

    @Test
    void updateCustomerByEmail_customerNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateCustomerByEmail("nonexistent@example.com", testUserDtoIO))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateEmployeeSalaryById_success() {
        // Arrange
        testUser.setRole(UserRole.CHEF);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.updateEmployeeSalaryById(1L, testBigDecimalDto);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        verify(orderMapper).userToUserDto(testUser);
        assertThat(testUser.getSalary()).isEqualTo(BigDecimal.valueOf(3000.00));
    }

    @Test
    void updateEmployeeSalaryById_nullSalary() {
        // Arrange
        testBigDecimalDto.setDecimal(null);

        // Act & Assert
        assertThatThrownBy(() -> userService.updateEmployeeSalaryById(1L, testBigDecimalDto))
                .isInstanceOf(NullValueException.class);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void updateEmployeeSalaryById_userNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateEmployeeSalaryById(999L, testBigDecimalDto))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateEmployeeSalaryById_notEmployee() {
        // Arrange - user is CUSTOMER, not employee
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateEmployeeSalaryById(1L, testBigDecimalDto))
                .isInstanceOf(InvalidOperationException.class);
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateEmployeeSalaryById_waiterRole() {
        // Arrange
        testUser.setRole(UserRole.WAITER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(orderMapper.userToUserDto(testUser)).thenReturn(testUserDto);

        // Act
        UserDto result = userService.updateEmployeeSalaryById(1L, testBigDecimalDto);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        assertThat(testUser.getSalary()).isEqualTo(BigDecimal.valueOf(3000.00));
    }

    @Test
    void getEmployeeByEmail_success() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(orderMapper.userToUserDtoEmployee(testUser)).thenReturn(testUserDtoEmployee);

        // Act
        UserDtoEmployee result = userService.getEmployeeByEmail("john.doe@example.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(orderMapper).userToUserDtoEmployee(testUser);
    }

    @Test
    void getEmployeeByEmail_notFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getEmployeeByEmail("nonexistent@example.com"))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void changePassword_success() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPassword123", "encodedPassword123")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("newEncodedPassword123");
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(orderMapper.userToUserDtoCustomer(testUser)).thenReturn(testUserDtoCustomer);

        // Act
        UserDtoCustomer result = userService.changePassword("john.doe@example.com", testPasswordChangeDto);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(passwordEncoder).matches("OldPassword123", "encodedPassword123");
        verify(passwordEncoder).encode("NewPassword123");
        verify(userRepository).save(testUser);
        verify(orderMapper).userToUserDtoCustomer(testUser);
        assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword123");
    }

    @Test
    void changePassword_userNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.changePassword("nonexistent@example.com", testPasswordChangeDto))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void changePassword_invalidCurrentPassword() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPassword123", "encodedPassword123")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.changePassword("john.doe@example.com", testPasswordChangeDto))
                .isInstanceOf(InvalidValueException.class);
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(passwordEncoder).matches("OldPassword123", "encodedPassword123");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_samePassword() {
        // Arrange
        testPasswordChangeDto.setNewPassword("OldPassword123"); // Same as current
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPassword123", "encodedPassword123")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.changePassword("john.doe@example.com", testPasswordChangeDto))
                .isInstanceOf(InvalidValueException.class);
        verify(userRepository).findByEmail("john.doe@example.com");
        verify(passwordEncoder).matches("OldPassword123", "encodedPassword123");
        verify(userRepository, never()).save(any());
    }
}
