package com.ste.restaurant.integration;

import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.repository.CategoryRepository;
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
 * Integration tests for CategoryRepository.
 * Tests the repository layer with real database operations using H2 in-memory database.
 */
@DataJpaTest
@DisplayName("Category Repository Integration Tests")
class CategoryRepositoryIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    // Test data
    private Category testCategory1;
    private Category testCategory2;
    private Category testCategory3;
    private FoodItem testFoodItem1;
    private FoodItem testFoodItem2;
    private FoodItem testFoodItem3;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    void setupTestData() {
        // Create test categories without relationships first
        testCategory1 = createTestCategory("Main Course", "Main course dishes");
        testCategory2 = createTestCategory("Appetizers", "Starter dishes");
        testCategory3 = createTestCategory("Beverages", "Drinks and beverages");

        // Persist categories
        entityManager.persistAndFlush(testCategory1);
        entityManager.persistAndFlush(testCategory2);
        entityManager.persistAndFlush(testCategory3);

        // Create test food items without categories first
        testFoodItem1 = createTestFoodItem("Pizza", BigDecimal.valueOf(15.99));
        testFoodItem2 = createTestFoodItem("Burger", BigDecimal.valueOf(12.99));
        testFoodItem3 = createTestFoodItem("Caesar Salad", BigDecimal.valueOf(8.99));

        // Persist food items
        entityManager.persistAndFlush(testFoodItem1);
        entityManager.persistAndFlush(testFoodItem2);
        entityManager.persistAndFlush(testFoodItem3);
        
        // Now set up relationships after both entities are persisted
        testFoodItem1.getCategories().add(testCategory1);
        testCategory1.getFoodItems().add(testFoodItem1);
        
        testFoodItem2.getCategories().add(testCategory1);
        testCategory1.getFoodItems().add(testFoodItem2);
        
        testFoodItem3.getCategories().add(testCategory2);
        testCategory2.getFoodItems().add(testFoodItem3);
        
        // Flush the relationship changes
        entityManager.flush();
    }

    private Category createTestCategory(String name, String description) {
        Category category = new Category();
        category.setCategoryName(name);
        return category;
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
        @DisplayName("Should save and retrieve category")
        void shouldSaveAndRetrieveCategory() {
            // Given
            Category newCategory = createTestCategory("Desserts", "Sweet treats");

            // When
            Category savedCategory = categoryRepository.save(newCategory);
            Category foundCategory = categoryRepository.findById(savedCategory.getCategoryId()).orElse(null);

            // Then
            assertThat(foundCategory).isNotNull();
            assertThat(foundCategory.getCategoryName()).isEqualTo("Desserts");
            assertThat(foundCategory.getCategoryId()).isNotNull();
        }

        @Test
        @DisplayName("Should update category successfully")
        void shouldUpdateCategory() {
            // Given
            Category category = testCategory1;
            String originalName = category.getCategoryName();
            
            // When
            category.setCategoryName("Updated Main Course");
            Category updatedCategory = categoryRepository.save(category);
            
            // Then
            assertThat(updatedCategory.getCategoryId()).isEqualTo(category.getCategoryId());
            assertThat(updatedCategory.getCategoryName()).isEqualTo("Updated Main Course");
            assertThat(updatedCategory.getCategoryName()).isNotEqualTo(originalName);
        }

        @Test
        @DisplayName("Should delete category successfully")
        void shouldDeleteCategory() {
            // Given
            Category category = testCategory3;
            Long categoryId = category.getCategoryId();
            
            // When
            categoryRepository.delete(category);
            entityManager.flush();
            
            // Then
            Optional<Category> deletedCategory = categoryRepository.findById(categoryId);
            assertThat(deletedCategory).isEmpty();
        }

        @Test
        @DisplayName("Should find all categories")
        void shouldFindAllCategories() {
            // When
            List<Category> allCategories = categoryRepository.findAll();
            
            // Then
            assertThat(allCategories).hasSize(3);
            assertThat(allCategories)
                .extracting(Category::getCategoryName)
                .containsExactlyInAnyOrder("Main Course", "Appetizers", "Beverages");
        }
    }

    @Nested
    @DisplayName("Custom Query Tests")
    class CustomQueryTests {

        @Test
        @DisplayName("Should find category by name")
        void shouldFindByCategoryName() {
            // When
            Optional<Category> foundCategory = categoryRepository.findByCategoryName("Main Course");
            
            // Then
            assertThat(foundCategory).isPresent();
            assertThat(foundCategory.get().getCategoryName()).isEqualTo("Main Course");
            assertThat(foundCategory.get().getCategoryId()).isEqualTo(testCategory1.getCategoryId());
        }

        @Test
        @DisplayName("Should return empty when category name not found")
        void shouldReturnEmptyWhenCategoryNameNotFound() {
            // When
            Optional<Category> foundCategory = categoryRepository.findByCategoryName("NonExistent");
            
            // Then
            assertThat(foundCategory).isEmpty();
        }

        @Test
        @DisplayName("Should check if category exists by name")
        void shouldCheckIfCategoryExistsByName() {
            // When & Then
            assertThat(categoryRepository.existsCategoryByCategoryName("Main Course")).isTrue();
            assertThat(categoryRepository.existsCategoryByCategoryName("Appetizers")).isTrue();
            assertThat(categoryRepository.existsCategoryByCategoryName("Beverages")).isTrue();
            assertThat(categoryRepository.existsCategoryByCategoryName("NonExistent")).isFalse();
        }

        @Test
        @DisplayName("Should get category by name (non-Optional)")
        void shouldGetCategoryByName() {
            // When
            Category foundCategory = categoryRepository.getCategoriesByCategoryName("Main Course");
            
            // Then
            assertThat(foundCategory).isNotNull();
            assertThat(foundCategory.getCategoryName()).isEqualTo("Main Course");
            assertThat(foundCategory.getCategoryId()).isEqualTo(testCategory1.getCategoryId());
        }

        @Test
        @DisplayName("Should find categories containing specified food items")
        void shouldFindCategoriesContainingSpecifiedFoodItems() {
            // Given
            Set<FoodItem> foodItems = Set.of(testFoodItem1); // Pizza
            
            // When
            List<Category> foundCategories = categoryRepository.findByFoodItemsIn(foodItems);
            
            // Then
            assertThat(foundCategories).hasSize(1);
            assertThat(foundCategories.get(0).getCategoryName()).isEqualTo("Main Course");
        }
    }

    @Nested
    @DisplayName("Relationship Tests")
    class RelationshipTests {

        @Test
        @DisplayName("Should maintain many-to-many relationship with food items")
        void shouldMaintainManyToManyRelationship() {
            // Given
            Category category = testCategory1;
            
            // When - retrieve category with food items
            Category retrievedCategory = categoryRepository.findById(category.getCategoryId()).orElse(null);
            
            // Then
            assertThat(retrievedCategory).isNotNull();
            assertThat(retrievedCategory.getFoodItems()).hasSize(2); // Pizza and Burger
            assertThat(retrievedCategory.getFoodItems())
                .extracting(FoodItem::getFoodName)
                .containsExactlyInAnyOrder("Pizza", "Burger");
        }

        @Test
        @DisplayName("Should handle category with no food items")
        void shouldHandleCategoryWithNoFoodItems() {
            // Given - Category with no food items
            Category emptyCategory = testCategory3; // Beverages has no food items in our setup
            
            // When
            Category retrievedCategory = categoryRepository.findById(emptyCategory.getCategoryId()).orElse(null);
            
            // Then
            assertThat(retrievedCategory).isNotNull();
            assertThat(retrievedCategory.getFoodItems()).isEmpty();
        }

        @Test
        @DisplayName("Should handle adding food items to category")
        void shouldHandleAddingFoodItemsToCategory() {
            // Given
            Category category = testCategory3; // Beverages
            FoodItem newFoodItem = createTestFoodItem("Cola", BigDecimal.valueOf(2.99));
            entityManager.persistAndFlush(newFoodItem);
            
            // Set up relationship after both entities are persisted
            newFoodItem.getCategories().add(category);
            category.getFoodItems().add(newFoodItem);
            entityManager.flush();
            
            // When - refresh category from database
            entityManager.clear();
            Category refreshedCategory = categoryRepository.findById(category.getCategoryId()).orElse(null);
            
            // Then
            assertThat(refreshedCategory).isNotNull();
            assertThat(refreshedCategory.getFoodItems()).hasSize(1);
            assertThat(refreshedCategory.getFoodItems().iterator().next().getFoodName()).isEqualTo("Cola");
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should handle unique category name constraint")
        void shouldHandleUniqueCategoryNameConstraint() {
            // Given
            Category duplicateCategory = createTestCategory("Main Course", "Duplicate category");
            
            // When & Then
            try {
                categoryRepository.save(duplicateCategory);
                entityManager.flush();
                // If we reach here without exception, the constraint might not be enforced
                // Let's check that only one category with this name exists
                List<Category> categoriesWithSameName = categoryRepository.findAll()
                    .stream()
                    .filter(cat -> "Main Course".equals(cat.getCategoryName()))
                    .toList();
                assertThat(categoriesWithSameName).hasSize(1);
            } catch (Exception e) {
                // Expected behavior if unique constraint is enforced
                assertThat(e).isNotNull();
            }
        }

        @Test
        @DisplayName("Should handle category name validation")
        void shouldHandleCategoryNameValidation() {
            // Given
            Category category = testCategory1;
            
            // When & Then - Test non-empty name
            category.setCategoryName("Valid Category Name");
            Category savedCategory = categoryRepository.save(category);
            assertThat(savedCategory.getCategoryName()).isEqualTo("Valid Category Name");
            
            // Test empty name (should be handled by business logic)
            category.setCategoryName("");
            Category emptyCategoryName = categoryRepository.save(category);
            assertThat(emptyCategoryName.getCategoryName()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty result sets gracefully")
        void shouldHandleEmptyResultSetsGracefully() {
            // When - querying for non-existent data
            Optional<Category> nonExistentCategory = categoryRepository.findByCategoryName("NonExistent");
            boolean categoryExists = categoryRepository.existsCategoryByCategoryName("NonExistent");
            Set<FoodItem> emptyFoodItems = new HashSet<>();
            List<Category> categoriesWithEmptyFoodItems = categoryRepository.findByFoodItemsIn(emptyFoodItems);
            
            // Then
            assertThat(nonExistentCategory).isEmpty();
            assertThat(categoryExists).isFalse();
            assertThat(categoriesWithEmptyFoodItems).isEmpty();
        }

        @Test
        @DisplayName("Should handle null values gracefully")
        void shouldHandleNullValuesGracefully() {
            // When & Then - Test null parameter handling
            Optional<Category> nullNameCategory = categoryRepository.findByCategoryName(null);
            boolean nullNameExists = categoryRepository.existsCategoryByCategoryName(null);
            
            assertThat(nullNameCategory).isEmpty();
            assertThat(nullNameExists).isFalse();
        }

        @Test
        @DisplayName("Should maintain data consistency after multiple operations")
        void shouldMaintainDataConsistencyAfterMultipleOperations() {
            // Given
            int initialCount = categoryRepository.findAll().size();
            
            // When - perform multiple operations
            Category newCategory1 = createTestCategory("Soups", "Hot and cold soups");
            Category newCategory2 = createTestCategory("Salads", "Fresh salads");
            
            categoryRepository.save(newCategory1);
            categoryRepository.save(newCategory2);
            categoryRepository.delete(testCategory1);
            
            entityManager.flush();
            
            // Then
            List<Category> remainingCategories = categoryRepository.findAll();
            assertThat(remainingCategories).hasSize(initialCount + 1); // +2 added, -1 deleted
            assertThat(remainingCategories).contains(testCategory2, newCategory1, newCategory2);
            assertThat(remainingCategories).doesNotContain(testCategory1);
        }

        @Test
        @DisplayName("Should handle case-sensitive category names")
        void shouldHandleCaseSensitiveCategoryNames() {
            // When & Then - Test case sensitivity
            Optional<Category> exactMatch = categoryRepository.findByCategoryName("Main Course");
            Optional<Category> wrongCase = categoryRepository.findByCategoryName("main course");
            Optional<Category> wrongCase2 = categoryRepository.findByCategoryName("MAIN COURSE");
            
            assertThat(exactMatch).isPresent();
            assertThat(wrongCase).isEmpty();
            assertThat(wrongCase2).isEmpty();
        }
    }
}
