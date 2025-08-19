package com.ste.restaurant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ste.restaurant.dto.OrderDto;
import com.ste.restaurant.dto.OrderItemDto;
import com.ste.restaurant.dto.FoodItemDto;
import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.entity.OrderStatus;
import com.ste.restaurant.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for EmployeeOrderController REST endpoints.
 * Uses full Spring Boot context with mocked service layer.
 * 
 * Tests cover:
 * - Order management operations (Get, Delete, Update Status)
 * - Order item retrieval and pagination
 * - Role-based security (@PreAuthorize)
 * - Table assignment changes
 * - Request/Response JSON mapping
 * - Error handling and validation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EmployeeOrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Nested
    @DisplayName("Delete Order Tests")
    class DeleteOrderTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete order successfully as admin")
        void shouldDeleteOrderSuccessfullyAsAdmin() throws Exception {
            // Given
            Long orderId = 1L;
            OrderDto deletedOrder = createMockOrderDto(orderId, "CANCELLED");
            
            when(orderService.deleteOrderById(orderId)).thenReturn(deletedOrder);

            // When & Then
            mockMvc.perform(delete("/rest/api/employee/orders/{id}", orderId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId))
                    .andExpect(jsonPath("$.status").value("CANCELLED"));

            verify(orderService).deleteOrderById(orderId);
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should return forbidden when waiter tries to delete order")
        void shouldReturnForbiddenWhenWaiterTriesToDeleteOrder() throws Exception {
            // When & Then
            mockMvc.perform(delete("/rest/api/employee/orders/{id}", 1L)
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).deleteOrderById(any());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        @DisplayName("Should return forbidden when chef tries to delete order")
        void shouldReturnForbiddenWhenChefTriesToDeleteOrder() throws Exception {
            // When & Then
            mockMvc.perform(delete("/rest/api/employee/orders/{id}", 1L)
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).deleteOrderById(any());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return forbidden when customer tries to delete order")
        void shouldReturnForbiddenWhenCustomerTriesToDeleteOrder() throws Exception {
            // When & Then
            mockMvc.perform(delete("/rest/api/employee/orders/{id}", 1L)
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).deleteOrderById(any());
        }

        @Test
        @DisplayName("Should return unauthorized when not authenticated")
        void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
            // When & Then
            mockMvc.perform(delete("/rest/api/employee/orders/{id}", 1L)
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).deleteOrderById(any());
        }
    }

    @Nested
    @DisplayName("Get Order Items Tests")
    class GetOrderItemsTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get order items successfully as admin")
        void shouldGetOrderItemsSuccessfullyAsAdmin() throws Exception {
            // Given
            Long orderId = 1L;
            List<OrderItemDto> orderItems = Arrays.asList(
                    createMockOrderItemDto(1L, "Burger", 2),
                    createMockOrderItemDto(2L, "Fries", 1)
            );
            
            when(orderService.getOrderItemsFromOrder(orderId)).thenReturn(orderItems);

            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders/{orderId}/items", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].orderItemId").value(1))
                    .andExpect(jsonPath("$[0].foodItem.foodName").value("Burger"))
                    .andExpect(jsonPath("$[0].quantity").value(2))
                    .andExpect(jsonPath("$[1].orderItemId").value(2))
                    .andExpect(jsonPath("$[1].foodItem.foodName").value("Fries"))
                    .andExpect(jsonPath("$[1].quantity").value(1));

            verify(orderService).getOrderItemsFromOrder(orderId);
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should get order items successfully as waiter")
        void shouldGetOrderItemsSuccessfullyAsWaiter() throws Exception {
            // Given
            Long orderId = 1L;
            List<OrderItemDto> orderItems = Arrays.asList(
                    createMockOrderItemDto(1L, "Pizza", 1)
            );
            
            when(orderService.getOrderItemsFromOrder(orderId)).thenReturn(orderItems);

            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders/{orderId}/items", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].foodItem.foodName").value("Pizza"));

            verify(orderService).getOrderItemsFromOrder(orderId);
        }

        @Test
        @WithMockUser(roles = "CHEF")
        @DisplayName("Should get order items successfully as chef")
        void shouldGetOrderItemsSuccessfullyAsChef() throws Exception {
            // Given
            Long orderId = 1L;
            List<OrderItemDto> orderItems = Arrays.asList(
                    createMockOrderItemDto(1L, "Pasta", 2)
            );
            
            when(orderService.getOrderItemsFromOrder(orderId)).thenReturn(orderItems);

            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders/{orderId}/items", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].foodItem.foodName").value("Pasta"));

            verify(orderService).getOrderItemsFromOrder(orderId);
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return forbidden when customer tries to get order items")
        void shouldReturnForbiddenWhenCustomerTriesToGetOrderItems() throws Exception {
            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders/{orderId}/items", 1L))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).getOrderItemsFromOrder(any());
        }
    }

    @Nested
    @DisplayName("Get Order Item List Tests")
    class GetOrderItemListTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get paginated order item list successfully as admin")
        void shouldGetPaginatedOrderItemListSuccessfullyAsAdmin() throws Exception {
            // Given
            List<OrderItemDto> orderItems = Arrays.asList(
                    createMockOrderItemDto(1L, "Burger", 2),
                    createMockOrderItemDto(2L, "Salad", 1)
            );
            Page<OrderItemDto> page = new PageImpl<>(orderItems, PageRequest.of(0, 20), 2);
            
            when(orderService.getOrderItemList(any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders/items")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].foodItem.foodName").value("Burger"))
                    .andExpect(jsonPath("$.content[1].foodItem.foodName").value("Salad"))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.size").value(20));

            verify(orderService).getOrderItemList(any(Pageable.class));
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should return forbidden when waiter tries to get order item list")
        void shouldReturnForbiddenWhenWaiterTriesToGetOrderItemList() throws Exception {
            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders/items"))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).getOrderItemList(any());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        @DisplayName("Should return forbidden when chef tries to get order item list")
        void shouldReturnForbiddenWhenChefTriesToGetOrderItemList() throws Exception {
            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders/items"))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).getOrderItemList(any());
        }
    }

    @Nested
    @DisplayName("Get Order By ID Tests")
    class GetOrderByIdTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should get order by ID successfully as admin")
        void shouldGetOrderByIdSuccessfullyAsAdmin() throws Exception {
            // Given
            Long orderId = 1L;
            OrderDto order = createMockOrderDto(orderId, "PREPARING");
            
            when(orderService.getOrderById(orderId)).thenReturn(order);

            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders/{orderId}", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId))
                    .andExpect(jsonPath("$.status").value("PREPARING"))
                    .andExpect(jsonPath("$.totalPrice").value(25.50));

            verify(orderService).getOrderById(orderId);
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should get order by ID successfully as waiter")
        void shouldGetOrderByIdSuccessfullyAsWaiter() throws Exception {
            // Given
            Long orderId = 1L;
            OrderDto order = createMockOrderDto(orderId, "READY");
            
            when(orderService.getOrderById(orderId)).thenReturn(order);

            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders/{orderId}", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId))
                    .andExpect(jsonPath("$.status").value("READY"));

            verify(orderService).getOrderById(orderId);
        }

        @Test
        @WithMockUser(roles = "CHEF")
        @DisplayName("Should get order by ID successfully as chef")
        void shouldGetOrderByIdSuccessfullyAsChef() throws Exception {
            // Given
            Long orderId = 1L;
            OrderDto order = createMockOrderDto(orderId, "PLACED");
            
            when(orderService.getOrderById(orderId)).thenReturn(order);

            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders/{orderId}", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId))
                    .andExpect(jsonPath("$.status").value("PLACED"));

            verify(orderService).getOrderById(orderId);
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return forbidden when customer tries to get order")
        void shouldReturnForbiddenWhenCustomerTriesToGetOrder() throws Exception {
            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders/{orderId}", 1L))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).getOrderById(any(Long.class));
        }
    }

    @Nested
    @DisplayName("Update Order Status Tests")
    class UpdateOrderStatusTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update order status successfully as admin")
        void shouldUpdateOrderStatusSuccessfullyAsAdmin() throws Exception {
            // Given
            Long orderId = 1L;
            StringDto statusDto = new StringDto();
            statusDto.setName("READY");
            OrderDto updatedOrder = createMockOrderDto(orderId, "READY");
            
            when(orderService.updateOrderStatus(eq(orderId), any(StringDto.class))).thenReturn(updatedOrder);

            // When & Then
            mockMvc.perform(patch("/rest/api/employee/orders/{orderId}/status", orderId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId))
                    .andExpect(jsonPath("$.status").value("READY"));

            verify(orderService).updateOrderStatus(eq(orderId), any(StringDto.class));
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should update order status successfully as waiter")
        void shouldUpdateOrderStatusSuccessfullyAsWaiter() throws Exception {
            // Given
            Long orderId = 1L;
            StringDto statusDto = new StringDto();
            statusDto.setName("SERVED");
            OrderDto updatedOrder = createMockOrderDto(orderId, "SERVED");
            
            when(orderService.updateOrderStatus(eq(orderId), any(StringDto.class))).thenReturn(updatedOrder);

            // When & Then
            mockMvc.perform(patch("/rest/api/employee/orders/{orderId}/status", orderId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SERVED"));

            verify(orderService).updateOrderStatus(eq(orderId), any(StringDto.class));
        }

        @Test
        @WithMockUser(roles = "CHEF")
        @DisplayName("Should update order status successfully as chef")
        void shouldUpdateOrderStatusSuccessfullyAsChef() throws Exception {
            // Given
            Long orderId = 1L;
            StringDto statusDto = new StringDto();
            statusDto.setName("PREPARING");
            OrderDto updatedOrder = createMockOrderDto(orderId, "PREPARING");
            
            when(orderService.updateOrderStatus(eq(orderId), any(StringDto.class))).thenReturn(updatedOrder);

            // When & Then
            mockMvc.perform(patch("/rest/api/employee/orders/{orderId}/status", orderId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PREPARING"));

            verify(orderService).updateOrderStatus(eq(orderId), any(StringDto.class));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return forbidden when customer tries to update order status")
        void shouldReturnForbiddenWhenCustomerTriesToUpdateOrderStatus() throws Exception {
            // Given
            StringDto statusDto = new StringDto();
            statusDto.setName("READY");

            // When & Then
            mockMvc.perform(patch("/rest/api/employee/orders/{orderId}/status", 1L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDto)))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).updateOrderStatus(any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request for invalid status update request")
        void shouldReturnBadRequestForInvalidStatusUpdateRequest() throws Exception {
            // Given - Empty status dto
            StringDto statusDto = new StringDto();
            statusDto.setName("");

            // When & Then
            mockMvc.perform(patch("/rest/api/employee/orders/{orderId}/status", 1L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(statusDto)))
                    .andExpect(status().isBadRequest());

            verify(orderService, never()).updateOrderStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("Get All Orders Tests")
    class GetAllOrdersTests {

        @Test
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        @DisplayName("Should get all orders without status filter as admin")
        void shouldGetAllOrdersWithoutStatusFilterAsAdmin() throws Exception {
            // Given
            List<OrderDto> orders = Arrays.asList(
                    createMockOrderDto(1L, "PLACED"),
                    createMockOrderDto(2L, "PREPARING")
            );
            Page<OrderDto> page = new PageImpl<>(orders, PageRequest.of(0, 20), 2);
            
            when(orderService.getOrderList(any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].status").value("PLACED"))
                    .andExpect(jsonPath("$.content[1].status").value("PREPARING"));

            verify(orderService).getOrderList(any(Pageable.class));
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = "ROLE_ADMIN")
        @DisplayName("Should get orders filtered by status as admin")
        void shouldGetOrdersFilteredByStatusAsAdmin() throws Exception {
            // Given
            List<OrderDto> orders = Arrays.asList(
                    createMockOrderDto(1L, "READY")
            );
            Page<OrderDto> page = new PageImpl<>(orders, PageRequest.of(0, 20), 1);
            
            when(orderService.getAllOrdersBy(eq("READY"), any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders")
                            .param("status", "READY")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].status").value("READY"));

            verify(orderService).getAllOrdersBy(eq("READY"), any(Pageable.class));
        }

        @Test
        @WithMockUser(username = "chef@example.com", authorities = "ROLE_CHEF")
        @DisplayName("Should get chef-specific orders as chef")
        void jshouldGetChefSpecificOrdersAsChef() throws Exception {
            // Given
            List<OrderDto> orders = Arrays.asList(
                    createMockOrderDto(1L, "PLACED"),
                    createMockOrderDto(2L, "PREPARING")
            );
            Page<OrderDto> page = new PageImpl<>(orders, PageRequest.of(0, 20), 2);
            
            when(orderService.getAllOrdersBy(any(List.class), any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2));

            verify(orderService).getAllOrdersBy(any(List.class), any(Pageable.class));
        }

        @Test
        @WithMockUser(username = "waiter@example.com", authorities = "ROLE_WAITER")
        @DisplayName("Should get all orders as waiter")
        void shouldGetAllOrdersAsWaiter() throws Exception {
            // Given
            List<OrderDto> orders = Arrays.asList(
                    createMockOrderDto(1L, "READY")
            );
            Page<OrderDto> page = new PageImpl<>(orders, PageRequest.of(0, 20), 1);
            
            when(orderService.getAllOrdersBy(any(List.class), any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));

            verify(orderService).getAllOrdersBy(any(List.class), any(Pageable.class));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return forbidden when customer tries to get all orders")
        void shouldReturnForbiddenWhenCustomerTriesToGetAllOrders() throws Exception {
            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders"))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).getOrderList(any(Pageable.class));
            verify(orderService, never()).getAllOrdersBy(any(String.class), any(Pageable.class));
            verify(orderService, never()).getAllOrdersBy(any(List.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Change Table Tests")
    class ChangeTableTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should change table of order successfully as admin")
        void shouldChangeTableOfOrderSuccessfullyAsAdmin() throws Exception {
            // Given
            Long orderId = 1L;
            StringDto tableDto = new StringDto();
            tableDto.setName("5B");
            OrderDto updatedOrder = createMockOrderDto(orderId, "PLACED");
            
            when(orderService.changeTableOfOrder(eq(orderId), any(StringDto.class))).thenReturn(updatedOrder);

            // When & Then
            mockMvc.perform(patch("/rest/api/employee/orders/{orderId}/table", orderId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tableDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId));

            verify(orderService).changeTableOfOrder(eq(orderId), any(StringDto.class));
        }

        @Test
        @WithMockUser(roles = "WAITER")
        @DisplayName("Should change table of order successfully as waiter")
        void shouldChangeTableOfOrderSuccessfullyAsWaiter() throws Exception {
            // Given
            Long orderId = 1L;
            StringDto tableDto = new StringDto();
            tableDto.setName("3A");
            OrderDto updatedOrder = createMockOrderDto(orderId, "PLACED");
            
            when(orderService.changeTableOfOrder(eq(orderId), any(StringDto.class))).thenReturn(updatedOrder);

            // When & Then
            mockMvc.perform(patch("/rest/api/employee/orders/{orderId}/table", orderId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tableDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId));

            verify(orderService).changeTableOfOrder(eq(orderId), any(StringDto.class));
        }

        @Test
        @WithMockUser(username = "fatma@gmail.com", roles = "CHEF")
        @DisplayName("Should return forbidden when chef tries to change table of order")
        void shouldReturn403WhenChefTriesToChangeTableOfOrder() throws Exception {
            // Given
            Long orderId = 1L; // Use a dummy ID as the service call will be mocked
            StringDto tableNumberDto = new StringDto();
            tableNumberDto.setName("2C"); // Dummy table number

            // When & Then
            mockMvc.perform(patch("/rest/api/employee/orders/" + orderId + "/table")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tableNumberDto)))
                    .andExpect(status().isForbidden());

            // Verify that the service method was NOT called
            verify(orderService, never()).changeTableOfOrder(any(Long.class), any(StringDto.class));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("Should return forbidden when customer tries to change table")
        void shouldReturnForbiddenWhenCustomerTriesToChangeTable() throws Exception {
            // Given
            StringDto tableDto = new StringDto();
            tableDto.setName("1A");

            // When & Then
            mockMvc.perform(patch("/rest/api/employee/orders/{orderId}/table", 1L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tableDto)))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).changeTableOfOrder(any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request for invalid table change request")
        void shouldReturnBadRequestForInvalidTableChangeRequest() throws Exception {
            // Given - Empty table dto
            StringDto tableDto = new StringDto();
            tableDto.setName("");

            // When & Then
            mockMvc.perform(patch("/rest/api/employee/orders/{orderId}/table", 1L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tableDto)))
                    .andExpect(status().isBadRequest());

            verify(orderService, never()).changeTableOfOrder(any(), any());
        }
    }

    @Nested
    @DisplayName("Validation and Error Handling Tests")
    class ValidationAndErrorHandlingTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request for invalid order ID format")
        void shouldReturnBadRequestForInvalidOrderIdFormat() throws Exception {
            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders/{orderId}", "invalid"))
                    .andExpect(status().isBadRequest());

            verify(orderService, never()).getOrderById(any(Long.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle missing request body for status update")
        void shouldHandleMissingRequestBodyForStatusUpdate() throws Exception {
            // When & Then
            mockMvc.perform(patch("/rest/api/employee/orders/{orderId}/status", 1L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(orderService, never()).updateOrderStatus(any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle missing request body for table change")
        void shouldHandleMissingRequestBodyForTableChange() throws Exception {
            // When & Then
            mockMvc.perform(patch("/rest/api/employee/orders/{orderId}/table", 1L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(orderService, never()).changeTableOfOrder(any(), any());
        }

                @Test
        @WithMockUser(authorities = "ROLE_ADMIN")
        @DisplayName("Should handle pagination parameters correctly")
        void shouldHandlePaginationParametersCorrectly() throws Exception {
            // Given
            List<OrderDto> orders = Arrays.asList(createMockOrderDto(1L, "PLACED"));
            Page<OrderDto> page = new PageImpl<>(orders, PageRequest.of(1, 10), 11);
            
            when(orderService.getOrderList(any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/rest/api/employee/orders")
                            .param("page", "1")
                            .param("size", "10")
                            .param("sort", "orderTime,desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.number").value(1))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(11));

            verify(orderService).getOrderList(any(Pageable.class));
        }
    }

    // Helper methods for creating mock data
    private OrderDto createMockOrderDto(Long id, String status) {
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderId(id);
        orderDto.setStatus(status);
        orderDto.setTotalPrice(BigDecimal.valueOf(25.50));
        orderDto.setOrderTime(LocalDateTime.now());
        orderDto.setNotes("Test order");
        return orderDto;
    }

    private OrderItemDto createMockOrderItemDto(Long id, String foodName, Integer quantity) {
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setOrderItemId(id);
        orderItemDto.setQuantity(quantity);
        orderItemDto.setUnitPrice(BigDecimal.valueOf(12.75));
        orderItemDto.setTotalPrice(BigDecimal.valueOf(12.75).multiply(BigDecimal.valueOf(quantity)));
        orderItemDto.setNote("Test note");
        
        // Create a mock FoodItemDto
        FoodItemDto foodItemDto = new FoodItemDto();
        foodItemDto.setFoodName(foodName);
        orderItemDto.setFoodItem(foodItemDto);
        
        return orderItemDto;
    }
}
