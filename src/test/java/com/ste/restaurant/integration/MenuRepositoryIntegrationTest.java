package com.ste.restaurant.integration;

import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.entity.Menu;
import com.ste.restaurant.repository.MenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MenuRepository.
 * Tests the repository layer with real database operations using H2 in-memory database.
 */
@DataJpaTest
@DisplayName("Menu Repository Integration Tests")
class MenuRepositoryIntegrationTest {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private TestEntityManager entityManager;

    // Test data
    private Menu testMenu1;
    private Menu testMenu2;
    private Menu testMenu3;
    private FoodItem testFoodItem1;
    private FoodItem testFoodItem2;
    private FoodItem testFoodItem3;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    void setupTestData() {
        // Create test menus without relationships first
        testMenu1 = createTestMenu("Lunch Menu", "Lunch specials", true);
        testMenu2 = createTestMenu("Dinner Menu", "Evening dining", true);
        testMenu3 = createTestMenu("Breakfast Menu", "Morning meals", false);

        // Persist menus
        entityManager.persistAndFlush(testMenu1);
        entityManager.persistAndFlush(testMenu2);
        entityManager.persistAndFlush(testMenu3);

        // Create test food items without menus first
        testFoodItem1 = createTestFoodItem("Pizza", BigDecimal.valueOf(15.99));
        testFoodItem2 = createTestFoodItem("Burger", BigDecimal.valueOf(12.99));
        testFoodItem3 = createTestFoodItem("Salad", BigDecimal.valueOf(8.99));

        // Persist food items
        entityManager.persistAndFlush(testFoodItem1);
        entityManager.persistAndFlush(testFoodItem2);
        entityManager.persistAndFlush(testFoodItem3);
        
        // Now set up relationships after both entities are persisted
        testFoodItem1.getCategories().clear(); // Clear any existing categories
        testMenu1.getFoodItems().add(testFoodItem1);
        testMenu1.getFoodItems().add(testFoodItem2);
        
        testMenu2.getFoodItems().add(testFoodItem2);
        testMenu2.getFoodItems().add(testFoodItem3);
        
        // testMenu3 (Breakfast) has no food items for testing empty cases
        
        // Final flush to persist relationships
        entityManager.flush();
        entityManager.clear();
    }

    private Menu createTestMenu(String name, String description, boolean active) {
        Menu menu = new Menu();
        menu.setMenuName(name);
        menu.setDescription(description);
        menu.setActive(active);
        return menu;
    }

    private FoodItem createTestFoodItem(String name, BigDecimal price) {
        FoodItem foodItem = new FoodItem();
        foodItem.setFoodName(name);
        foodItem.setDescription("Test description for " + name);
        foodItem.setPrice(price);
        foodItem.setImage("test-image.jpg");
        return foodItem;
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve menu")
        void shouldSaveAndRetrieveMenu() {
            // Given
            Menu newMenu = createTestMenu("Test Menu", "Test Description", false);
            
            // When
            Menu savedMenu = menuRepository.save(newMenu);
            Menu retrievedMenu = menuRepository.findById(savedMenu.getMenuId()).orElse(null);
            
            // Then
            assertThat(savedMenu).isNotNull();
            assertThat(savedMenu.getMenuId()).isNotNull();
            assertThat(retrievedMenu).isNotNull();
            assertThat(retrievedMenu.getMenuName()).isEqualTo("Test Menu");
            assertThat(retrievedMenu.getDescription()).isEqualTo("Test Description");
            assertThat(retrievedMenu.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should find all menus")
        void shouldFindAllMenus() {
            // When
            List<Menu> allMenus = menuRepository.findAll();
            
            // Then
            assertThat(allMenus).hasSize(3);
            assertThat(allMenus)
                .extracting(Menu::getMenuName)
                .containsExactlyInAnyOrder("Lunch Menu", "Dinner Menu", "Breakfast Menu");
        }

        @Test
        @DisplayName("Should delete menu")
        void shouldDeleteMenu() {
            // Given
            Menu menuToDelete = testMenu3; // Breakfast Menu has no relationships
            Long menuId = menuToDelete.getMenuId();
            
            // When
            menuRepository.delete(menuToDelete);
            Optional<Menu> deletedMenu = menuRepository.findById(menuId);
            
            // Then
            assertThat(deletedMenu).isEmpty();
            
            // Verify remaining menus
            List<Menu> remainingMenus = menuRepository.findAll();
            assertThat(remainingMenus).hasSize(2);
            assertThat(remainingMenus)
                .extracting(Menu::getMenuName)
                .containsExactlyInAnyOrder("Lunch Menu", "Dinner Menu");
        }

        @Test
        @DisplayName("Should update menu")
        void shouldUpdateMenu() {
            // Given
            Menu menuToUpdate = testMenu1;
            
            // When
            menuToUpdate.setDescription("Updated lunch specials");
            menuToUpdate.setActive(false);
            Menu updatedMenu = menuRepository.save(menuToUpdate);
            
            // Then
            assertThat(updatedMenu.getDescription()).isEqualTo("Updated lunch specials");
            assertThat(updatedMenu.isActive()).isFalse();
            assertThat(updatedMenu.getMenuName()).isEqualTo("Lunch Menu"); // Should remain unchanged
        }
    }

    @Nested
    @DisplayName("Custom Query Tests")
    class CustomQueryTests {

        @Test
        @DisplayName("Should find menu by name")
        void shouldFindByMenuName() {
            // When
            Optional<Menu> foundMenu = menuRepository.findByMenuName("Lunch Menu");
            
            // Then
            assertThat(foundMenu).isPresent();
            assertThat(foundMenu.get().getMenuName()).isEqualTo("Lunch Menu");
            assertThat(foundMenu.get().getMenuId()).isEqualTo(testMenu1.getMenuId());
        }

        @Test
        @DisplayName("Should return empty when menu name not found")
        void shouldReturnEmptyWhenMenuNameNotFound() {
            // When
            Optional<Menu> foundMenu = menuRepository.findByMenuName("NonExistent Menu");
            
            // Then
            assertThat(foundMenu).isEmpty();
        }

        @Test
        @DisplayName("Should check if menu exists by name")
        void shouldCheckIfMenuExistsByName() {
            // When & Then
            assertThat(menuRepository.existsMenuByMenuName("Lunch Menu")).isTrue();
            assertThat(menuRepository.existsMenuByMenuName("Dinner Menu")).isTrue();
            assertThat(menuRepository.existsMenuByMenuName("Breakfast Menu")).isTrue();
            assertThat(menuRepository.existsMenuByMenuName("NonExistent Menu")).isFalse();
        }

        @Test
        @DisplayName("Should find all active menus")
        void shouldFindAllActiveMenus() {
            // When
            List<Menu> activeMenus = menuRepository.findAllByActive(true);
            
            // Then
            assertThat(activeMenus).hasSize(2);
            assertThat(activeMenus)
                .extracting(Menu::getMenuName)
                .containsExactlyInAnyOrder("Lunch Menu", "Dinner Menu");
            assertThat(activeMenus)
                .allMatch(Menu::isActive);
        }

        @Test
        @DisplayName("Should find all inactive menus")
        void shouldFindAllInactiveMenus() {
            // When
            List<Menu> inactiveMenus = menuRepository.findAllByActive(false);
            
            // Then
            assertThat(inactiveMenus).hasSize(1);
            assertThat(inactiveMenus.get(0).getMenuName()).isEqualTo("Breakfast Menu");
            assertThat(inactiveMenus.get(0).isActive()).isFalse();
        }

        @Test
        @DisplayName("Should find menus by active status and food items containing")
        void shouldFindMenusByActiveAndFoodItemsContaining() {
            // Given
            Set<FoodItem> foodItems = new HashSet<>();
            foodItems.add(testFoodItem1); // Pizza (in Lunch Menu)
            
            // When
            List<Menu> menusWithPizza = menuRepository.findAllByActiveAndFoodItemsIsContaining(true, foodItems);
            
            // Then
            assertThat(menusWithPizza).hasSize(1);
            assertThat(menusWithPizza.get(0).getMenuName()).isEqualTo("Lunch Menu");
        }
    }

    @Nested
    @DisplayName("Relationship Tests")
    class RelationshipTests {

        @Test
        @DisplayName("Should maintain many-to-many relationship with food items")
        void shouldMaintainManyToManyRelationship() {
            // Given
            Menu menu = testMenu1; // Lunch Menu
            
            // When - retrieve menu with food items
            Menu retrievedMenu = menuRepository.findById(menu.getMenuId()).orElse(null);
            
            // Then
            assertThat(retrievedMenu).isNotNull();
            assertThat(retrievedMenu.getFoodItems()).hasSize(2); // Pizza and Burger
            assertThat(retrievedMenu.getFoodItems())
                .extracting(FoodItem::getFoodName)
                .containsExactlyInAnyOrder("Pizza", "Burger");
        }

        @Test
        @DisplayName("Should handle menu with no food items")
        void shouldHandleMenuWithNoFoodItems() {
            // Given - Menu with no food items
            Menu emptyMenu = testMenu3; // Breakfast Menu has no food items in our setup
            
            // When
            Menu retrievedMenu = menuRepository.findById(emptyMenu.getMenuId()).orElse(null);
            
            // Then
            assertThat(retrievedMenu).isNotNull();
            assertThat(retrievedMenu.getFoodItems()).isEmpty();
        }

        @Test
        @DisplayName("Should handle adding food items to menu")
        void shouldHandleAddingFoodItemsToMenu() {
            // Given
            Menu menu = testMenu3; // Breakfast Menu
            FoodItem newFoodItem = createTestFoodItem("Pancakes", BigDecimal.valueOf(7.99));
            entityManager.persistAndFlush(newFoodItem);
            
            // Set up relationship after both entities are persisted
            menu.getFoodItems().add(newFoodItem);
            // Since Menu is the owning side, we need to save it through repository
            Menu savedMenu = menuRepository.save(menu);
            entityManager.flush();
            
            // When - refresh menu from database
            entityManager.clear();
            Menu refreshedMenu = menuRepository.findById(savedMenu.getMenuId()).orElse(null);
            
            // Then
            assertThat(refreshedMenu).isNotNull();
            assertThat(refreshedMenu.getFoodItems()).hasSize(1);
            assertThat(refreshedMenu.getFoodItems().iterator().next().getFoodName()).isEqualTo("Pancakes");
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should enforce unique menu name constraint")
        void shouldEnforceUniqueMenuNameConstraint() {
            // Given
            Menu duplicateMenu = createTestMenu("Lunch Menu", "Duplicate", false);
            
            // When & Then
            try {
                entityManager.persistAndFlush(duplicateMenu);
                // Should fail due to unique constraint
                assertThat(false).as("Expected unique constraint violation").isTrue();
            } catch (Exception e) {
                // Expected behavior - unique constraint violation
                assertThat(e.getMessage()).containsIgnoringCase("unique");
            }
        }

        @Test
        @DisplayName("Should handle cascade operations correctly")
        void shouldHandleCascadeOperationsCorrectly() {
            // Given
            Menu menuWithFoodItems = testMenu1; // Has 2 food items
            Long foodItem1Id = testFoodItem1.getFoodId();
            Long foodItem2Id = testFoodItem2.getFoodId();
            
            // When - delete menu
            menuRepository.delete(menuWithFoodItems);
            entityManager.flush();
            
            // Then - food items should still exist (no cascade delete)
            FoodItem foodItem1 = entityManager.find(FoodItem.class, foodItem1Id);
            FoodItem foodItem2 = entityManager.find(FoodItem.class, foodItem2Id);
            
            assertThat(foodItem1).isNotNull();
            assertThat(foodItem2).isNotNull();
        }
    }

    @Nested
    @DisplayName("Custom Operations Tests")
    class CustomOperationsTests {

        @Test
        @DisplayName("Should deactivate all menus")
        void shouldDeactivateAllMenus() {
            // Given - Initially we have 2 active menus
            List<Menu> initialActiveMenus = menuRepository.findAllByActive(true);
            assertThat(initialActiveMenus).hasSize(2);
            
            // When
            menuRepository.deactivateAll();
            entityManager.flush();
            entityManager.clear();
            
            // Then
            List<Menu> activeMenusAfter = menuRepository.findAllByActive(true);
            List<Menu> allMenus = menuRepository.findAll();
            
            assertThat(activeMenusAfter).isEmpty();
            assertThat(allMenus).hasSize(3); // All menus still exist
            assertThat(allMenus).allMatch(menu -> !menu.isActive()); // All are inactive
        }

        @Test
        @DisplayName("Should reactivate menus after deactivateAll")
        void shouldReactivateMenusAfterDeactivateAll() {
            // Given - deactivate all first
            menuRepository.deactivateAll();
            entityManager.flush();
            
            // When - reactivate a specific menu
            Menu menuToReactivate = testMenu1;
            menuToReactivate.setActive(true);
            menuRepository.save(menuToReactivate);
            entityManager.flush();
            entityManager.clear();
            
            // Then
            List<Menu> activeMenus = menuRepository.findAllByActive(true);
            assertThat(activeMenus).hasSize(1);
            assertThat(activeMenus.get(0).getMenuName()).isEqualTo("Lunch Menu");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty result sets gracefully")
        void shouldHandleEmptyResultSetsGracefully() {
            // When - querying for non-existent data
            Optional<Menu> nonExistentMenu = menuRepository.findByMenuName("NonExistent");
            boolean menuExists = menuRepository.existsMenuByMenuName("NonExistent");
            
            // Create empty set of food items
            Set<FoodItem> emptyFoodItems = new HashSet<>();
            List<Menu> menusWithEmptyFoodItems = menuRepository.findAllByActiveAndFoodItemsIsContaining(true, emptyFoodItems);
            
            // Then
            assertThat(nonExistentMenu).isEmpty();
            assertThat(menuExists).isFalse();
            assertThat(menusWithEmptyFoodItems).isEmpty();
        }

        @Test
        @DisplayName("Should handle null values gracefully")
        void shouldHandleNullValuesGracefully() {
            // When & Then - test null handling
            Optional<Menu> nullNameMenu = menuRepository.findByMenuName(null);
            boolean nullNameExists = menuRepository.existsMenuByMenuName(null);
            
            assertThat(nullNameMenu).isEmpty();
            assertThat(nullNameExists).isFalse();
        }

        @Test
        @DisplayName("Should maintain data consistency after multiple operations")
        void shouldMaintainDataConsistencyAfterMultipleOperations() {
            // Given - initial state
            int initialMenuCount = menuRepository.findAll().size();
            
            // When - perform multiple operations
            Menu newMenu = createTestMenu("Special Menu", "Special offers", true);
            Menu savedMenu = menuRepository.save(newMenu);
            
            savedMenu.setDescription("Updated special offers");
            menuRepository.save(savedMenu);
            
            menuRepository.deactivateAll();
            
            // Then - verify consistency
            List<Menu> allMenus = menuRepository.findAll();
            List<Menu> activeMenus = menuRepository.findAllByActive(true);
            
            assertThat(allMenus).hasSize(initialMenuCount + 1);
            assertThat(activeMenus).isEmpty();
            assertThat(allMenus)
                .anyMatch(menu -> menu.getMenuName().equals("Special Menu") && 
                         menu.getDescription().equals("Updated special offers"));
        }

        @Test
        @DisplayName("Should handle case-sensitive menu names")
        void shouldHandleCaseSensitiveMenuNames() {
            // When & Then - Test case sensitivity
            Optional<Menu> exactMatch = menuRepository.findByMenuName("Lunch Menu");
            Optional<Menu> wrongCase = menuRepository.findByMenuName("lunch menu");
            Optional<Menu> wrongCase2 = menuRepository.findByMenuName("LUNCH MENU");
            
            assertThat(exactMatch).isPresent();
            assertThat(wrongCase).isEmpty();
            assertThat(wrongCase2).isEmpty();
        }
    }
}
