package com.ste.restaurant.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ste.restaurant.dto.OrderItemDtoBasic;
import com.ste.restaurant.dto.PlaceOrderDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class PlaceOrderTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldPlaceOrderSuccessfully() throws Exception {
            PlaceOrderDto placeOrderDto = new PlaceOrderDto();
            placeOrderDto.setNotes("Please make it spicy");
            placeOrderDto.setAddressId(1L);
            placeOrderDto.setTableNumber("7A");

            List<OrderItemDtoBasic> orderItems = new ArrayList<>();
            OrderItemDtoBasic item1 = new OrderItemDtoBasic();
            item1.setFoodName("Cheeseburger");
            item1.setQuantity(2);
            item1.setNote("Extra cheese");
            orderItems.add(item1);

            OrderItemDtoBasic item2 = new OrderItemDtoBasic();
            item2.setFoodName("Caesar Salad");
            item2.setQuantity(1);
            orderItems.add(item2);

            placeOrderDto.setOrderItems(orderItems);

            mockMvc.perform(post("/rest/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(placeOrderDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes").value("Please make it spicy"))
                    .andExpect(jsonPath("$.data.status").value("PLACED"));
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldAllowAdminToPlaceOrder() throws Exception {
            PlaceOrderDto placeOrderDto = new PlaceOrderDto();
            placeOrderDto.setNotes("Admin order");
            placeOrderDto.setTableNumber("7A");

            List<OrderItemDtoBasic> orderItems = new ArrayList<>();
            OrderItemDtoBasic item = new OrderItemDtoBasic();
            item.setFoodName("Cheeseburger");
            item.setQuantity(1);
            orderItems.add(item);

            placeOrderDto.setOrderItems(orderItems);

            mockMvc.perform(post("/rest/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(placeOrderDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes").value("Admin order"));
        }        @Test
        @WithMockUser(username = "bulent@restaurant.com", roles = "WAITER")
        void shouldAllowWaiterToPlaceOrder() throws Exception {
            PlaceOrderDto placeOrderDto = new PlaceOrderDto();
            placeOrderDto.setNotes("Waiter assisted order");
            placeOrderDto.setTableNumber("7A");

            List<OrderItemDtoBasic> orderItems = new ArrayList<>();
            OrderItemDtoBasic item = new OrderItemDtoBasic();
            item.setFoodName("Cheeseburger");
            item.setQuantity(1);
            orderItems.add(item);

            placeOrderDto.setOrderItems(orderItems);

            mockMvc.perform(post("/rest/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(placeOrderDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes").value("Waiter assisted order"));
        }

        @Test
        @WithMockUser(username = "rachel@hotmail.com", roles = "CHEF")
        void shouldAllowChefToPlaceOrder() throws Exception {
            PlaceOrderDto placeOrderDto = new PlaceOrderDto();
            placeOrderDto.setNotes("Chef order");
            placeOrderDto.setTableNumber("7A"); // Providing a table number

            List<OrderItemDtoBasic> orderItems = new ArrayList<>();
            OrderItemDtoBasic item = new OrderItemDtoBasic();
            item.setFoodName("Cheeseburger");
            item.setQuantity(1);
            orderItems.add(item);

            placeOrderDto.setOrderItems(orderItems);

            mockMvc.perform(post("/rest/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(placeOrderDto)))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldReturn403WhenUnauthenticatedUserTriesToPlaceOrder() throws Exception {
            PlaceOrderDto placeOrderDto = new PlaceOrderDto();

            List<OrderItemDtoBasic> orderItems = new ArrayList<>();
            OrderItemDtoBasic item = new OrderItemDtoBasic();
            item.setFoodName("Margherita Pizza");
            item.setQuantity(1);
            orderItems.add(item);

            placeOrderDto.setOrderItems(orderItems);

            mockMvc.perform(post("/rest/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(placeOrderDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldReturn400WhenPlacingOrderWithoutItems() throws Exception {
            PlaceOrderDto placeOrderDto = new PlaceOrderDto();
            placeOrderDto.setNotes("Order without items");
            placeOrderDto.setOrderItems(new ArrayList<>()); // Empty list

            mockMvc.perform(post("/rest/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(placeOrderDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class GetOrdersTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldGetCustomerOrdersSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldReturn400ForAdminOnGetOrders() throws Exception {
            mockMvc.perform(get("/rest/api/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("USER_ROLE_INVALID"));
        }

        @Test
        @WithMockUser(username = "bulent@restaurant.com", roles = "WAITER")
        void shouldGetWaiterOrdersSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(username = "rachel@hotmail.com", roles = "CHEF")
        void shouldGetChefOrdersSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldReturn403WhenUnauthenticatedUserTriesToGetOrders() throws Exception {
            mockMvc.perform(get("/rest/api/orders")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class GetLastOrderTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldGetLastOrderSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/orders/last")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").exists())
                    .andExpect(jsonPath("$.status").exists());
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "ADMIN")
        void shouldGetLastOrderForAdmin() throws Exception {
            mockMvc.perform(get("/rest/api/orders/last")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").exists());
        }

        @Test
        @WithMockUser(username = "rachel@hotmail.com", roles = "CHEF")
        void shouldReturn404WhenChefHasNoOrders() throws Exception {
            mockMvc.perform(get("/rest/api/orders/last")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class GetOrderByIdTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldGetOrderByIdSuccessfully() throws Exception {
            // First get the last order to get a valid ID
            String lastOrderResponse = mockMvc.perform(get("/rest/api/orders/last")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            
            JsonNode lastOrder = objectMapper.readTree(lastOrderResponse);
            Long orderId = lastOrder.get("orderId").asLong();

            mockMvc.perform(get("/rest/api/orders/" + orderId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId));
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldGetOrderByIdForAdmin() throws Exception {
            // Should be able to access any order
            mockMvc.perform(get("/rest/api/orders/999999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound()); // Expect 404 for non-existent order
        }

        @Test
        @WithMockUser(username = "bulent@restaurant.com", roles = "WAITER")
        void shouldGetOrderByIdForWaiter() throws Exception {
            // Should be able to access any order
            mockMvc.perform(get("/rest/api/orders/999999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound()); // Expect 404 for non-existent order
        }

        @Test
        @WithMockUser(username = "rachel@hotmail.com", roles = "CHEF")
        void shouldReturn404WhenChefTriesToGetOrderById() throws Exception {
            mockMvc.perform(get("/rest/api/orders/999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            mockMvc.perform(get("/rest/api/orders/999999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class GetOrderItemsTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldGetOrderItemsSuccessfully() throws Exception {
            // First get the last order to get a valid ID
            String lastOrderResponse = mockMvc.perform(get("/rest/api/orders/last")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            
            JsonNode lastOrder = objectMapper.readTree(lastOrderResponse);
            Long orderId = lastOrder.get("orderId").asLong();

            mockMvc.perform(get("/rest/api/orders/" + orderId + "/items")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldGetOrderItemsForAdmin() throws Exception {
            // Test access control - should be able to access endpoint
            mockMvc.perform(get("/rest/api/orders/999999/items")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound()); // Order doesn't exist
        }

        @Test
        @WithMockUser(username = "bulent@restaurant.com", roles = "WAITER")
        void shouldGetOrderItemsForWaiter() throws Exception {
            // Test access control - should be able to access endpoint
            mockMvc.perform(get("/rest/api/orders/999999/items")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound()); // Order doesn't exist
        }

        @Test
        @WithMockUser(username = "rachel@hotmail.com", roles = "CHEF")
        void shouldReturn404WhenChefTriesToGetOrderItems() throws Exception {
            mockMvc.perform(get("/rest/api/orders/999/items")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldReturn404WhenOrderItemsNotFound() throws Exception {
            mockMvc.perform(get("/rest/api/orders/999999/items")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class CancelOrderTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldCancelOrderSuccessfully() throws Exception {
            mockMvc.perform(patch("/rest/api/orders/1/cancel")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldAllowAdminToCancelOrder() throws Exception {
            mockMvc.perform(patch("/rest/api/orders/1/cancel")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @WithMockUser(username = "bulent@restaurant.com", roles = "WAITER")
        void shouldAllowWaiterToCancelOrder() throws Exception {
            mockMvc.perform(patch("/rest/api/orders/1/cancel")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @WithMockUser(username = "rachel@hotmail.com", roles = "CHEF")
        void shouldReturn403WhenChefTriesToCancelOrder() throws Exception {
            mockMvc.perform(patch("/rest/api/orders/1/cancel")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldReturn404WhenCancellingNonExistentOrder() throws Exception {
            mockMvc.perform(patch("/rest/api/orders/999/cancel")
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn403WhenUnauthenticatedUserTriesToCancelOrder() throws Exception {
            mockMvc.perform(patch("/rest/api/orders/1/cancel")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
