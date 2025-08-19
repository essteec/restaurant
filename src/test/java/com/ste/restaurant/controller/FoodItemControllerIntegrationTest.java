package com.ste.restaurant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ste.restaurant.dto.FoodItemDto;
import com.ste.restaurant.service.FoodItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FoodItemController REST endpoints.
 * Uses full Spring Boot context with mocked service layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Food Item Controller Integration Tests")
class FoodItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FoodItemService foodItemService;

    @Autowired
    private ObjectMapper objectMapper;

    private FoodItemDto testFoodItemDto;

    @BeforeEach
    void setUp() {
        testFoodItemDto = new FoodItemDto();
        testFoodItemDto.setFoodName("Margherita Pizza");
        testFoodItemDto.setDescription("Classic pizza with tomato sauce, mozzarella, and basil");
        testFoodItemDto.setPrice(BigDecimal.valueOf(12.99));
        testFoodItemDto.setImage("pizza-margherita.jpg");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should successfully create food item as admin")
    void saveFoodItem_asAdmin_success() throws Exception {
        // Arrange
        when(foodItemService.saveFoodItem(any(FoodItemDto.class))).thenReturn(testFoodItemDto);

        // Act & Assert
        mockMvc.perform(post("/rest/api/food-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testFoodItemDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.foodName").value("Margherita Pizza"))
                .andExpect(jsonPath("$.description").value("Classic pizza with tomato sauce, mozzarella, and basil"))
                .andExpect(jsonPath("$.price").value(12.99))
                .andExpect(jsonPath("$.image").value("pizza-margherita.jpg"));

        verify(foodItemService).saveFoodItem(any(FoodItemDto.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Should deny access when customer tries to create food item")
    void saveFoodItem_asCustomer_accessDenied() throws Exception {
        // Act & Assert - AuthorizationDeniedException is now properly handled
        // Returns 403 Forbidden as expected for authorization failures
        mockMvc.perform(post("/rest/api/food-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testFoodItemDto)))
                .andExpect(status().isForbidden()) // 403 Forbidden for access denied
                .andExpect(jsonPath("$.message").value("Access denied"));

        verifyNoInteractions(foodItemService);
    }

    @Test
    @DisplayName("Should deny access when unauthenticated user tries to create food item")
    void saveFoodItem_unauthenticated_accessDenied() throws Exception {
        // Act & Assert - No authentication provided, Spring Security returns 403 Forbidden
        // This is standard Spring Security behavior for unauthenticated requests to protected endpoints
        mockMvc.perform(post("/rest/api/food-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testFoodItemDto)))
                .andExpect(status().isForbidden()); // 403 for unauthenticated access to protected endpoint

        verifyNoInteractions(foodItemService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should successfully retrieve paginated food items")
    void getAllFoodItems_success() throws Exception {
        // Arrange
        List<FoodItemDto> foodItems = Arrays.asList(testFoodItemDto);
        Page<FoodItemDto> foodItemPage = new PageImpl<>(foodItems, PageRequest.of(0, 20), 1);
        when(foodItemService.getAllFoodItems(any(Pageable.class))).thenReturn(foodItemPage);

        // Act & Assert
        mockMvc.perform(get("/rest/api/food-items")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].foodName").value("Margherita Pizza"))
                .andExpect(jsonPath("$.content[0].price").value(12.99))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(20));

        verify(foodItemService).getAllFoodItems(any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should successfully retrieve food item by name")
    void getFoodItemByName_success() throws Exception {
        // Arrange
        when(foodItemService.getFoodItemByName("Margherita Pizza")).thenReturn(testFoodItemDto);

        // Act & Assert
        mockMvc.perform(get("/rest/api/food-items/Margherita Pizza")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.foodName").value("Margherita Pizza"))
                .andExpect(jsonPath("$.description").value("Classic pizza with tomato sauce, mozzarella, and basil"))
                .andExpect(jsonPath("$.price").value(12.99));

        verify(foodItemService).getFoodItemByName("Margherita Pizza");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should successfully delete food item as admin")
    void deleteFoodItem_asAdmin_success() throws Exception {
        // Arrange
        when(foodItemService.deleteFoodItemByName("Margherita Pizza")).thenReturn(testFoodItemDto);

        // Act & Assert
        mockMvc.perform(delete("/rest/api/food-items/Margherita Pizza")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.foodName").value("Margherita Pizza"));

        verify(foodItemService).deleteFoodItemByName("Margherita Pizza");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return bad request for invalid food item data")
    void saveFoodItem_invalidData_badRequest() throws Exception {
        // Arrange - Invalid food item with empty name and negative price
        FoodItemDto invalidFoodItem = new FoodItemDto();
        invalidFoodItem.setFoodName(""); // Invalid empty name
        invalidFoodItem.setPrice(BigDecimal.valueOf(-5.0)); // Invalid negative price

        // Act & Assert
        mockMvc.perform(post("/rest/api/food-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidFoodItem)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(foodItemService);
    }
}
