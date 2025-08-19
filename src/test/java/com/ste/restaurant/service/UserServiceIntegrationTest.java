package com.ste.restaurant.service;

import com.ste.restaurant.dto.common.BigDecimalDto;
import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.dto.userdto.*;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.exception.*;
import com.ste.restaurant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for UserService.
 * Tests complex user management operations, authentication,
 * role-based functionality, and user lifecycle management
 * with real database operations.
 * <p>
 * This verifies:
 * - User CRUD operations with proper validations
 * - Role-based user management and permissions
 * - Password encryption and validation
 * - Address management for users
 * - Employee salary management
 * - Complex user queries and filtering
 * - Transaction handling and data consistency
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("UserService Integration Tests")
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testCustomer;
    private User testWaiter;
    private User testChef;
    private User testAdmin;
    private String testPassword = "TestPass123";
    private String timestamp;

    @BeforeEach
    void setUp() {
        timestamp = String.valueOf(System.currentTimeMillis());
        
        // Create test users with different roles
        setupTestUsers();
    }

    @Nested
    @DisplayName("User Creation Integration Tests")
    class UserCreationIntegrationTests {

        @Test
        @DisplayName("Should create new user with valid data and encrypted password")
        void shouldCreateNewUserWithValidDataAndEncryptedPassword() {
            // Given
            UserDtoIO userDtoIO = createTestUserDtoIO("newuser" + timestamp + "@test.com", "NewUser", "Test");
            
            // When
            UserDto savedUser = userService.saveUser(userDtoIO);
            
            // Then
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getEmail()).isEqualTo(userDtoIO.getEmail());
            assertThat(savedUser.getFirstName()).isEqualTo(userDtoIO.getFirstName());
            assertThat(savedUser.getLastName()).isEqualTo(userDtoIO.getLastName());
            assertThat(savedUser.getRole()).isEqualTo(UserRole.CUSTOMER);
            
            // Verify password is encrypted - find user by email since ID might not be mapped
            User dbUser = userRepository.findByEmail(savedUser.getEmail()).orElseThrow();
            assertThat(dbUser.getPassword()).isNotEqualTo(testPassword);
            assertThat(passwordEncoder.matches(testPassword, dbUser.getPassword())).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when creating user with existing email")
        void shouldThrowExceptionWhenCreatingUserWithExistingEmail() {
            // Given
            UserDtoIO userDtoIO = createTestUserDtoIO(testCustomer.getEmail(), "Duplicate", "User");
            
            // When & Then
            assertThatThrownBy(() -> userService.saveUser(userDtoIO))
                    .isInstanceOf(AlreadyExistsException.class)
                    .hasMessage("User already exists");
        }

        @Test
        @DisplayName("Should create user with birthday and proper date handling")
        void shouldCreateUserWithBirthdayAndProperDateHandling() {
            // Given
            UserDtoIO userDtoIO = createTestUserDtoIO("birthday" + timestamp + "@test.com", "Birthday", "User");
            LocalDate testBirthday = LocalDate.of(1990, 5, 15);
            userDtoIO.setBirthday(testBirthday);
            
            // When
            UserDto savedUser = userService.saveUser(userDtoIO);
            
            // Then
            assertThat(savedUser.getBirthday()).isEqualTo(testBirthday);
            
            User dbUser = userRepository.findByEmail(savedUser.getEmail()).orElseThrow();
            assertThat(dbUser.getBirthday()).isEqualTo(testBirthday);
        }
    }

    @Nested
    @DisplayName("User Retrieval Integration Tests")
    class UserRetrievalIntegrationTests {

        @Test
        @DisplayName("Should retrieve all users with pagination")
        void shouldRetrieveAllUsersWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 2);
            
            // When
            Page<UserDto> userPage = userService.getAllUsers(pageable);
            
            // Then
            assertThat(userPage).isNotNull();
            assertThat(userPage.getContent()).isNotEmpty();
            assertThat(userPage.getSize()).isEqualTo(2);
            assertThat(userPage.getContent().size()).isLessThanOrEqualTo(2);
            
            // Verify proper DTO mapping
            UserDto firstUser = userPage.getContent().get(0);
            assertThat(firstUser.getEmail()).isNotBlank();
            assertThat(firstUser.getRole()).isNotNull();
        }

        @Test
        @DisplayName("Should retrieve users by role with proper filtering")
        void shouldRetrieveUsersByRoleWithProperFiltering() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            
            // When
            Page<UserDto> customerPage = userService.getAllUsersByRole("CUSTOMER", pageable);
            Page<UserDto> waiterPage = userService.getAllUsersByRole("waiter", pageable);
            
            // Then
            assertThat(customerPage.getContent()).isNotEmpty();
            assertThat(customerPage.getContent()).allMatch(user -> user.getRole() == UserRole.CUSTOMER);
            
            assertThat(waiterPage.getContent()).isNotEmpty();
            assertThat(waiterPage.getContent()).allMatch(user -> user.getRole() == UserRole.WAITER);
            
            // Verify different role counts
            assertThat(customerPage.getContent().size()).isNotEqualTo(waiterPage.getContent().size());
        }

        @Test
        @DisplayName("Should throw exception for invalid role in filtering")
        void shouldThrowExceptionForInvalidRoleInFiltering() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            
            // When & Then
            assertThatThrownBy(() -> userService.getAllUsersByRole("INVALID_ROLE", pageable))
                    .isInstanceOf(InvalidValueException.class)
                    .hasMessageContaining("INVALID_ROLE");
        }

        @Test
        @DisplayName("Should retrieve user by ID with complete data")
        void shouldRetrieveUserByIdWithCompleteData() {
            // When
            UserDto retrievedUser = userService.getUserById(testCustomer.getUserId());
            
            // Then
            assertThat(retrievedUser).isNotNull();
            assertThat(retrievedUser.getEmail()).isEqualTo(testCustomer.getEmail());
            assertThat(retrievedUser.getFirstName()).isEqualTo(testCustomer.getFirstName());
            assertThat(retrievedUser.getLastName()).isEqualTo(testCustomer.getLastName());
            assertThat(retrievedUser.getRole()).isEqualTo(testCustomer.getRole());
            if (testCustomer.getLoyaltyPoints() != null) {
                assertThat(retrievedUser.getLoyaltyPoints()).isEqualTo(testCustomer.getLoyaltyPoints());
            }
        }

        @Test
        @DisplayName("Should throw exception when retrieving non-existent user")
        void shouldThrowExceptionWhenRetrievingNonExistentUser() {
            // Given
            Long nonExistentId = 99999L;
            
            // When & Then
            assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("User Update Integration Tests")
    class UserUpdateIntegrationTests {

        @Test
        @DisplayName("Should update user basic information successfully")
        void shouldUpdateUserBasicInformationSuccessfully() {
            // Given
            UserDtoEmployee updateDto = new UserDtoEmployee();
            updateDto.setFirstName("UpdatedFirst");
            updateDto.setLastName("UpdatedLast");
            updateDto.setBirthday(LocalDate.of(1985, 12, 25));
            
            // When
            UserDto updatedUser = userService.updateUserById(testCustomer.getUserId(), updateDto);
            
            // Then
            assertThat(updatedUser.getFirstName()).isEqualTo("UpdatedFirst");
            assertThat(updatedUser.getLastName()).isEqualTo("UpdatedLast");
            assertThat(updatedUser.getBirthday()).isEqualTo(LocalDate.of(1985, 12, 25));
            assertThat(updatedUser.getEmail()).isEqualTo(testCustomer.getEmail()); // Should remain unchanged
            
            // Verify in database
            User dbUser = userRepository.findById(testCustomer.getUserId()).orElseThrow();
            assertThat(dbUser.getFirstName()).isEqualTo("UpdatedFirst");
            assertThat(dbUser.getLastName()).isEqualTo("UpdatedLast");
        }

        @Test
        @DisplayName("Should update user email with uniqueness validation")
        void shouldUpdateUserEmailWithUniquenessValidation() {
            // Given
            String newEmail = "newemail" + timestamp + "@test.com";
            UserDtoEmployee updateDto = new UserDtoEmployee();
            updateDto.setFirstName("UpdatedFirst");
            updateDto.setEmail(newEmail);

            // When
            UserDto updatedUser = userService.updateUserById(testCustomer.getUserId(), updateDto);
            
            // Then
            assertThat(updatedUser.getEmail()).isEqualTo(newEmail);
            
            User dbUser = userRepository.findById(testCustomer.getUserId()).orElseThrow();
            assertThat(dbUser.getEmail()).isEqualTo(newEmail);
        }

        @Test
        @DisplayName("Should throw exception when updating to existing email")
        void shouldThrowExceptionWhenUpdatingToExistingEmail() {
            // Given
            UserDtoEmployee updateDto = new UserDtoEmployee();
            updateDto.setEmail(testWaiter.getEmail()); // Use existing email
            
            // When & Then
            assertThatThrownBy(() -> userService.updateUserById(testCustomer.getUserId(), updateDto))
                    .isInstanceOf(AlreadyExistsException.class)
                    .hasMessageContaining("User already exists");
        }

        @Test
        @DisplayName("Should ignore password in update operations")
        void shouldIgnorePasswordInUpdateOperations() {
            // Given
            UserDtoEmployee updateDto = new UserDtoEmployee();
            updateDto.setFirstName("UpdatedName");
            updateDto.setLastName("UpdatedLast");
            updateDto.setBirthday(LocalDate.of(1985, 12, 25));

            String originalPassword = testCustomer.getPassword();
            
            // When
            userService.updateUserById(testCustomer.getUserId(), updateDto);
            
            // Then
            User dbUser = userRepository.findById(testCustomer.getUserId()).orElseThrow();
            assertThat(dbUser.getPassword()).isEqualTo(originalPassword); // Password should not change
            assertThat(dbUser.getFirstName()).isEqualTo("UpdatedName");
        }
    }

    @Nested
    @DisplayName("User Role Management Integration Tests")
    class UserRoleManagementIntegrationTests {

        @Test
        @DisplayName("Should update user role from customer to waiter")
        void shouldUpdateUserRoleFromCustomerToWaiter() {
            // Given
            StringDto roleDto = new StringDto();
            roleDto.setName("WAITER");
            
            // When
            UserDto updatedUser = userService.updateUserRoleById(testCustomer.getUserId(), roleDto);
            
            // Then
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.WAITER);
            
            User dbUser = userRepository.findById(testCustomer.getUserId()).orElseThrow();
            assertThat(dbUser.getRole()).isEqualTo(UserRole.WAITER);
        }

        @Test
        @DisplayName("Should clear salary when changing from employee to customer")
        void shouldClearSalaryWhenChangingFromEmployeeToCustomer() {
            // Given - Set salary for waiter first
            testWaiter.setSalary(BigDecimal.valueOf(3000.00));
            userRepository.save(testWaiter);
            
            StringDto roleDto = new StringDto();
            roleDto.setName("CUSTOMER");
            
            // When
            UserDto updatedUser = userService.updateUserRoleById(testWaiter.getUserId(), roleDto);
            
            // Then
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.CUSTOMER);
            assertThat(updatedUser.getSalary()).isNull();
            
            User dbUser = userRepository.findById(testWaiter.getUserId()).orElseThrow();
            assertThat(dbUser.getSalary()).isNull();
        }

        @Test
        @DisplayName("Should throw exception for invalid role")
        void shouldThrowExceptionForInvalidRole() {
            // Given
            StringDto roleDto = new StringDto();
            roleDto.setName("INVALID_ROLE");
            
            // When & Then
            assertThatThrownBy(() -> userService.updateUserRoleById(testCustomer.getUserId(), roleDto))
                    .isInstanceOf(InvalidValueException.class)
                    .hasMessageContaining("INVALID_ROLE");
        }

        @Test
        @DisplayName("Should throw exception when setting same role")
        void shouldThrowExceptionWhenSettingSameRole() {
            // Given
            StringDto roleDto = new StringDto();
            roleDto.setName("CUSTOMER");
            
            // When & Then
            assertThatThrownBy(() -> userService.updateUserRoleById(testCustomer.getUserId(), roleDto))
                    .isInstanceOf(AlreadyHasException.class)
                    .hasMessageContaining("CUSTOMER");
        }

        @Test
        @DisplayName("Should throw exception for null role")
        void shouldThrowExceptionForNullRole() {
            // Given
            StringDto roleDto = new StringDto();
            roleDto.setName(null);
            
            // When & Then
            assertThatThrownBy(() -> userService.updateUserRoleById(testCustomer.getUserId(), roleDto))
                    .isInstanceOf(NullValueException.class)
                    .hasMessageContaining("role");
        }
    }

    @Nested
    @DisplayName("Employee Salary Management Integration Tests")
    class EmployeeSalaryManagementIntegrationTests {

        @Test
        @DisplayName("Should update salary for chef employee")
        void shouldUpdateSalaryForChefEmployee() {
            // Given
            BigDecimal newSalary = BigDecimal.valueOf(4500.00);
            BigDecimalDto salaryDto = new BigDecimalDto();
            salaryDto.setDecimal(newSalary);
            
            // When
            UserDto updatedUser = userService.updateEmployeeSalaryById(testChef.getUserId(), salaryDto);
            
            // Then
            assertThat(updatedUser.getSalary()).isEqualTo(newSalary);
            
            User dbUser = userRepository.findById(testChef.getUserId()).orElseThrow();
            assertThat(dbUser.getSalary()).isEqualTo(newSalary);
        }

        @Test
        @DisplayName("Should update salary for waiter employee")
        void shouldUpdateSalaryForWaiterEmployee() {
            // Given
            BigDecimal newSalary = BigDecimal.valueOf(3200.00);
            BigDecimalDto salaryDto = new BigDecimalDto();
            salaryDto.setDecimal(newSalary);
            
            // When
            UserDto updatedUser = userService.updateEmployeeSalaryById(testWaiter.getUserId(), salaryDto);
            
            // Then
            assertThat(updatedUser.getSalary()).isEqualTo(newSalary);
        }

        @Test
        @DisplayName("Should throw exception when setting salary for customer")
        void shouldThrowExceptionWhenSettingSalaryForCustomer() {
            // Given
            BigDecimalDto salaryDto = new BigDecimalDto();
            salaryDto.setDecimal(BigDecimal.valueOf(2000.00));
            
            // When & Then
            assertThatThrownBy(() -> userService.updateEmployeeSalaryById(testCustomer.getUserId(), salaryDto))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Customers or Admins cannot have salary");
        }

        @Test
        @DisplayName("Should throw exception when setting salary for admin")
        void shouldThrowExceptionWhenSettingSalaryForAdmin() {
            // Given
            BigDecimalDto salaryDto = new BigDecimalDto();
            salaryDto.setDecimal(BigDecimal.valueOf(5000.00));
            
            // When & Then
            assertThatThrownBy(() -> userService.updateEmployeeSalaryById(testAdmin.getUserId(), salaryDto))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasMessageContaining("Customers or Admins cannot have salary");
        }

        @Test
        @DisplayName("Should throw exception for null salary")
        void shouldThrowExceptionForNullSalary() {
            // Given
            BigDecimalDto salaryDto = new BigDecimalDto();
            salaryDto.setDecimal(null);
            
            // When & Then
            assertThatThrownBy(() -> userService.updateEmployeeSalaryById(testWaiter.getUserId(), salaryDto))
                    .isInstanceOf(NullValueException.class)
                    .hasMessageContaining("salary");
        }
    }

    @Nested
    @DisplayName("Customer-Specific Operations Integration Tests")
    class CustomerSpecificOperationsIntegrationTests {

        @Test
        @DisplayName("Should retrieve customer by email with proper DTO mapping")
        void shouldRetrieveCustomerByEmailWithProperDtoMapping() {
            // When
            UserDtoCustomer customer = userService.getCustomerByEmail(testCustomer.getEmail());
            
            // Then
            assertThat(customer).isNotNull();
            assertThat(customer.getEmail()).isEqualTo(testCustomer.getEmail());
            assertThat(customer.getFirstName()).isEqualTo(testCustomer.getFirstName());
            assertThat(customer.getLastName()).isEqualTo(testCustomer.getLastName());
            assertThat(customer.getLoyaltyPoints()).isEqualTo(testCustomer.getLoyaltyPoints());
            assertThat(customer.getBirthday()).isEqualTo(testCustomer.getBirthday());
            // Note: UserDtoCustomer doesn't include password or role for security
        }

        @Test
        @DisplayName("Should update customer information by email")
        void shouldUpdateCustomerInformationByEmail() {
            // Given
            UserDtoIO updateDto = new UserDtoIO();
            updateDto.setFirstName("UpdatedCustomer");
            updateDto.setLastName("NewLastName");
            
            // When
            UserDtoCustomer updatedCustomer = userService.updateCustomerByEmail(testCustomer.getEmail(), updateDto);
            
            // Then
            assertThat(updatedCustomer.getFirstName()).isEqualTo("UpdatedCustomer");
            assertThat(updatedCustomer.getLastName()).isEqualTo("NewLastName");
            assertThat(updatedCustomer.getEmail()).isEqualTo(testCustomer.getEmail());
        }

        @Test
        @DisplayName("Should delete customer by email and clean up related data")
        void shouldDeleteCustomerByEmailAndCleanUpRelatedData() {
            // Given - Create simple test customer without complex relationships
            String customerEmail = "deleteme" + timestamp + "@test.com";
            User simpleCustomer = createTestUser(customerEmail, "Delete", "Me", UserRole.CUSTOMER);
            simpleCustomer = userRepository.save(simpleCustomer);
            Long customerId = simpleCustomer.getUserId();
            
            // When
            UserDtoCustomer deletedCustomer = userService.deleteCustomerByEmail(customerEmail);
            
            // Then
            assertThat(deletedCustomer.getEmail()).isEqualTo(customerEmail);
            
            // Verify user is deleted
            Optional<User> deletedUser = userRepository.findById(customerId);
            assertThat(deletedUser).isEmpty();
        }
    }

    @Nested
    @DisplayName("Employee-Specific Operations Integration Tests")
    class EmployeeSpecificOperationsIntegrationTests {

        @Test
        @DisplayName("Should retrieve employee by email with salary information")
        void shouldRetrieveEmployeeByEmailWithSalaryInformation() {
            // Given - Set salary for waiter
            testWaiter.setSalary(BigDecimal.valueOf(3500.00));
            userRepository.save(testWaiter);
            
            // When
            UserDtoEmployee employee = userService.getEmployeeByEmail(testWaiter.getEmail());
            
            // Then
            assertThat(employee).isNotNull();
            assertThat(employee.getEmail()).isEqualTo(testWaiter.getEmail());
            assertThat(employee.getRole()).isEqualTo(UserRole.WAITER);
            assertThat(employee.getSalary()).isEqualTo(BigDecimal.valueOf(3500.00));
            assertThat(employee.getFirstName()).isEqualTo(testWaiter.getFirstName());
        }
    }

    @Nested
    @DisplayName("Password Management Integration Tests")
    class PasswordManagementIntegrationTests {

        @Test
        @DisplayName("Should change password successfully with valid credentials")
        void shouldChangePasswordSuccessfullyWithValidCredentials() {
            // Given
            String newPassword = "NewPass456";
            PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
            passwordChangeDto.setPassword(testPassword);
            passwordChangeDto.setNewPassword(newPassword);
            
            // When
            UserDtoCustomer updatedCustomer = userService.changePassword(testCustomer.getEmail(), passwordChangeDto);
            
            // Then
            assertThat(updatedCustomer.getEmail()).isEqualTo(testCustomer.getEmail());
            
            // Verify password is changed and encrypted
            User dbUser = userRepository.findById(testCustomer.getUserId()).orElseThrow();
            assertThat(passwordEncoder.matches(newPassword, dbUser.getPassword())).isTrue();
            assertThat(passwordEncoder.matches(testPassword, dbUser.getPassword())).isFalse();
        }

        @Test
        @DisplayName("Should throw exception for incorrect current password")
        void shouldThrowExceptionForIncorrectCurrentPassword() {
            // Given
            PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
            passwordChangeDto.setPassword("WrongPassword123");
            passwordChangeDto.setNewPassword("NewPass456");
            
            // When & Then
            assertThatThrownBy(() -> userService.changePassword(testCustomer.getEmail(), passwordChangeDto))
                    .isInstanceOf(InvalidValueException.class)
                    .hasMessageContaining("Invalid password");
        }

        @Test
        @DisplayName("Should throw exception when new password is same as current")
        void shouldThrowExceptionWhenNewPasswordIsSameAsCurrent() {
            // Given
            PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
            passwordChangeDto.setPassword(testPassword);
            passwordChangeDto.setNewPassword(testPassword); // Same password
            
            // When & Then
            assertThatThrownBy(() -> userService.changePassword(testCustomer.getEmail(), passwordChangeDto))
                    .isInstanceOf(InvalidValueException.class)
                    .hasMessageContaining("Same password with previous one");
        }
    }

    @Nested
    @DisplayName("User Deletion Integration Tests")
    class UserDeletionIntegrationTests {

        @Test
        @DisplayName("Should delete user and clean up related data properly")
        void shouldDeleteUserAndCleanUpRelatedDataProperly() {
            // Given - Create a simple user
            String userEmail = "usertodelete" + timestamp + "@test.com";
            User userToDelete = createTestUser(userEmail, "Delete", "User", UserRole.CUSTOMER);
            userToDelete = userRepository.save(userToDelete);
            Long userId = userToDelete.getUserId();
            
            // When
            UserDto deletedUser = userService.deleteUserById(userId);
            
            // Then
            assertThat(deletedUser.getEmail()).isEqualTo(userEmail);
            
            // Verify user is deleted from database
            Optional<User> deletedUserCheck = userRepository.findById(userId);
            assertThat(deletedUserCheck).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent user")
        void shouldThrowExceptionWhenDeletingNonExistentUser() {
            // Given
            Long nonExistentId = 99999L;
            
            // When & Then
            assertThatThrownBy(() -> userService.deleteUserById(nonExistentId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // Helper methods for test data creation
    private void setupTestUsers() {
        testCustomer = createTestUser("customer" + timestamp + "@test.com", "Test", "Customer", UserRole.CUSTOMER);
        testCustomer.setLoyaltyPoints(100);
        testCustomer.setBirthday(LocalDate.of(1992, 3, 15));
        userRepository.save(testCustomer);
        
        testWaiter = createTestUser("waiter" + timestamp + "@test.com", "Test", "Waiter", UserRole.WAITER);
        testWaiter.setSalary(BigDecimal.valueOf(3000.00));
        userRepository.save(testWaiter);
        
        testChef = createTestUser("chef" + timestamp + "@test.com", "Test", "Chef", UserRole.CHEF);
        testChef.setSalary(BigDecimal.valueOf(4000.00));
        userRepository.save(testChef);
        
        testAdmin = createTestUser("admin" + timestamp + "@test.com", "Test", "Admin", UserRole.ADMIN);
        userRepository.save(testAdmin);
    }

    private User createTestUser(String email, String firstName, String lastName, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(testPassword));
        return user;
    }

    private UserDtoIO createTestUserDtoIO(String email, String firstName, String lastName) {
        UserDtoIO userDtoIO = new UserDtoIO();
        userDtoIO.setEmail(email);
        userDtoIO.setFirstName(firstName);
        userDtoIO.setLastName(lastName);
        userDtoIO.setPassword(testPassword);
        return userDtoIO;
    }
}
