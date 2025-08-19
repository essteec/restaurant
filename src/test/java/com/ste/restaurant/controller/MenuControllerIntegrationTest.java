package com.ste.restaurant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ste.restaurant.dto.MenuDtoBasic;
import com.ste.restaurant.dto.common.StringsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("MenuController Integration Tests")
class MenuControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Create Menu Tests")
    class CreateMenuTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create menu successfully when user is admin")
        void shouldCreateMenuSuccessfully() throws Exception {
            MenuDtoBasic menuDto = new MenuDtoBasic("Test Menu " + System.currentTimeMillis(), "Test menu for integration test");

            mockMvc.perform(post("/rest/api/menus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(menuDto))
                    .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return forbidden when user is not admin")
        void shouldReturnForbiddenForNonAdmin() throws Exception {
            MenuDtoBasic menuDto = new MenuDtoBasic("Test Menu User", "Test menu description");

            mockMvc.perform(post("/rest/api/menus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(menuDto))
                    .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return forbidden when user is not authenticated")
        void shouldReturnForbiddenForUnauthenticatedUser() throws Exception {
            MenuDtoBasic menuDto = new MenuDtoBasic("Dinner Menu", "Evening dinner selection");

            mockMvc.perform(post("/rest/api/menus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(menuDto))
                    .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request when menu name is blank")
        void shouldReturnBadRequestForBlankMenuName() throws Exception {
            MenuDtoBasic menuDto = new MenuDtoBasic("", "Evening dinner selection");

            mockMvc.perform(post("/rest/api/menus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(menuDto))
                    .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get All Menus Tests")
    class GetAllMenusTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get all menus successfully when user is admin")
        void shouldGetAllMenusSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/menus")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return forbidden when user is not admin")
        void shouldReturnForbiddenForNonAdmin() throws Exception {
            mockMvc.perform(get("/rest/api/menus")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return forbidden when user is not authenticated")
        void shouldReturnForbiddenForUnauthenticatedUser() throws Exception {
            mockMvc.perform(get("/rest/api/menus")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Get Menu by Name Tests")
    class GetMenuByNameTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get menu by name successfully when user is admin")
        void shouldGetMenuByNameSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/menus/by-name")
                    .param("name", "Dinner Menu")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return forbidden when user is not admin")
        void shouldReturnForbiddenForNonAdmin() throws Exception {
            mockMvc.perform(get("/rest/api/menus/by-name")
                    .param("name", "Dinner Menu")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Delete Menu Tests")
    class DeleteMenuTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete menu successfully when user is admin")
        void shouldDeleteMenuSuccessfully() throws Exception {
            // First create a menu to delete
            MenuDtoBasic menuDto = new MenuDtoBasic("Test Menu", "Test description");
            mockMvc.perform(post("/rest/api/menus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(menuDto))
                    .with(csrf()))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/rest/api/menus/{name}", "Test Menu")
                    .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return forbidden when user is not admin")
        void shouldReturnForbiddenForNonAdmin() throws Exception {
            mockMvc.perform(delete("/rest/api/menus/{name}", "Test Menu")
                    .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Update Menu Tests")
    class UpdateMenuTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update menu successfully when user is admin")
        void shouldUpdateMenuSuccessfully() throws Exception {
            // First create a menu to update
            MenuDtoBasic originalMenu = new MenuDtoBasic("Test Menu", "Original description");
            mockMvc.perform(post("/rest/api/menus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(originalMenu))
                    .with(csrf()))
                    .andExpect(status().isOk());

            MenuDtoBasic updatedMenu = new MenuDtoBasic("Updated Test Menu", "Updated description");

            mockMvc.perform(put("/rest/api/menus/{name}", "Test Menu")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedMenu))
                    .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return forbidden when user is not admin")
        void shouldReturnForbiddenForNonAdmin() throws Exception {
            MenuDtoBasic menuDto = new MenuDtoBasic("Updated Menu", "Updated description");

            mockMvc.perform(put("/rest/api/menus/{name}", "Test Menu")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(menuDto))
                    .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request when menu name is blank")
        void shouldReturnBadRequestForBlankMenuName() throws Exception {
            MenuDtoBasic menuDto = new MenuDtoBasic("", "Updated evening dinner selection");

            mockMvc.perform(put("/rest/api/menus/Dinner%20Menu")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(menuDto))
                    .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Set Active Menu Tests")
    class SetActiveMenuTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should set active menus successfully when user is admin")
        void shouldSetActiveMenusSuccessfully() throws Exception {
            StringsDto menuNames = new StringsDto(Set.of("Dinner Menu", "Lunch Menu"));

            mockMvc.perform(post("/rest/api/menus/active")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(menuNames))
                    .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return forbidden when user is not admin")
        void shouldReturnForbiddenForNonAdmin() throws Exception {
            StringsDto menuNames = new StringsDto(Set.of("Dinner Menu", "Lunch Menu"));

            mockMvc.perform(post("/rest/api/menus/active")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(menuNames))
                    .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Menu Food Item Relationship Tests")
    class MenuFoodItemRelationshipTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should add food items to menu successfully when user is admin")
        void shouldAddFoodItemsToMenuSuccessfully() throws Exception {
            // First create a menu
            MenuDtoBasic menuDto = new MenuDtoBasic("Test Menu", "Test description");
            mockMvc.perform(post("/rest/api/menus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(menuDto))
                    .with(csrf()))
                    .andExpect(status().isOk());

            StringsDto foodNames = new StringsDto(Set.of("Margherita Pizza", "Caesar Salad"));

            mockMvc.perform(put("/rest/api/menus/{menuName}/food-items", "Test Menu")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(foodNames))
                    .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should remove food items from menu successfully when user is admin")
        void shouldRemoveFoodItemsFromMenuSuccessfully() throws Exception {
            // First create a menu and add items
            MenuDtoBasic menuDto = new MenuDtoBasic("Test Menu", "Test description");
            mockMvc.perform(post("/rest/api/menus")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(menuDto))
                    .with(csrf()))
                    .andExpect(status().isOk());

            StringsDto addFoods = new StringsDto(Set.of("Margherita Pizza"));
            mockMvc.perform(put("/rest/api/menus/{menuName}/food-items", "Test Menu")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addFoods))
                    .with(csrf()))
                    .andExpect(status().isOk());

            StringsDto removeFoods = new StringsDto(Set.of("Margherita Pizza"));
            mockMvc.perform(delete("/rest/api/menus/{menuName}/food-items", "Test Menu")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(removeFoods))
                    .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return forbidden when non-admin tries to modify menu food items")
        void shouldReturnForbiddenForNonAdminFoodItemModification() throws Exception {
            StringsDto foodNames = new StringsDto(Set.of("Margherita Pizza"));

            mockMvc.perform(put("/rest/api/menus/{menuName}/food-items", "Test Menu")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(foodNames))
                    .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Get Active Menu Tests")
    class GetActiveMenuTests {

        @Test
        @DisplayName("Should get active menu for public access")
        void shouldGetActiveMenuForPublicAccess() throws Exception {
            mockMvc.perform(get("/rest/api/menus/active")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should get active menu when user is authenticated")
        void shouldGetActiveMenuForAuthenticatedUser() throws Exception {
            mockMvc.perform(get("/rest/api/menus/active")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get active menu when user is admin")
        void shouldGetActiveMenuForAdmin() throws Exception {
            mockMvc.perform(get("/rest/api/menus/active")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }
}
