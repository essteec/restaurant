package com.ste.restaurant.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ste.restaurant.TestConfig;
import com.ste.restaurant.dto.OrderItemDtoBasic;
import com.ste.restaurant.dto.PlaceOrderDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles("test")
@Import(TestConfig.class)
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
        // Fetch a real address for the logged-in customer to avoid hardcoding IDs
        String addressResponse = mockMvc.perform(get("/rest/api/addresses")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode addresses = objectMapper.readTree(addressResponse);
        Long addressId = addresses.get(0).get("addressId").asLong();

            PlaceOrderDto placeOrderDto = new PlaceOrderDto();
            placeOrderDto.setNotes("Please make it spicy");
        placeOrderDto.setAddressId(addressId);
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
    }

    @Test
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
            placeOrderDto.setTableNumber("7A"); // Providing a.java table number

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
    @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldGetLastOrderForAdmin() throws Exception {
        PlaceOrderDto placeOrderDto = new PlaceOrderDto();
        placeOrderDto.setNotes("Seed order for admin last order test");
        placeOrderDto.setTableNumber("7A");

        List<OrderItemDtoBasic> items = new ArrayList<>();
        OrderItemDtoBasic item = new OrderItemDtoBasic();
        item.setFoodName("Cheeseburger");
        item.setQuantity(1);
        items.add(item);
        placeOrderDto.setOrderItems(items);

        // Temporarily impersonate a customer to place order
        // This endpoint requires authentication; use existing mock customer context
        // by calling place order within a nested request using perform with user() is non-trivial here,
        // so instead we just proceed as admin; service allows admin to place orders.
        mockMvc.perform(post("/rest/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(placeOrderDto)))
            .andExpect(status().isOk());

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
            // Given: Get user's first address to use for order
            String addressResponse = mockMvc.perform(get("/rest/api/addresses")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            
            JsonNode addresses = objectMapper.readTree(addressResponse);
            Long addressId = addresses.get(0).get("addressId").asLong();

            // Given: Place an order first
            PlaceOrderDto placeOrderDto = new PlaceOrderDto();
            placeOrderDto.setNotes("Order for get by ID test");
            placeOrderDto.setAddressId(addressId); // Use actual address ID
            placeOrderDto.setTableNumber("7A"); // Assuming table 7A exists

            List<OrderItemDtoBasic> orderItems = new ArrayList<>();
            OrderItemDtoBasic item1 = new OrderItemDtoBasic();
            item1.setFoodName("Cheeseburger"); // Assuming Cheeseburger exists
            item1.setQuantity(2);
            orderItems.add(item1);
            placeOrderDto.setOrderItems(orderItems);

            String placeOrderResponse = mockMvc.perform(post("/rest/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(placeOrderDto)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            JsonNode placedOrder = objectMapper.readTree(placeOrderResponse).get("data");
            Long orderId = placedOrder.get("orderId").asLong();

            // When: Get the order by ID
            mockMvc.perform(get("/rest/api/orders/" + orderId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId))
                    .andExpect(jsonPath("$.notes").value("Order for get by ID test"))
                    .andExpect(jsonPath("$.orderItems.length()").value(1))
                    .andExpect(jsonPath("$.orderItems[0].foodItem.foodName").value("Cheeseburger"));
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
            // Given: Get user's first address to use for order
            String addressResponse = mockMvc.perform(get("/rest/api/addresses")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            
            JsonNode addresses = objectMapper.readTree(addressResponse);
            Long addressId = addresses.get(0).get("addressId").asLong();

            // Given: Place an order first
            PlaceOrderDto placeOrderDto = new PlaceOrderDto();
            placeOrderDto.setNotes("Order for items test");
            placeOrderDto.setAddressId(addressId); // Use actual address ID
            placeOrderDto.setTableNumber("7A"); // Assuming table 7A exists

            List<OrderItemDtoBasic> orderItems = new ArrayList<>();
            OrderItemDtoBasic item1 = new OrderItemDtoBasic();
            item1.setFoodName("Cheeseburger"); // Assuming Cheeseburger exists
            item1.setQuantity(2);
            orderItems.add(item1);

            OrderItemDtoBasic item2 = new OrderItemDtoBasic();
            item2.setFoodName("Caesar Salad"); // Assuming Caesar Salad exists
            item2.setQuantity(1);
            orderItems.add(item2);
            placeOrderDto.setOrderItems(orderItems);

            String placeOrderResponse = mockMvc.perform(post("/rest/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(placeOrderDto)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            JsonNode placedOrder = objectMapper.readTree(placeOrderResponse).get("data");
            Long orderId = placedOrder.get("orderId").asLong();

            // When: Get order items for the placed order
            mockMvc.perform(get("/rest/api/orders/" + orderId + "/items")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2)) // Expect 2 items
                    .andExpect(jsonPath("$[0].foodItem.foodName").value("Cheeseburger"))
                    .andExpect(jsonPath("$[0].quantity").value(2))
                    .andExpect(jsonPath("$[1].foodItem.foodName").value("Caesar Salad"))
                    .andExpect(jsonPath("$[1].quantity").value(1));
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
            // Given: Get user's first address to use for order
            String addressResponse = mockMvc.perform(get("/rest/api/addresses")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            
            JsonNode addresses = objectMapper.readTree(addressResponse);
            Long addressId = addresses.get(0).get("addressId").asLong();

            // Given: Place an order first
            PlaceOrderDto placeOrderDto = new PlaceOrderDto();
            placeOrderDto.setNotes("Order to be cancelled");
            placeOrderDto.setAddressId(addressId); // Use actual address ID
            placeOrderDto.setTableNumber("7A"); // Assuming table 7A exists

            List<OrderItemDtoBasic> orderItems = new ArrayList<>();
            OrderItemDtoBasic item1 = new OrderItemDtoBasic();
            item1.setFoodName("Cheeseburger"); // Assuming Cheeseburger exists
            item1.setQuantity(1);
            orderItems.add(item1);
            placeOrderDto.setOrderItems(orderItems);

            String placeOrderResponse = mockMvc.perform(post("/rest/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(placeOrderDto)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            JsonNode placedOrder = objectMapper.readTree(placeOrderResponse).get("data");
            Long orderId = placedOrder.get("orderId").asLong();

            // When: Attempt to cancel the placed order
            mockMvc.perform(patch("/rest/api/orders/" + orderId + "/cancel")
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
