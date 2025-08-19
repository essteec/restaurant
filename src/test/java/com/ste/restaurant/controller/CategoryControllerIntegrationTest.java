package com.ste.restaurant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ste.restaurant.dto.CategoryDto;
import com.ste.restaurant.dto.CategoryDtoBasic;
import com.ste.restaurant.dto.FoodItemDto;
import com.ste.restaurant.dto.common.StringsDto;
import com.ste.restaurant.dto.common.WarningResponse;
import com.ste.restaurant.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CategoryController REST endpoints.
 * Uses full Spring Boot context with mocked service layer.
 * 
 * Tests cover:
 * - CRUD operations (Create, Read, Update, Delete)
 * - Pagination support
 * - Role-based security (@PreAuthorize)
 * - Food item relationship management
 * - Request/Response JSON mapping
 * - Error handling and validation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Category Controller Integration Tests")
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private CategoryDtoBasic testCategoryBasic;
    private CategoryDto testCategoryDto;
    private FoodItemDto testFoodItemDto;

    @BeforeEach
    void setUp() {
        testCategoryBasic = new CategoryDtoBasic();
        testCategoryBasic.setCategoryName("Main Course");

        testFoodItemDto = new FoodItemDto();
        testFoodItemDto.setFoodName("Margherita Pizza");
        testFoodItemDto.setDescription("Classic pizza with tomato sauce, mozzarella, and basil");
        testFoodItemDto.setPrice(BigDecimal.valueOf(12.99));
        testFoodItemDto.setImage("pizza-margherita.jpg");

        testCategoryDto = new CategoryDto();
        testCategoryDto.setCategoryName("Main Course");
        testCategoryDto.setFoodItems(Set.of(testFoodItemDto));
    }

    @Nested
    @DisplayName("POST /rest/api/categories - Create Category")
    class CreateCategoryTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should successfully create category as admin")
        void saveCategory_asAdmin_success() throws Exception {
            // Arrange
            when(categoryService.saveCategory(any(CategoryDtoBasic.class))).thenReturn(testCategoryBasic);

            // Act & Assert
            mockMvc.perform(post("/rest/api/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCategoryBasic)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.categoryName").value("Main Course"));

            verify(categoryService).saveCategory(any(CategoryDtoBasic.class));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should deny access when customer tries to create category")
        void saveCategory_asCustomer_accessDenied() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/rest/api/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCategoryBasic)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("Should deny access when unauthenticated user tries to create category")
        void saveCategory_unauthenticated_accessDenied() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/rest/api/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCategoryBasic)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(categoryService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request for invalid category data")
        void saveCategory_invalidData_badRequest() throws Exception {
            // Arrange - Invalid category with blank name
            CategoryDtoBasic invalidCategory = new CategoryDtoBasic();
            invalidCategory.setCategoryName(""); // Invalid blank name

            // Act & Assert
            mockMvc.perform(post("/rest/api/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidCategory)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(categoryService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request when category name is null")
        void saveCategory_nullName_badRequest() throws Exception {
            // Arrange
            CategoryDtoBasic invalidCategory = new CategoryDtoBasic();
            invalidCategory.setCategoryName(null); // Null name should trigger validation

            // Act & Assert
            mockMvc.perform(post("/rest/api/categories")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidCategory)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(categoryService);
        }
    }

    @Nested
    @DisplayName("GET /rest/api/categories - Get All Categories")
    class GetAllCategoriesTests {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should successfully retrieve paginated categories as customer")
        void getAllCategories_asCustomer_success() throws Exception {
            // Arrange
            List<CategoryDto> categories = Arrays.asList(testCategoryDto);
            Page<CategoryDto> categoryPage = new PageImpl<>(categories, PageRequest.of(0, 24), 1);
            when(categoryService.listAllCategory(any(Pageable.class))).thenReturn(categoryPage);

            // Act & Assert
            mockMvc.perform(get("/rest/api/categories")
                            .param("page", "0")
                            .param("size", "24")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].categoryName").value("Main Course"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.size").value(24));

            verify(categoryService).listAllCategory(any(Pageable.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should successfully retrieve paginated categories as admin")
        void getAllCategories_asAdmin_success() throws Exception {
            // Arrange
            List<CategoryDto> categories = Arrays.asList(testCategoryDto);
            Page<CategoryDto> categoryPage = new PageImpl<>(categories, PageRequest.of(0, 24), 1);
            when(categoryService.listAllCategory(any(Pageable.class))).thenReturn(categoryPage);

            // Act & Assert
            mockMvc.perform(get("/rest/api/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].categoryName").value("Main Course"));

            verify(categoryService).listAllCategory(any(Pageable.class));
        }

        @Test
        @DisplayName("Should allow access when unauthenticated user tries to get categories")
        void getAllCategories_unauthenticated_success() throws Exception {
            // Arrange
            List<CategoryDto> categories = Arrays.asList(testCategoryDto);
            Page<CategoryDto> categoryPage = new PageImpl<>(categories, PageRequest.of(0, 24), 1);
            when(categoryService.listAllCategory(any(Pageable.class))).thenReturn(categoryPage);

            // Act & Assert
            mockMvc.perform(get("/rest/api/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());

            verify(categoryService).listAllCategory(any(Pageable.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return empty page when no categories exist")
        void getAllCategories_emptyResult() throws Exception {
            // Arrange
            Page<CategoryDto> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 24), 0);
            when(categoryService.listAllCategory(any(Pageable.class))).thenReturn(emptyPage);

            // Act & Assert
            mockMvc.perform(get("/rest/api/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));

            verify(categoryService).listAllCategory(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("PUT /rest/api/categories/{name} - Update Category")
    class UpdateCategoryTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should successfully update category as admin")
        void updateCategory_asAdmin_success() throws Exception {
            // Arrange
            CategoryDto updatedCategory = new CategoryDto();
            updatedCategory.setCategoryName("Updated Main Course");
            updatedCategory.setFoodItems(Set.of(testFoodItemDto));

            when(categoryService.updateCategoryByName(eq("Main Course"), any(CategoryDtoBasic.class)))
                    .thenReturn(updatedCategory);

            // Act & Assert
            mockMvc.perform(put("/rest/api/categories/Main Course")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCategoryBasic)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.categoryName").value("Updated Main Course"));

            verify(categoryService).updateCategoryByName("Main Course", testCategoryBasic);
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should deny access when waiter tries to update category")
        void updateCategory_asWaiter_accessDenied() throws Exception {
            // Act & Assert
            mockMvc.perform(put("/rest/api/categories/Main Course")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCategoryBasic)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(categoryService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle URL encoded category names")
        void updateCategory_urlEncoded_success() throws Exception {
            // Arrange
            String encodedName = "Main Course"; // Use direct name without encoding since MockMvc handles it
            when(categoryService.updateCategoryByName(eq("Main Course"), any(CategoryDtoBasic.class)))
                    .thenReturn(testCategoryDto);

            // Act & Assert
            mockMvc.perform(put("/rest/api/categories/" + encodedName)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCategoryBasic)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categoryName").value("Main Course"));

            verify(categoryService).updateCategoryByName("Main Course", testCategoryBasic);
        }
    }

    @Nested
    @DisplayName("DELETE /rest/api/categories/{name} - Delete Category")
    class DeleteCategoryTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should successfully delete category as admin")
        void deleteCategory_asAdmin_success() throws Exception {
            // Arrange
            when(categoryService.deleteCategoryByName("Main Course")).thenReturn(testCategoryDto);

            // Act & Assert
            mockMvc.perform(delete("/rest/api/categories/Main Course")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.categoryName").value("Main Course"));

            verify(categoryService).deleteCategoryByName("Main Course");
        }

        @Test
        @WithMockUser(roles = "CHEF")
        @DisplayName("Should deny access when chef tries to delete category")
        void deleteCategory_asChef_accessDenied() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/rest/api/categories/Main Course")
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(categoryService);
        }
    }

    @Nested
    @DisplayName("PUT /rest/api/categories/{categoryName}/food-items - Add Food Items to Category")
    class AddFoodItemsToCategoryTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should successfully add food items to category as admin")
        void addFoodItemsToCategory_asAdmin_success() throws Exception {
            // Arrange
            StringsDto foodNames = new StringsDto();
            foodNames.setNames(Set.of("Margherita Pizza", "Caesar Salad"));

            WarningResponse<CategoryDto> response = new WarningResponse<>(testCategoryDto, Arrays.asList());
            when(categoryService.addFoodItemsToCategory(eq("Main Course"), any(StringsDto.class)))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(put("/rest/api/categories/Main Course/food-items")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(foodNames)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.categoryName").value("Main Course"))
                    .andExpect(jsonPath("$.warnings").isArray())
                    .andExpect(jsonPath("$.warnings").isEmpty());

            verify(categoryService).addFoodItemsToCategory("Main Course", foodNames);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return warnings when some food items fail to be added")
        void addFoodItemsToCategory_withWarnings_success() throws Exception {
            // Arrange
            StringsDto foodNames = new StringsDto();
            foodNames.setNames(Set.of("Margherita Pizza", "NonExistent Food"));

            WarningResponse<CategoryDto> response = new WarningResponse<>(testCategoryDto, 
                    Arrays.asList("Food item 'NonExistent Food' not found"));
            when(categoryService.addFoodItemsToCategory(eq("Main Course"), any(StringsDto.class)))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(put("/rest/api/categories/Main Course/food-items")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(foodNames)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.categoryName").value("Main Course"))
                    .andExpect(jsonPath("$.warnings").isArray())
                    .andExpect(jsonPath("$.warnings[0]").value("Food item 'NonExistent Food' not found"));

            verify(categoryService).addFoodItemsToCategory("Main Course", foodNames);
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should deny access when customer tries to add food items to category")
        void addFoodItemsToCategory_asCustomer_accessDenied() throws Exception {
            // Arrange
            StringsDto foodNames = new StringsDto();
            foodNames.setNames(Set.of("Margherita Pizza"));

            // Act & Assert
            mockMvc.perform(put("/rest/api/categories/Main Course/food-items")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(foodNames)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(categoryService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request for empty food names list")
        void addFoodItemsToCategory_emptyList_badRequest() throws Exception {
            // Arrange
            StringsDto emptyFoodNames = new StringsDto();
            emptyFoodNames.setNames(Set.of()); // Empty set should trigger validation

            // Act & Assert
            mockMvc.perform(put("/rest/api/categories/Main Course/food-items")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyFoodNames)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(categoryService);
        }
    }

    @Nested
    @DisplayName("DELETE /rest/api/categories/{categoryName}/food-items - Remove Food Items from Category")
    class RemoveFoodItemsFromCategoryTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should successfully remove food items from category as admin")
        void removeFoodItemsFromCategory_asAdmin_success() throws Exception {
            // Arrange
            StringsDto foodNames = new StringsDto();
            foodNames.setNames(Set.of("Margherita Pizza"));

            WarningResponse<CategoryDto> response = new WarningResponse<>(testCategoryDto, Arrays.asList());
            when(categoryService.removeFoodItemsFromCategory(eq("Main Course"), any(StringsDto.class)))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(delete("/rest/api/categories/Main Course/food-items")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(foodNames)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.categoryName").value("Main Course"))
                    .andExpect(jsonPath("$.warnings").isArray());

            verify(categoryService).removeFoodItemsFromCategory("Main Course", foodNames);
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should deny access when waiter tries to remove food items from category")
        void removeFoodItemsFromCategory_asWaiter_accessDenied() throws Exception {
            // Arrange
            StringsDto foodNames = new StringsDto();
            foodNames.setNames(Set.of("Margherita Pizza"));

            // Act & Assert
            mockMvc.perform(delete("/rest/api/categories/Main Course/food-items")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(foodNames)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(categoryService);
        }
    }
}
