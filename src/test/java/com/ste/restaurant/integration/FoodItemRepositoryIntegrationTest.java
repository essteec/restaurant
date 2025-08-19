package com.ste.restaurant.integration;

import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.repository.FoodItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for FoodItemRepository.
 * Tests the repository layer with real database operations using H2 in-memory database.
 */
@DataJpaTest
@DisplayName("FoodItem Repository Integration Tests")
class FoodItemRepositoryIntegrationTest {

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    // Test data
    private FoodItem testFoodItem1;
    private FoodItem testFoodItem2;
    private FoodItem testFoodItem3;
    private Category testCategory1;
    private Category testCategory2;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    void setupTestData() {
        // Create test categories first
        testCategory1 = createTestCategory("Main Course");
        testCategory2 = createTestCategory("Appetizers");

        // Persist categories
        entityManager.persistAndFlush(testCategory1);
        entityManager.persistAndFlush(testCategory2);

        // Create test food items without relationships first
        testFoodItem1 = createTestFoodItem("Margherita Pizza", "Classic pizza with tomato sauce, mozzarella, and basil", BigDecimal.valueOf(15.99));
        testFoodItem2 = createTestFoodItem("Caesar Salad", "Fresh romaine lettuce with Caesar dressing and croutons", BigDecimal.valueOf(8.99));
        testFoodItem3 = createTestFoodItem("Chicken Wings", "Spicy chicken wings with buffalo sauce", BigDecimal.valueOf(12.99));

        // Persist food items
        entityManager.persistAndFlush(testFoodItem1);
        entityManager.persistAndFlush(testFoodItem2);
        entityManager.persistAndFlush(testFoodItem3);

        // Now set up relationships after both entities are persisted
        // Pizza → Main Course
        testCategory1.getFoodItems().add(testFoodItem1);
        testFoodItem1.getCategories().add(testCategory1);

        // Caesar Salad → Appetizers  
        testCategory2.getFoodItems().add(testFoodItem2);
        testFoodItem2.getCategories().add(testCategory2);

        // Chicken Wings → Both categories (many-to-many example)
        testCategory1.getFoodItems().add(testFoodItem3);
        testCategory2.getFoodItems().add(testFoodItem3);
        testFoodItem3.getCategories().add(testCategory1);
        testFoodItem3.getCategories().add(testCategory2);

        // Final flush to persist relationships
        entityManager.flush();
        entityManager.clear();
    }

    private FoodItem createTestFoodItem(String name, String description, BigDecimal price) {
        FoodItem foodItem = new FoodItem();
        foodItem.setFoodName(name);
        foodItem.setDescription(description);
        foodItem.setPrice(price);
        foodItem.setImage("test-" + name.toLowerCase().replace(" ", "-") + ".jpg");
        return foodItem;
    }

    private Category createTestCategory(String name) {
        Category category = new Category();
        category.setCategoryName(name);
        return category;
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve food item")
        void shouldSaveAndRetrieveFoodItem() {
            // Given
            FoodItem newFoodItem = createTestFoodItem("Beef Burger", "Juicy beef burger with lettuce and tomato", BigDecimal.valueOf(13.99));
            
            // When
            FoodItem savedFoodItem = foodItemRepository.save(newFoodItem);
            FoodItem retrievedFoodItem = foodItemRepository.findById(savedFoodItem.getFoodId()).orElse(null);
            
            // Then
            assertThat(savedFoodItem).isNotNull();
            assertThat(savedFoodItem.getFoodId()).isNotNull();
            assertThat(retrievedFoodItem).isNotNull();
            assertThat(retrievedFoodItem.getFoodName()).isEqualTo("Beef Burger");
            assertThat(retrievedFoodItem.getDescription()).isEqualTo("Juicy beef burger with lettuce and tomato");
            assertThat(retrievedFoodItem.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(13.99));
            assertThat(retrievedFoodItem.getImage()).isEqualTo("test-beef-burger.jpg");
        }

        @Test
        @DisplayName("Should find all food items")
        void shouldFindAllFoodItems() {
            // When
            List<FoodItem> allFoodItems = foodItemRepository.findAll();
            
            // Then
            assertThat(allFoodItems).hasSize(3);
            assertThat(allFoodItems)
                .extracting(FoodItem::getFoodName)
                .containsExactlyInAnyOrder("Margherita Pizza", "Caesar Salad", "Chicken Wings");
        }

        @Test
        @DisplayName("Should delete food item")
        void shouldDeleteFoodItem() {
            // Given
            FoodItem foodItemToDelete = testFoodItem2; // Caesar Salad
            Long foodItemId = foodItemToDelete.getFoodId();
            
            // First, remove the food item from all categories to avoid constraint violations
            // Find and reload the categories to get fresh instances
            Category category = entityManager.find(Category.class, testCategory2.getCategoryId());
            category.getFoodItems().remove(foodItemToDelete);
            entityManager.flush();
            
            // When
            foodItemRepository.delete(foodItemToDelete);
            entityManager.flush();
            Optional<FoodItem> deletedFoodItem = foodItemRepository.findById(foodItemId);
            
            // Then
            assertThat(deletedFoodItem).isEmpty();
            
            // Verify remaining food items
            List<FoodItem> remainingFoodItems = foodItemRepository.findAll();
            assertThat(remainingFoodItems).hasSize(2);
            assertThat(remainingFoodItems)
                .extracting(FoodItem::getFoodName)
                .containsExactlyInAnyOrder("Margherita Pizza", "Chicken Wings");
        }

        @Test
        @DisplayName("Should update food item")
        void shouldUpdateFoodItem() {
            // Given
            FoodItem foodItemToUpdate = testFoodItem1; // Margherita Pizza
            
            // When
            foodItemToUpdate.setDescription("Updated: Classic pizza with fresh tomato sauce, mozzarella, and basil");
            foodItemToUpdate.setPrice(BigDecimal.valueOf(17.99));
            FoodItem updatedFoodItem = foodItemRepository.save(foodItemToUpdate);
            
            // Then
            assertThat(updatedFoodItem.getDescription()).isEqualTo("Updated: Classic pizza with fresh tomato sauce, mozzarella, and basil");
            assertThat(updatedFoodItem.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(17.99));
            assertThat(updatedFoodItem.getFoodName()).isEqualTo("Margherita Pizza"); // Should remain unchanged
        }
    }

    @Nested
    @DisplayName("Custom Query Tests")
    class CustomQueryTests {

        @Test
        @DisplayName("Should find food item by name")
        void shouldFindByFoodName() {
            // When
            Optional<FoodItem> foundFoodItem = foodItemRepository.findByFoodName("Margherita Pizza");
            
            // Then
            assertThat(foundFoodItem).isPresent();
            assertThat(foundFoodItem.get().getFoodName()).isEqualTo("Margherita Pizza");
            assertThat(foundFoodItem.get().getFoodId()).isEqualTo(testFoodItem1.getFoodId());
            assertThat(foundFoodItem.get().getPrice()).isEqualByComparingTo(BigDecimal.valueOf(15.99));
        }

        @Test
        @DisplayName("Should return empty when food name not found")
        void shouldReturnEmptyWhenFoodNameNotFound() {
            // When
            Optional<FoodItem> foundFoodItem = foodItemRepository.findByFoodName("NonExistent Food");
            
            // Then
            assertThat(foundFoodItem).isEmpty();
        }

        @Test
        @DisplayName("Should check if food item exists by name")
        void shouldCheckIfFoodItemExistsByName() {
            // When & Then
            assertThat(foodItemRepository.existsFoodItemByFoodName("Margherita Pizza")).isTrue();
            assertThat(foodItemRepository.existsFoodItemByFoodName("Caesar Salad")).isTrue();
            assertThat(foodItemRepository.existsFoodItemByFoodName("Chicken Wings")).isTrue();
            assertThat(foodItemRepository.existsFoodItemByFoodName("NonExistent Food")).isFalse();
        }

        @Test
        @DisplayName("Should handle case-sensitive food names")
        void shouldHandleCaseSensitiveFoodNames() {
            // When & Then - Test case sensitivity
            Optional<FoodItem> exactMatch = foodItemRepository.findByFoodName("Margherita Pizza");
            Optional<FoodItem> wrongCase = foodItemRepository.findByFoodName("margherita pizza");
            Optional<FoodItem> wrongCase2 = foodItemRepository.findByFoodName("MARGHERITA PIZZA");
            
            assertThat(exactMatch).isPresent();
            assertThat(wrongCase).isEmpty();
            assertThat(wrongCase2).isEmpty();
            
            // Same test for existence check
            assertThat(foodItemRepository.existsFoodItemByFoodName("Margherita Pizza")).isTrue();
            assertThat(foodItemRepository.existsFoodItemByFoodName("margherita pizza")).isFalse();
            assertThat(foodItemRepository.existsFoodItemByFoodName("MARGHERITA PIZZA")).isFalse();
        }
    }

    @Nested
    @DisplayName("Relationship Tests")
    class RelationshipTests {

        @Test
        @DisplayName("Should maintain many-to-many relationship with categories")
        void shouldMaintainManyToManyRelationship() {
            // Given
            FoodItem foodItem = testFoodItem3; // Chicken Wings (has both categories)
            
            // When - retrieve food item with categories
            FoodItem retrievedFoodItem = foodItemRepository.findById(foodItem.getFoodId()).orElse(null);
            
            // Then
            assertThat(retrievedFoodItem).isNotNull();
            assertThat(retrievedFoodItem.getCategories()).hasSize(2); // Main Course and Appetizers
            assertThat(retrievedFoodItem.getCategories())
                .extracting(Category::getCategoryName)
                .containsExactlyInAnyOrder("Main Course", "Appetizers");
        }

        @Test
        @DisplayName("Should handle food item with single category")
        void shouldHandleFoodItemWithSingleCategory() {
            // Given - Food item with single category
            FoodItem foodItem = testFoodItem1; // Margherita Pizza (Main Course only)
            
            // When
            FoodItem retrievedFoodItem = foodItemRepository.findById(foodItem.getFoodId()).orElse(null);
            
            // Then
            assertThat(retrievedFoodItem).isNotNull();
            assertThat(retrievedFoodItem.getCategories()).hasSize(1);
            assertThat(retrievedFoodItem.getCategories().iterator().next().getCategoryName()).isEqualTo("Main Course");
        }

        @Test
        @DisplayName("Should handle food item with no categories")
        void shouldHandleFoodItemWithNoCategories() {
            // Given - Create a food item without categories
            FoodItem foodItemWithoutCategories = createTestFoodItem("Plain Bread", "Simple bread roll", BigDecimal.valueOf(2.99));
            FoodItem savedFoodItem = foodItemRepository.save(foodItemWithoutCategories);
            entityManager.flush();
            entityManager.clear();
            
            // When
            FoodItem retrievedFoodItem = foodItemRepository.findById(savedFoodItem.getFoodId()).orElse(null);
            
            // Then
            assertThat(retrievedFoodItem).isNotNull();
            assertThat(retrievedFoodItem.getCategories()).isEmpty();
        }

        @Test
        @DisplayName("Should handle adding categories to food item")
        void shouldHandleAddingCategoriesToFoodItem() {
            // Given
            FoodItem foodItem = testFoodItem2; // Caesar Salad (currently has Appetizers)
            Category newCategory = testCategory1; // Main Course
            
            // Set up relationship - Category is the owning side, so we need to manage both sides
            newCategory.getFoodItems().add(foodItem);
            foodItem.getCategories().add(newCategory);
            
            // Save the category since it's the owning side
            entityManager.merge(newCategory);
            entityManager.flush();
            
            // When - refresh food item from database
            entityManager.clear();
            FoodItem refreshedFoodItem = foodItemRepository.findById(foodItem.getFoodId()).orElse(null);
            
            // Force initialization of the categories collection
            assertThat(refreshedFoodItem).isNotNull();
            int categoryCount = refreshedFoodItem.getCategories().size(); // This will trigger lazy loading
            
            // Then
            assertThat(categoryCount).isEqualTo(2); // Appetizers + Main Course
            assertThat(refreshedFoodItem.getCategories())
                .extracting(Category::getCategoryName)
                .containsExactlyInAnyOrder("Appetizers", "Main Course");
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should enforce unique food name constraint")
        void shouldEnforceUniqueFoodNameConstraint() {
            // Given
            FoodItem duplicateFoodItem = createTestFoodItem("Margherita Pizza", "Duplicate pizza", BigDecimal.valueOf(10.99));
            
            // When & Then
            try {
                entityManager.persistAndFlush(duplicateFoodItem);
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
            FoodItem foodItemWithCategories = testFoodItem3; // Has 2 categories
            Long category1Id = testCategory1.getCategoryId();
            Long category2Id = testCategory2.getCategoryId();
            
            // First remove the food item from categories to avoid constraint violations
            Category category1 = entityManager.find(Category.class, category1Id);
            Category category2 = entityManager.find(Category.class, category2Id);
            
            category1.getFoodItems().remove(foodItemWithCategories);
            category2.getFoodItems().remove(foodItemWithCategories);
            entityManager.flush();
            
            // When - delete food item
            foodItemRepository.delete(foodItemWithCategories);
            entityManager.flush();
            
            // Then - categories should still exist (no cascade delete)
            category1 = entityManager.find(Category.class, category1Id);
            category2 = entityManager.find(Category.class, category2Id);
            
            assertThat(category1).isNotNull();
            assertThat(category2).isNotNull();
        }

        @Test
        @DisplayName("Should handle null values correctly")
        void shouldHandleNullValuesCorrectly() {
            // Given - Create food item with null optional fields
            FoodItem foodItemWithNulls = new FoodItem();
            foodItemWithNulls.setFoodName("Simple Food");
            foodItemWithNulls.setPrice(BigDecimal.valueOf(5.99));
            // description and image are null
            
            // When
            FoodItem savedFoodItem = foodItemRepository.save(foodItemWithNulls);
            entityManager.flush();
            entityManager.clear();
            
            FoodItem retrievedFoodItem = foodItemRepository.findById(savedFoodItem.getFoodId()).orElse(null);
            
            // Then
            assertThat(retrievedFoodItem).isNotNull();
            assertThat(retrievedFoodItem.getFoodName()).isEqualTo("Simple Food");
            assertThat(retrievedFoodItem.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(5.99));
            assertThat(retrievedFoodItem.getDescription()).isNull();
            assertThat(retrievedFoodItem.getImage()).isNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty result sets gracefully")
        void shouldHandleEmptyResultSetsGracefully() {
            // When - querying for non-existent data
            Optional<FoodItem> nonExistentFoodItem = foodItemRepository.findByFoodName("NonExistent");
            boolean foodItemExists = foodItemRepository.existsFoodItemByFoodName("NonExistent");
            
            // Then
            assertThat(nonExistentFoodItem).isEmpty();
            assertThat(foodItemExists).isFalse();
        }

        @Test
        @DisplayName("Should handle null values gracefully in queries")
        void shouldHandleNullValuesGracefullyInQueries() {
            // When & Then - test null handling
            Optional<FoodItem> nullNameFoodItem = foodItemRepository.findByFoodName(null);
            boolean nullNameExists = foodItemRepository.existsFoodItemByFoodName(null);
            
            assertThat(nullNameFoodItem).isEmpty();
            assertThat(nullNameExists).isFalse();
        }

        @Test
        @DisplayName("Should maintain data consistency after multiple operations")
        void shouldMaintainDataConsistencyAfterMultipleOperations() {
            // Given - initial state
            int initialFoodItemCount = foodItemRepository.findAll().size();
            
            // When - perform multiple operations
            FoodItem newFoodItem = createTestFoodItem("Test Dish", "Test description", BigDecimal.valueOf(9.99));
            FoodItem savedFoodItem = foodItemRepository.save(newFoodItem);
            
            savedFoodItem.setDescription("Updated test description");
            savedFoodItem.setPrice(BigDecimal.valueOf(11.99));
            foodItemRepository.save(savedFoodItem);
            
            // Add categories
            testCategory1.getFoodItems().add(savedFoodItem);
            savedFoodItem.getCategories().add(testCategory1);
            entityManager.flush();
            
            // Then - verify consistency
            List<FoodItem> allFoodItems = foodItemRepository.findAll();
            FoodItem retrievedFoodItem = foodItemRepository.findByFoodName("Test Dish").orElse(null);
            
            assertThat(allFoodItems).hasSize(initialFoodItemCount + 1);
            assertThat(retrievedFoodItem).isNotNull();
            assertThat(retrievedFoodItem.getDescription()).isEqualTo("Updated test description");
            assertThat(retrievedFoodItem.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(11.99));
            assertThat(retrievedFoodItem.getCategories()).hasSize(1);
            assertThat(retrievedFoodItem.getCategories().iterator().next().getCategoryName()).isEqualTo("Main Course");
        }

        @Test
        @DisplayName("Should handle special characters in food names")
        void shouldHandleSpecialCharactersInFoodNames() {
            // Given
            FoodItem specialFoodItem = createTestFoodItem("Crème Brûlée & Café", "French dessert with special chars", BigDecimal.valueOf(7.50));
            
            // When
            FoodItem savedFoodItem = foodItemRepository.save(specialFoodItem);
            entityManager.flush();
            
            Optional<FoodItem> retrievedFoodItem = foodItemRepository.findByFoodName("Crème Brûlée & Café");
            boolean exists = foodItemRepository.existsFoodItemByFoodName("Crème Brûlée & Café");
            
            // Then
            assertThat(savedFoodItem.getFoodName()).isEqualTo("Crème Brûlée & Café");
            assertThat(retrievedFoodItem).isPresent();
            assertThat(retrievedFoodItem.get().getFoodName()).isEqualTo("Crème Brûlée & Café");
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should handle price precision correctly")
        void shouldHandlePricePrecisionCorrectly() {
            // Given - Test with various price precisions (using realistic decimal places)
            FoodItem foodItem1 = createTestFoodItem("Price Test 1", "Test", new BigDecimal("10.99"));
            FoodItem foodItem2 = createTestFoodItem("Price Test 2", "Test", new BigDecimal("15.95")); // 2 decimal places (typical for currency)
            FoodItem foodItem3 = createTestFoodItem("Price Test 3", "Test", new BigDecimal("20.00"));    // Explicit 2 decimal places
            
            // When
            foodItemRepository.save(foodItem1);
            foodItemRepository.save(foodItem2);
            foodItemRepository.save(foodItem3);
            entityManager.flush();
            entityManager.clear();
            
            // Then
            FoodItem retrieved1 = foodItemRepository.findByFoodName("Price Test 1").orElse(null);
            FoodItem retrieved2 = foodItemRepository.findByFoodName("Price Test 2").orElse(null);
            FoodItem retrieved3 = foodItemRepository.findByFoodName("Price Test 3").orElse(null);
            
            assertThat(retrieved1).isNotNull();
            assertThat(retrieved1.getPrice()).isEqualByComparingTo(new BigDecimal("10.99"));
            
            assertThat(retrieved2).isNotNull();
            assertThat(retrieved2.getPrice()).isEqualByComparingTo(new BigDecimal("15.95"));
            
            assertThat(retrieved3).isNotNull();
            assertThat(retrieved3.getPrice()).isEqualByComparingTo(new BigDecimal("20.00"));
        }
    }
}
