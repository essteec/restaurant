package com.ste.restaurant.integration;

import com.ste.restaurant.entity.Address;
import com.ste.restaurant.entity.User;
import com.ste.restaurant.entity.UserRole;
import com.ste.restaurant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User createTestUser(String firstName, String lastName, String email, UserRole role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setRole(role);
        user.setPassword("password123");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setLoyaltyPoints(100);
        if (role == UserRole.WAITER || role == UserRole.CHEF || role == UserRole.ADMIN) {
            user.setSalary(new BigDecimal("50000.00"));
        }
        return user;
    }

    private Address createTestAddress(String name) {
        Address address = new Address();
        address.setName(name);
        address.setCountry("Turkey");
        address.setCity("Istanbul");
        address.setProvince("Istanbul");
        address.setDistrict("Besiktas");
        address.setStreet("Test Street");
        address.setApartment("1A");
        address.setDescription("Test address");
        return address;
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve user successfully")
        void shouldSaveAndRetrieveUser() {
            // Given
            User user = createTestUser("John", "Doe", "john.doe@test.com", UserRole.CUSTOMER);

            // When
            User saved = userRepository.save(user);
            entityManager.flush();
            entityManager.clear();

            Optional<User> retrieved = userRepository.findById(saved.getUserId());

            // Then
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getFirstName()).isEqualTo("John");
            assertThat(retrieved.get().getLastName()).isEqualTo("Doe");
            assertThat(retrieved.get().getEmail()).isEqualTo("john.doe@test.com");
            assertThat(retrieved.get().getRole()).isEqualTo(UserRole.CUSTOMER);
            assertThat(retrieved.get().getLoyaltyPoints()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should update user successfully")
        void shouldUpdateUser() {
            // Given
            User user = createTestUser("Jane", "Smith", "jane.smith@test.com", UserRole.CUSTOMER);
            User saved = userRepository.save(user);
            entityManager.flush();
            entityManager.clear();

            // When
            saved.setFirstName("Jane Updated");
            saved.setLoyaltyPoints(200);
            User updated = userRepository.save(saved);
            entityManager.flush();
            entityManager.clear();

            User retrieved = userRepository.findById(updated.getUserId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getFirstName()).isEqualTo("Jane Updated");
            assertThat(retrieved.getLoyaltyPoints()).isEqualTo(200);
            assertThat(retrieved.getEmail()).isEqualTo("jane.smith@test.com");
        }

        @Test
        @DisplayName("Should delete user successfully")
        void shouldDeleteUser() {
            // Given
            User user = createTestUser("Bob", "Wilson", "bob.wilson@test.com", UserRole.CUSTOMER);
            User saved = userRepository.save(user);
            entityManager.flush();
            Long userId = saved.getUserId();

            // When
            userRepository.deleteById(userId);
            entityManager.flush();

            // Then
            Optional<User> retrieved = userRepository.findById(userId);
            assertThat(retrieved).isEmpty();
        }

        @Test
        @DisplayName("Should return all users")
        void shouldReturnAllUsers() {
            // Given
            userRepository.saveAll(List.of(
                createTestUser("User1", "Test", "user1@test.com", UserRole.CUSTOMER),
                createTestUser("User2", "Test", "user2@test.com", UserRole.WAITER),
                createTestUser("User3", "Test", "user3@test.com", UserRole.CHEF)
            ));
            entityManager.flush();

            // When
            List<User> users = userRepository.findAll();

            // Then
            assertThat(users).hasSize(3);
            assertThat(users.stream().map(User::getFirstName))
                .containsExactlyInAnyOrder("User1", "User2", "User3");
        }
    }

    @Nested
    @DisplayName("Custom Query Methods")
    class CustomQueryTests {

        @BeforeEach
        void setUp() {
            userRepository.saveAll(List.of(
                createTestUser("Alice", "Admin", "alice@test.com", UserRole.ADMIN),
                createTestUser("Bob", "Waiter", "bob@test.com", UserRole.WAITER),
                createTestUser("Charlie", "Chef", "charlie@test.com", UserRole.CHEF),
                createTestUser("Dave", "Customer", "dave@test.com", UserRole.CUSTOMER),
                createTestUser("Eve", "Customer", "eve@test.com", UserRole.CUSTOMER)
            ));
            entityManager.flush();
        }

        @Test
        @DisplayName("Should find user by email")
        void shouldFindUserByEmail() {
            // When
            Optional<User> found = userRepository.findByEmail("alice@test.com");

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getFirstName()).isEqualTo("Alice");
            assertThat(found.get().getRole()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("Should return empty when email not found")
        void shouldReturnEmptyWhenEmailNotFound() {
            // When
            Optional<User> found = userRepository.findByEmail("nonexistent@test.com");

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should check if email exists")
        void shouldCheckIfEmailExists() {
            // When & Then
            assertThat(userRepository.existsByEmail("bob@test.com")).isTrue();
            assertThat(userRepository.existsByEmail("nonexistent@test.com")).isFalse();
        }

        @Test
        @DisplayName("Should delete user by email")
        void shouldDeleteUserByEmail() {
            // Given
            assertThat(userRepository.existsByEmail("charlie@test.com")).isTrue();

            // When
            userRepository.deleteByEmail("charlie@test.com");
            entityManager.flush();

            // Then
            assertThat(userRepository.existsByEmail("charlie@test.com")).isFalse();
        }

        @Test
        @DisplayName("Should find all users by role")
        void shouldFindAllUsersByRole() {
            // When
            List<User> customers = userRepository.findAllByRole(UserRole.CUSTOMER);
            List<User> waiters = userRepository.findAllByRole(UserRole.WAITER);

            // Then
            assertThat(customers).hasSize(2);
            assertThat(customers.stream().map(User::getFirstName))
                .containsExactlyInAnyOrder("Dave", "Eve");
            
            assertThat(waiters).hasSize(1);
            assertThat(waiters.get(0).getFirstName()).isEqualTo("Bob");
        }

        @Test
        @DisplayName("Should find users by role with pagination")
        void shouldFindUsersByRoleWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 1);

            // When
            Page<User> customersPage = userRepository.findAllByRole(UserRole.CUSTOMER, pageable);

            // Then
            assertThat(customersPage.getTotalElements()).isEqualTo(2);
            assertThat(customersPage.getContent()).hasSize(1);
            assertThat(customersPage.getTotalPages()).isEqualTo(2);
            assertThat(customersPage.isFirst()).isTrue();
            assertThat(customersPage.hasNext()).isTrue();
        }
    }

    @Nested
    @DisplayName("Relationship Tests")
    class RelationshipTests {

        @Test
        @DisplayName("Should save user with addresses")
        void shouldSaveUserWithAddresses() {
            // Given
            User user = createTestUser("John", "Doe", "john@test.com", UserRole.CUSTOMER);
            List<Address> addresses = new ArrayList<>();
            addresses.add(createTestAddress("Home Address"));
            addresses.add(createTestAddress("Work Address"));
            user.setAddresses(addresses);

            // When
            User saved = userRepository.save(user);
            entityManager.flush();
            entityManager.clear();

            User retrieved = userRepository.findById(saved.getUserId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getAddresses()).hasSize(2);
            assertThat(retrieved.getAddresses().stream().map(Address::getName))
                .containsExactlyInAnyOrder("Home Address", "Work Address");
        }

        @Test
        @DisplayName("Should delete addresses when user is deleted (orphan removal)")
        void shouldDeleteAddressesWhenUserDeleted() {
            // Given
            User user = createTestUser("Test", "User", "test@test.com", UserRole.CUSTOMER);
            List<Address> addresses = new ArrayList<>();
            addresses.add(createTestAddress("Address 1"));
            addresses.add(createTestAddress("Address 2"));
            user.setAddresses(addresses);

            User saved = userRepository.save(user);
            entityManager.flush();
            
            // Verify addresses exist
            Long userId = saved.getUserId();
            List<Address> savedAddresses = entityManager.getEntityManager()
                .createQuery("SELECT a FROM Address a WHERE a.name IN :names", Address.class)
                .setParameter("names", List.of("Address 1", "Address 2"))
                .getResultList();
            assertThat(savedAddresses).hasSize(2);

            // When
            userRepository.deleteById(userId);
            entityManager.flush();

            // Then
            List<Address> remainingAddresses = entityManager.getEntityManager()
                .createQuery("SELECT a FROM Address a WHERE a.name IN :names", Address.class)
                .setParameter("names", List.of("Address 1", "Address 2"))
                .getResultList();
            assertThat(remainingAddresses).isEmpty();
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should enforce unique email constraint")
        void shouldEnforceUniqueEmailConstraint() {
            // Given
            User user1 = createTestUser("User1", "Test", "duplicate@test.com", UserRole.CUSTOMER);
            User user2 = createTestUser("User2", "Test", "duplicate@test.com", UserRole.WAITER);

            userRepository.save(user1);
            entityManager.flush();

            // When & Then
            assertThatThrownBy(() -> {
                userRepository.save(user2);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Should handle null email gracefully")
        void shouldHandleNullEmail() {
            // Given
            User user = createTestUser("Test", "User", null, UserRole.CUSTOMER);

            // When & Then
            assertThatThrownBy(() -> {
                userRepository.save(user);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Should handle default role correctly")
        void shouldHandleDefaultRole() {
            // Given
            User user = new User();
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEmail("test@test.com");
            // Note: not setting role explicitly, should default to CUSTOMER

            // When
            User saved = userRepository.save(user);
            entityManager.flush();
            entityManager.clear();

            User retrieved = userRepository.findById(saved.getUserId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getRole()).isEqualTo(UserRole.CUSTOMER);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle users with different roles and salaries")
        void shouldHandleUsersWithDifferentRolesAndSalaries() {
            // Given
            User customer = createTestUser("Customer", "User", "customer@test.com", UserRole.CUSTOMER);
            customer.setSalary(null); // Customers shouldn't have salary
            
            User waiter = createTestUser("Waiter", "User", "waiter@test.com", UserRole.WAITER);
            waiter.setSalary(new BigDecimal("45000.00"));

            // When
            User savedCustomer = userRepository.save(customer);
            User savedWaiter = userRepository.save(waiter);
            entityManager.flush();
            entityManager.clear();

            // Then
            User retrievedCustomer = userRepository.findById(savedCustomer.getUserId()).orElse(null);
            User retrievedWaiter = userRepository.findById(savedWaiter.getUserId()).orElse(null);

            assertThat(retrievedCustomer.getSalary()).isNull();
            assertThat(retrievedWaiter.getSalary()).isEqualTo(new BigDecimal("45000.00"));
        }

        @Test
        @DisplayName("Should handle special characters in names")
        void shouldHandleSpecialCharactersInNames() {
            // Given
            User user = createTestUser("Müslüm", "Öztürk", "test@test.com", UserRole.CUSTOMER);

            // When
            User saved = userRepository.save(user);
            entityManager.flush();
            entityManager.clear();

            User retrieved = userRepository.findById(saved.getUserId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getFirstName()).isEqualTo("Müslüm");
            assertThat(retrieved.getLastName()).isEqualTo("Öztürk");
        }

        @Test
        @DisplayName("Should handle zero and negative loyalty points")
        void shouldHandleZeroAndNegativeLoyaltyPoints() {
            // Given
            User user1 = createTestUser("User1", "Test", "user1@test.com", UserRole.CUSTOMER);
            user1.setLoyaltyPoints(0);
            
            User user2 = createTestUser("User2", "Test", "user2@test.com", UserRole.CUSTOMER);
            user2.setLoyaltyPoints(-50);

            // When
            User saved1 = userRepository.save(user1);
            User saved2 = userRepository.save(user2);
            entityManager.flush();
            entityManager.clear();

            // Then
            User retrieved1 = userRepository.findById(saved1.getUserId()).orElse(null);
            User retrieved2 = userRepository.findById(saved2.getUserId()).orElse(null);

            assertThat(retrieved1.getLoyaltyPoints()).isEqualTo(0);
            assertThat(retrieved2.getLoyaltyPoints()).isEqualTo(-50);
        }

        @Test
        @DisplayName("Should handle null and empty string fields")
        void shouldHandleNullAndEmptyStringFields() {
            // Given
            User user = new User();
            user.setEmail("empty@test.com");
            user.setFirstName(""); // Empty string
            user.setLastName(null); // Null
            user.setPassword("");

            // When
            User saved = userRepository.save(user);
            entityManager.flush();
            entityManager.clear();

            User retrieved = userRepository.findById(saved.getUserId()).orElse(null);

            // Then
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getFirstName()).isEmpty();
            assertThat(retrieved.getLastName()).isNull();
            assertThat(retrieved.getPassword()).isEmpty();
        }

        @Test
        @DisplayName("Should handle future and past birthdates")
        void shouldHandleFutureAndPastBirthdates() {
            // Given
            User user1 = createTestUser("Past", "User", "past@test.com", UserRole.CUSTOMER);
            user1.setBirthday(LocalDate.of(1950, 1, 1));
            
            User user2 = createTestUser("Future", "User", "future@test.com", UserRole.CUSTOMER);
            user2.setBirthday(LocalDate.of(2030, 12, 31));

            // When
            User saved1 = userRepository.save(user1);
            User saved2 = userRepository.save(user2);
            entityManager.flush();
            entityManager.clear();

            // Then
            User retrieved1 = userRepository.findById(saved1.getUserId()).orElse(null);
            User retrieved2 = userRepository.findById(saved2.getUserId()).orElse(null);

            assertThat(retrieved1.getBirthday()).isEqualTo(LocalDate.of(1950, 1, 1));
            assertThat(retrieved2.getBirthday()).isEqualTo(LocalDate.of(2030, 12, 31));
        }
    }
}
