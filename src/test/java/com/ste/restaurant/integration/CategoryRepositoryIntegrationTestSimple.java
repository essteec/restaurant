package com.ste.restaurant.integration;

import com.ste.restaurant.entity.Category;
import com.ste.restaurant.repository.CategoryRepository;
import com.ste.restaurant.utils.RepositoryTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple integration test for CategoryRepository to isolate the compilation issue.
 */
@DataJpaTest
@DisplayName("Category Repository Simple Integration Tests")
class CategoryRepositoryIntegrationTestSimple extends RepositoryTestBase {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("Should save and retrieve category without DatabaseTestUtils")
    void shouldSaveAndRetrieveCategorySimple() {
        // Given
        Category category = new Category();
        category.setCategoryName("Beverages");
        
        // When
        Category savedCategory = categoryRepository.save(category);
        Category foundCategory = categoryRepository.findById(savedCategory.getCategoryId()).orElse(null);
        
        // Then
        assertThat(foundCategory).isNotNull();
        assertThat(foundCategory.getCategoryName()).isEqualTo("Beverages");
        assertThat(foundCategory.getCategoryId()).isNotNull();
    }
}
