package com.ste.restaurant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ste.restaurant.dto.TableTopDto;
import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.entity.enums.TableStatus;
import com.ste.restaurant.service.TableTopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TableTopController REST endpoints.
 * Uses full Spring Boot context with mocked service layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TableTop Controller Integration Tests")
class TableTopControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TableTopService tableTopService;

    @Autowired
    private ObjectMapper objectMapper;

    private TableTopDto testTableDto;

    @BeforeEach
    void setUp() {
        testTableDto = new TableTopDto();
        testTableDto.setTableNumber("T01");
        testTableDto.setCapacity(4);
        testTableDto.setTableStatus(TableStatus.AVAILABLE);
    }

    @Nested
    @DisplayName("Create Table Tests")
    class CreateTableTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create table when admin user creates table")
        void saveTable_asAdmin_success() throws Exception {
            // Arrange
            when(tableTopService.saveTable(any(TableTopDto.class))).thenReturn(testTableDto);

            // Act & Assert
            mockMvc.perform(post("/rest/api/tables")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testTableDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.tableNumber").value("T01"))
                    .andExpect(jsonPath("$.capacity").value(4))
                    .andExpect(jsonPath("$.tableStatus").value("AVAILABLE"));

            verify(tableTopService).saveTable(any(TableTopDto.class));
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should deny access when waiter tries to create table")
        void saveTable_asWaiter_accessDenied() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/rest/api/tables")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testTableDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access denied"));

            verifyNoInteractions(tableTopService);
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should deny access when customer tries to create table")
        void saveTable_asCustomer_accessDenied() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/rest/api/tables")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testTableDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access denied"));

            verifyNoInteractions(tableTopService);
        }

        @Test
        @DisplayName("Should deny access when unauthenticated user tries to create table")
        void saveTable_unauthenticated_accessDenied() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/rest/api/tables")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testTableDto)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(tableTopService);
        }
    }

    @Nested
    @DisplayName("Get Table Tests")
    class GetTableTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get table by name when admin requests")
        void getTableByName_asAdmin_success() throws Exception {
            // Arrange
            when(tableTopService.getTableByName("T01")).thenReturn(testTableDto);

            // Act & Assert
            mockMvc.perform(get("/rest/api/tables/by-name")
                            .param("name", "T01")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tableNumber").value("T01"))
                    .andExpect(jsonPath("$.capacity").value(4));

            verify(tableTopService).getTableByName("T01");
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should get table by name when waiter requests")
        void getTableByName_asWaiter_success() throws Exception {
            // Arrange
            when(tableTopService.getTableByName("T01")).thenReturn(testTableDto);

            // Act & Assert
            mockMvc.perform(get("/rest/api/tables/by-name")
                            .param("name", "T01")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tableNumber").value("T01"));

            verify(tableTopService).getTableByName("T01");
        }
    }

    @Nested
    @DisplayName("Get All Tables Tests")
    class GetAllTablesTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get all tables when admin requests")
        void getAllTables_asAdmin_success() throws Exception {
            // Arrange
            TableTopDto table2 = new TableTopDto();
            table2.setTableNumber("T02");
            table2.setCapacity(6);
            table2.setTableStatus(TableStatus.OCCUPIED);

            List<TableTopDto> tables = Arrays.asList(testTableDto, table2);
            when(tableTopService.getAllTables()).thenReturn(tables);

            // Act & Assert
            mockMvc.perform(get("/rest/api/tables")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].tableNumber").value("T01"))
                    .andExpect(jsonPath("$[1].tableNumber").value("T02"));

            verify(tableTopService).getAllTables();
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should get all tables when waiter requests")
        void getAllTables_asWaiter_success() throws Exception {
            // Arrange
            List<TableTopDto> tables = Arrays.asList(testTableDto);
            when(tableTopService.getAllTables()).thenReturn(tables);

            // Act & Assert
            mockMvc.perform(get("/rest/api/tables")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(tableTopService).getAllTables();
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should deny access when customer tries to get all tables")
        void getAllTables_asCustomer_accessDenied() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/rest/api/tables")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access denied"));

            verifyNoInteractions(tableTopService);
        }
    }

    @Nested
    @DisplayName("Update Table Tests")
    class UpdateTableTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update table when admin updates table")
        void updateTable_asAdmin_success() throws Exception {
            // Arrange
            TableTopDto updatedTable = new TableTopDto();
            updatedTable.setTableNumber("T01");
            updatedTable.setCapacity(6);
            updatedTable.setTableStatus(TableStatus.AVAILABLE);

            when(tableTopService.updateTable(eq("T01"), any(TableTopDto.class))).thenReturn(updatedTable);

            // Act & Assert
            mockMvc.perform(put("/rest/api/tables/T01")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testTableDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tableNumber").value("T01"))
                    .andExpect(jsonPath("$.capacity").value(6));

            verify(tableTopService).updateTable(eq("T01"), any(TableTopDto.class));
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should deny access when waiter tries to update table")
        void updateTable_asWaiter_accessDenied() throws Exception {
            // Act & Assert
            mockMvc.perform(put("/rest/api/tables/T01")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testTableDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access denied"));

            verifyNoInteractions(tableTopService);
        }
    }

    @Nested
    @DisplayName("Update Table Status Tests")
    class UpdateTableStatusTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update table status when admin updates status")
        void updateTableStatus_asAdmin_success() throws Exception {
            // Arrange
            StringDto statusDto = new StringDto("OCCUPIED");
            TableTopDto updatedTable = new TableTopDto();
            updatedTable.setTableNumber("T01");
            updatedTable.setCapacity(4);
            updatedTable.setTableStatus(TableStatus.OCCUPIED);

            when(tableTopService.updateTableStatusByName(eq("T01"), any(StringDto.class))).thenReturn(updatedTable);

            // Act & Assert
            mockMvc.perform(patch("/rest/api/tables/T01/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tableStatus").value("OCCUPIED"));

            verify(tableTopService).updateTableStatusByName(eq("T01"), any(StringDto.class));
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should update table status when waiter updates status")
        void updateTableStatus_asWaiter_success() throws Exception {
            // Arrange
            StringDto statusDto = new StringDto("DIRTY");
            TableTopDto updatedTable = new TableTopDto();
            updatedTable.setTableNumber("T01");
            updatedTable.setCapacity(4);
            updatedTable.setTableStatus(TableStatus.DIRTY);

            when(tableTopService.updateTableStatusByName(eq("T01"), any(StringDto.class))).thenReturn(updatedTable);

            // Act & Assert
            mockMvc.perform(patch("/rest/api/tables/T01/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tableStatus").value("DIRTY"));

            verify(tableTopService).updateTableStatusByName(eq("T01"), any(StringDto.class));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should deny access when customer tries to update table status")
        void updateTableStatus_asCustomer_accessDenied() throws Exception {
            // Arrange
            StringDto statusDto = new StringDto("OCCUPIED");

            // Act & Assert
            mockMvc.perform(patch("/rest/api/tables/T01/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access denied"));

            verifyNoInteractions(tableTopService);
        }
    }

    @Nested
    @DisplayName("Delete Table Tests")
    class DeleteTableTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete table when admin deletes table")
        void deleteTable_asAdmin_success() throws Exception {
            // Arrange
            when(tableTopService.deleteTableByName("T01")).thenReturn(testTableDto);

            // Act & Assert
            mockMvc.perform(delete("/rest/api/tables/T01")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tableNumber").value("T01"));

            verify(tableTopService).deleteTableByName("T01");
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should deny access when waiter tries to delete table")
        void deleteTable_asWaiter_accessDenied() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/rest/api/tables/T01")
                            .with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Access denied"));

            verifyNoInteractions(tableTopService);
        }
    }

    @Nested
    @DisplayName("Get Available Tables Tests")
    class GetAvailableTablesTests {

        @Test
        @DisplayName("Should get available tables when public access")
        void getAvailableTables_publicAccess_success() throws Exception {
            // Arrange
            List<TableTopDto> availableTables = Arrays.asList(testTableDto);
            when(tableTopService.getAvailableTables()).thenReturn(availableTables);

            // Act & Assert
            mockMvc.perform(get("/rest/api/tables/available")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].tableStatus").value("AVAILABLE"));

            verify(tableTopService).getAvailableTables();
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should get available tables when customer requests")
        void getAvailableTables_asCustomer_success() throws Exception {
            // Arrange
            List<TableTopDto> availableTables = Arrays.asList(testTableDto);
            when(tableTopService.getAvailableTables()).thenReturn(availableTables);

            // Act & Assert
            mockMvc.perform(get("/rest/api/tables/available")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            verify(tableTopService).getAvailableTables();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get available tables when admin requests")
        void getAvailableTables_asAdmin_success() throws Exception {
            // Arrange
            List<TableTopDto> availableTables = Arrays.asList(testTableDto);
            when(tableTopService.getAvailableTables()).thenReturn(availableTables);

            // Act & Assert
            mockMvc.perform(get("/rest/api/tables/available")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            verify(tableTopService).getAvailableTables();
        }
    }
}
