package com.ste.restaurant.integration;

import com.ste.restaurant.entity.*;
import com.ste.restaurant.repository.OrderRepository;
import com.ste.restaurant.repository.OrderItemRepository;
import com.ste.restaurant.utils.DatabaseTestUtils;
import com.ste.restaurant.utils.RepositoryTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive integration tests for OrderRepository.
 * Tests all custom query methods, relationships, and complex business scenarios.
 * This test class covers:
 * - Basic CRUD operations
 * - Customer-based queries
 * - Status-based filtering
 * - Time-range queries
 * - Pagination support
 * - Complex relationship queries
 * - Order lifecycle management
 * - Data integrity constraints
 */
@DisplayName("Order Repository Integration Tests")
class OrderRepositoryIntegrationTest extends RepositoryTestBase {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

        // Test entities
    private User testCustomer1;
    private User testCustomer2;
    private Address testAddress1;
    private Address testAddress2;
    private TableTop testTable1;
    private TableTop testTable2;
    private Category testCategory1;
    private Category testCategory2;
    private FoodItem testFoodItem1;
    private FoodItem testFoodItem2;

    @Override
    protected void setUp() {
        // Create test entities for each test
        setupTestData();
    }

    private void setupTestData() {
        // Create test customers with expected email addresses
        testCustomer1 = DatabaseTestUtils.createTestUser("john.doe@example.com", "John", "Doe", UserRole.CUSTOMER);
        testCustomer2 = DatabaseTestUtils.createTestUser("jane.smith@example.com", "Jane", "Smith", UserRole.CUSTOMER);
        
        // Create test addresses
        testAddress1 = DatabaseTestUtils.createTestAddress("123 Main St", "Istanbul", "Home Address", testCustomer1);
        testAddress2 = DatabaseTestUtils.createTestAddress("456 Oak Ave", "Ankara", "Work Address", testCustomer2);
        
        // Create test tables
        testTable1 = DatabaseTestUtils.createTestTable(1, 4);
        testTable2 = DatabaseTestUtils.createTestTable(2, 6);
        
        // Create test categories without relationships first
        testCategory1 = DatabaseTestUtils.createTestCategory("Main Course", "Main course dishes");
        testCategory2 = DatabaseTestUtils.createTestCategory("Drinks", "Beverages and drinks");
        
        // Create test food items with expected prices, without category relationships
        testFoodItem1 = DatabaseTestUtils.createTestFoodItem("Pizza", BigDecimal.valueOf(25.99), null);
        testFoodItem2 = DatabaseTestUtils.createTestFoodItem("Burger", BigDecimal.valueOf(12.99), null);

        // Persist entities in correct order (dependencies first)
        persistAndFlush(testCustomer1);
        persistAndFlush(testCustomer2);
        persistAndFlush(testAddress1);
        persistAndFlush(testAddress2);
        persistAndFlush(testTable1);
        persistAndFlush(testTable2);
        persistAndFlush(testCategory1);
        persistAndFlush(testCategory2);
        persistAndFlush(testFoodItem1);
        persistAndFlush(testFoodItem2);
        
        // Now establish the many-to-many relationships after all entities are persisted
        testCategory1.getFoodItems().add(testFoodItem1);
        testFoodItem1.getCategories().add(testCategory1);
        testCategory1.getFoodItems().add(testFoodItem2);
        testFoodItem2.getCategories().add(testCategory1);
        
        // Persist the updated relationships
        persistAndFlush(testCategory1);
        persistAndFlush(testFoodItem1);
        persistAndFlush(testFoodItem2);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve order successfully")
        void shouldSaveAndRetrieveOrder() {
            // Given
            Order order = createTestOrder(testCustomer1, testAddress1, OrderStatus.PLACED);
            
            // When
            Order savedOrder = orderRepository.save(order);
            Order retrievedOrder = orderRepository.findById(savedOrder.getOrderId()).orElse(null);
            
            // Then
            assertThat(retrievedOrder).isNotNull();
            assertThat(retrievedOrder.getOrderId()).isEqualTo(savedOrder.getOrderId());
            assertThat(retrievedOrder.getCustomer().getEmail()).isEqualTo("john.doe@example.com");
            assertThat(retrievedOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
            assertThat(retrievedOrder.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(41.49));
        }

        @Test
        @DisplayName("Should save order with order items and maintain relationships")
        void shouldSaveOrderWithOrderItems() {
            // Given
            Order order = createTestOrderWithItems(testCustomer1, testAddress1, OrderStatus.PLACED);
            
            // When
            Order savedOrder = orderRepository.save(order);
            clear(); // Clear persistence context to ensure a fresh load
            Order retrievedOrder = orderRepository.findById(savedOrder.getOrderId()).orElse(null);
            
            // Then
            assertThat(retrievedOrder).isNotNull();
            assertThat(retrievedOrder.getOrderItems()).isNotNull().hasSize(2);
            
            OrderItem pizzaItem = retrievedOrder.getOrderItems().stream()
                .filter(item -> "Pizza".equals(item.getFoodItem().getFoodName()))
                .findFirst().orElse(null);
            
            assertThat(pizzaItem).isNotNull();
            assertThat(pizzaItem.getQuantity()).isEqualTo(2);
            assertThat(pizzaItem.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(25.99));
            assertThat(pizzaItem.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(51.98));
        }

        @Test
        @DisplayName("Should delete order and cascade to order items")
        void shouldDeleteOrderAndCascadeToOrderItems() {
            // Given
            long initialOrderItemCount = orderItemRepository.count();
            
            Order order = createTestOrderWithItems(testCustomer1, testAddress1, OrderStatus.PLACED);
            // Note: createTestOrderWithItems already saves the order and returns the saved version
            Long orderId = order.getOrderId();
            
            // Verify order items were created
            assertThat(order.getOrderItems()).as("Order should have order items").hasSize(2);
            long countAfterCreation = orderItemRepository.count();
            assertThat(countAfterCreation).as("Order item count should increase after creation").isEqualTo(initialOrderItemCount + 2);
            
            // When
            orderRepository.deleteById(orderId);
            flush(); // Force the deletion to be executed immediately
            clear(); // Clear persistence context
            
            // Then
            assertThat(orderRepository.findById(orderId)).as("Order should be deleted").isEmpty();
            
            // Verify the order items are cascade deleted
            long finalOrderItemCount = orderItemRepository.count();
            assertThat(finalOrderItemCount).as("Order items should be cascade deleted").isEqualTo(initialOrderItemCount);
        }
    }

    @Nested
    @DisplayName("Customer-Based Queries")
    class CustomerBasedQueryTests {

        @Test
        @DisplayName("Should find orders by customer email ordered by time desc")
        void shouldFindOrdersByCustomerEmailOrderedByTimeDesc() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            Order order1 = createTestOrderAtTime(testCustomer1, testAddress1, OrderStatus.PLACED, now.minusHours(2));
            Order order2 = createTestOrderAtTime(testCustomer1, testAddress1, OrderStatus.PREPARING, now.minusHours(1));
            Order order3 = createTestOrderAtTime(testCustomer2, testAddress2, OrderStatus.PLACED, now.minusMinutes(30));
            
            orderRepository.saveAll(Arrays.asList(order1, order2, order3));
            
            // When
            List<Order> customer1Orders = orderRepository.findAllByCustomerEmailOrderByOrderTimeDesc("john.doe@example.com");
            
            // Then
            assertThat(customer1Orders).hasSize(2);
            assertThat(customer1Orders.get(0).getOrderTime()).isAfter(customer1Orders.get(1).getOrderTime());
            assertThat(customer1Orders.get(0).getStatus()).isEqualTo(OrderStatus.PREPARING);
            assertThat(customer1Orders.get(1).getStatus()).isEqualTo(OrderStatus.PLACED);
        }

        @Test
        @DisplayName("Should find first order by customer email ordered by time desc")
        void shouldFindFirstOrderByCustomerEmailOrderedByTimeDesc() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            Order order1 = createTestOrderAtTime(testCustomer1, testAddress1, OrderStatus.PLACED, now.minusHours(2));
            Order order2 = createTestOrderAtTime(testCustomer1, testAddress1, OrderStatus.PREPARING, now.minusHours(1));
            
            orderRepository.saveAll(Arrays.asList(order1, order2));
            
            // When
            Order latestOrder = orderRepository.findFirstByCustomerEmailOrderByOrderTimeDesc("john.doe@example.com");
            
            // Then
            assertThat(latestOrder).isNotNull();
            assertThat(latestOrder.getStatus()).isEqualTo(OrderStatus.PREPARING);
            assertThat(latestOrder.getOrderTime()).isAfter(order1.getOrderTime());
        }

        @Test
        @DisplayName("Should find orders by customer entity")
        void shouldFindOrdersByCustomer() {
            // Given
            Order order1 = createTestOrder(testCustomer1, testAddress1, OrderStatus.PLACED);
            Order order2 = createTestOrder(testCustomer1, testAddress1, OrderStatus.PREPARING);
            Order order3 = createTestOrder(testCustomer2, testAddress2, OrderStatus.PLACED);
            
            orderRepository.saveAll(Arrays.asList(order1, order2, order3));
            
            // When
            List<Order> customer1Orders = orderRepository.findByCustomer(testCustomer1);
            
            // Then
            assertThat(customer1Orders).hasSize(2);
            assertThat(customer1Orders).allMatch(order -> order.getCustomer().getEmail().equals("john.doe@example.com"));
        }

        @Test
        @DisplayName("Should find first order by customer ordered by time asc")
        void shouldFindFirstOrderByCustomerOrderedByTimeAsc() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            Order order1 = createTestOrderAtTime(testCustomer1, testAddress1, OrderStatus.PLACED, now.minusHours(3));
            Order order2 = createTestOrderAtTime(testCustomer1, testAddress1, OrderStatus.PREPARING, now.minusHours(1));
            
            orderRepository.saveAll(Arrays.asList(order1, order2));
            
            // When
            Order firstOrder = orderRepository.findFirstByCustomerOrderByOrderTimeAsc(testCustomer1);
            
            // Then
            assertThat(firstOrder).isNotNull();
            assertThat(firstOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
            assertThat(firstOrder.getOrderTime()).isBefore(order2.getOrderTime());
        }
    }

    @Nested
    @DisplayName("Status-Based Queries")
    class StatusBasedQueryTests {

        @Test
        @DisplayName("Should find all orders by status")
        void shouldFindAllOrdersByStatus() {
            // Given
            Order order1 = createTestOrder(testCustomer1, testAddress1, OrderStatus.PLACED);
            Order order2 = createTestOrder(testCustomer2, testAddress2, OrderStatus.PLACED);
            Order order3 = createTestOrder(testCustomer1, testAddress1, OrderStatus.PREPARING);
            
            orderRepository.saveAll(Arrays.asList(order1, order2, order3));
            
            // When
            List<Order> placedOrders = orderRepository.findAllByStatus(OrderStatus.PLACED);
            
            // Then
            assertThat(placedOrders).hasSize(2);
            assertThat(placedOrders).allMatch(order -> order.getStatus() == OrderStatus.PLACED);
        }

        @Test
        @DisplayName("Should find orders by status with pagination")
        void shouldFindOrdersByStatusWithPagination() {
            // Given
            for (int i = 0; i < 5; i++) {
                Order order = createTestOrder(testCustomer1, testAddress1, OrderStatus.PLACED);
                orderRepository.save(order);
            }
            
            Pageable pageable = PageRequest.of(0, 3);
            
            // When
            Page<Order> orderPage = orderRepository.findAllByStatus(OrderStatus.PLACED, pageable);
            
            // Then
            assertThat(orderPage.getContent()).hasSize(3);
            assertThat(orderPage.getTotalElements()).isEqualTo(5);
            assertThat(orderPage.getTotalPages()).isEqualTo(2);
            assertThat(orderPage.isFirst()).isTrue();
            assertThat(orderPage.hasNext()).isTrue();
        }

        @Test
        @DisplayName("Should find orders by multiple statuses with pagination")
        void shouldFindOrdersByMultipleStatusesWithPagination() {
            // Given
            Order order1 = createTestOrder(testCustomer1, testAddress1, OrderStatus.PLACED);
            Order order2 = createTestOrder(testCustomer2, testAddress2, OrderStatus.PREPARING);
            Order order3 = createTestOrder(testCustomer1, testAddress1, OrderStatus.DELIVERED);
            
            orderRepository.saveAll(Arrays.asList(order1, order2, order3));
            
            Pageable pageable = PageRequest.of(0, 10);
            List<OrderStatus> statuses = Arrays.asList(OrderStatus.PLACED, OrderStatus.PREPARING);
            
            // When
            Page<Order> orderPage = orderRepository.findAllByStatusIn(statuses, pageable);
            
            // Then
            assertThat(orderPage.getContent()).hasSize(2);
            assertThat(orderPage.getContent()).allMatch(order -> 
                order.getStatus() == OrderStatus.PLACED || order.getStatus() == OrderStatus.PREPARING
            );
        }
    }

    @Nested
    @DisplayName("Time-Range Queries")
    class TimeRangeQueryTests {

        @Test
        @DisplayName("Should find orders by status and time range")
        void shouldFindOrdersByStatusAndTimeRange() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = now.minusHours(2);
            LocalDateTime end = now.plusHours(1);
            
            Order order1 = createTestOrderAtTime(testCustomer1, testAddress1, OrderStatus.PLACED, now.minusHours(3)); // Outside range
            Order order2 = createTestOrderAtTime(testCustomer2, testAddress2, OrderStatus.PLACED, now.minusHours(1)); // Inside range
            Order order3 = createTestOrderAtTime(testCustomer1, testAddress1, OrderStatus.PREPARING, now.minusMinutes(30)); // Wrong status
            Order order4 = createTestOrderAtTime(testCustomer2, testAddress2, OrderStatus.PLACED, now.minusMinutes(15)); // Inside range
            
            orderRepository.saveAll(Arrays.asList(order1, order2, order3, order4));
            
            // When
            List<Order> orders = orderRepository.findAllByStatusAndOrderTimeBetween(OrderStatus.PLACED, start, end);
            
            // Then
            assertThat(orders).hasSize(2);
            assertThat(orders).allMatch(order -> 
                order.getStatus() == OrderStatus.PLACED &&
                order.getOrderTime().isAfter(start.minusSeconds(1)) &&
                order.getOrderTime().isBefore(end.plusSeconds(1))
            );
        }
    }

    @Nested
    @DisplayName("Complex Relationship Queries")
    class ComplexRelationshipQueryTests {

        @Test
        @DisplayName("Should find latest order by customer excluding certain statuses")
        void shouldFindLatestOrderByCustomerExcludingStatuses() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            Order order1 = createTestOrderAtTime(testCustomer1, testAddress1, OrderStatus.CANCELLED, now.minusHours(1));
            Order order2 = createTestOrderAtTime(testCustomer1, testAddress1, OrderStatus.PLACED, now.minusHours(2));
            Order order3 = createTestOrderAtTime(testCustomer1, testAddress1, OrderStatus.DELIVERED, now.minusMinutes(30));
            
            orderRepository.saveAll(Arrays.asList(order1, order2, order3));
            
            List<OrderStatus> excludedStatuses = Arrays.asList(OrderStatus.CANCELLED, OrderStatus.DELIVERED);
            
            // When
            Order latestActiveOrder = orderRepository.findTopByCustomerAndStatusNotInOrderByOrderTimeDesc(
                testCustomer1, excludedStatuses);
            
            // Then
            assertThat(latestActiveOrder).isNotNull();
            assertThat(latestActiveOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
            assertThat(excludedStatuses).doesNotContain(latestActiveOrder.getStatus());
        }

        @Test
        @DisplayName("Should find orders by customer, table, status and time range")
        void shouldFindOrdersByCustomerTableStatusAndTimeRange() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = now.minusHours(2);
            LocalDateTime end = now.plusHours(1);
            
            Order order1 = createTestOrderAtTimeWithTable(testCustomer1, testAddress1, testTable1, 
                OrderStatus.PLACED, now.minusHours(1));
            Order order2 = createTestOrderAtTimeWithTable(testCustomer1, testAddress1, testTable2, 
                OrderStatus.PREPARING, now.minusMinutes(30)); // Different table
            Order order3 = createTestOrderAtTimeWithTable(testCustomer2, testAddress2, testTable1, 
                OrderStatus.DELIVERED, now.minusMinutes(45)); // Different customer
            
            orderRepository.saveAll(Arrays.asList(order1, order2, order3));
            
            // When
            List<Order> orders = orderRepository.findAllByCustomerAndTableAndStatusAndOrderTimeBetween(
                testCustomer1, testTable1, OrderStatus.PLACED, start, end);
            
            // Then
            assertThat(orders).hasSize(1);
            Order foundOrder = orders.get(0);
            assertThat(foundOrder.getCustomer()).isEqualTo(testCustomer1);
            assertThat(foundOrder.getTable()).isEqualTo(testTable1);
            assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
        }
    }

    @Nested
    @DisplayName("Custom Update Queries")
    class CustomUpdateQueryTests {

        @Test
        @DisplayName("Should update customer and address to null")
        void shouldUpdateCustomerAndAddressToNull() {
            // Given
            Order order1 = createTestOrder(testCustomer1, testAddress1, OrderStatus.DELIVERED);
            Order order2 = createTestOrder(testCustomer1, testAddress1, OrderStatus.COMPLETED);
            Order order3 = createTestOrder(testCustomer2, testAddress2, OrderStatus.PLACED);
            
            orderRepository.saveAll(Arrays.asList(order1, order2, order3));
            flush();
            
            // When
            orderRepository.updateCustomerAndAddressToNull(testCustomer1);
            flush();
            clear();
            
            // Then
            Order updatedOrder1 = orderRepository.findById(order1.getOrderId()).orElse(null);
            Order updatedOrder2 = orderRepository.findById(order2.getOrderId()).orElse(null);
            Order unchangedOrder3 = orderRepository.findById(order3.getOrderId()).orElse(null);
            
            assertThat(updatedOrder1).isNotNull();
            assertThat(updatedOrder1.getCustomer()).isNull();
            assertThat(updatedOrder1.getAddress()).isNull();
            assertThat(updatedOrder2).isNotNull();
            assertThat(updatedOrder2.getCustomer()).isNull();
            assertThat(updatedOrder2.getAddress()).isNull();
            
            assertThat(unchangedOrder3).isNotNull();
            assertThat(unchangedOrder3.getCustomer()).isEqualTo(testCustomer2);
            assertThat(unchangedOrder3.getAddress()).isEqualTo(testAddress2);
        }
    }

    @Nested
    @DisplayName("Data Integrity and Edge Cases")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should handle orders with null optional fields")
        void shouldHandleOrdersWithNullOptionalFields() {
            // Given
            Order order = new Order();
            order.setOrderTime(LocalDateTime.now());
            order.setStatus(OrderStatus.PLACED);
            order.setTotalPrice(BigDecimal.valueOf(25.99));
            order.setCustomer(testCustomer1);
            // No address, no table, no notes
            
            // When
            Order savedOrder = orderRepository.save(order);
            Order retrievedOrder = orderRepository.findById(savedOrder.getOrderId()).orElse(null);
            
            // Then
            assertThat(retrievedOrder).isNotNull();
            assertThat(retrievedOrder.getAddress()).isNull();
            assertThat(retrievedOrder.getTable()).isNull();
            assertThat(retrievedOrder.getNotes()).isNull();
            assertThat(retrievedOrder.getCustomer()).isNotNull();
        }

        @Test
        @DisplayName("Should handle empty result sets gracefully")
        void shouldHandleEmptyResultSetsGracefully() {
            // When - querying for non-existent data
            List<Order> orders = orderRepository.findAllByCustomerEmailOrderByOrderTimeDesc("nonexistent@example.com");
            Order firstOrder = orderRepository.findFirstByCustomerEmailOrderByOrderTimeDesc("nonexistent@example.com");
            List<Order> statusOrders = orderRepository.findAllByStatus(OrderStatus.DELIVERED);
            
            // Then
            assertThat(orders).isEmpty();
            assertThat(firstOrder).isNull();
            assertThat(statusOrders).isEmpty();
        }

        @Test
        @DisplayName("Should maintain referential integrity with cascading operations")
        void shouldMaintainReferentialIntegrityWithCascading() {
            // Given
            Order order = createTestOrderWithItems(testCustomer1, testAddress1, OrderStatus.PLACED);
            Order savedOrder = orderRepository.save(order);
            Long orderId = savedOrder.getOrderId();
            
            // Verify initial state
            assertThat(savedOrder.getOrderItems()).hasSize(2);
            
            // When - modify order items through proper orphan removal
            // First, get a fresh copy from the database to ensure proper Hibernate management
            flush();
            clear();
            Order managedOrder = orderRepository.findById(orderId).orElse(null);
            assertThat(managedOrder).isNotNull();
            
            // Find and remove the Pizza item using iterator (safe for Hibernate collections)
            Iterator<OrderItem> iterator = managedOrder.getOrderItems().iterator();
            while (iterator.hasNext()) {
                OrderItem item = iterator.next();
                if ("Pizza".equals(item.getFoodItem().getFoodName())) {
                    iterator.remove(); // This should trigger orphan removal
                    break;
                }
            }
            
            orderRepository.save(managedOrder);
            flush();
            clear();
            
            // Then
            Order updatedOrder = orderRepository.findById(orderId).orElse(null);
            assertThat(updatedOrder).isNotNull();
            assertThat(updatedOrder.getOrderItems()).isNotNull().hasSize(1);
            assertThat(updatedOrder.getOrderItems().get(0).getFoodItem().getFoodName()).isEqualTo("Burger");
        }
    }

    // === Helper Methods ===

    private Order createTestOrder(User customer, Address address, OrderStatus status) {
        Order order = new Order();
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(status);
        order.setTotalPrice(BigDecimal.valueOf(41.49));
        order.setNotes("Test order notes");
        order.setCustomer(customer);
        order.setAddress(address);
        return order;
    }

    private Order createTestOrderAtTime(User customer, Address address, OrderStatus status, LocalDateTime orderTime) {
        Order order = createTestOrder(customer, address, status);
        order.setOrderTime(orderTime);
        return order;
    }

    private Order createTestOrderAtTimeWithTable(User customer, Address address, TableTop table, 
                                                 OrderStatus status, LocalDateTime orderTime) {
        Order order = createTestOrderAtTime(customer, address, status, orderTime);
        order.setTable(table);
        return order;
    }

    private Order createTestOrderWithItems(User customer, Address address, OrderStatus status) {
        // Create the order without items first
        Order order = createTestOrder(customer, address, status);
        
        // Save the order first to get an ID
        order = orderRepository.save(order);
        
        // Create order items and associate them with the saved order
        OrderItem pizzaItem = new OrderItem();
        pizzaItem.setFoodItem(testFoodItem1);
        pizzaItem.setQuantity(2);
        pizzaItem.setUnitPrice(testFoodItem1.getPrice());
        pizzaItem.setTotalPrice(testFoodItem1.getPrice().multiply(BigDecimal.valueOf(2)));
        pizzaItem.setNote("Extra cheese");
        pizzaItem.setOrder(order);
        
        OrderItem burgerItem = new OrderItem();
        burgerItem.setFoodItem(testFoodItem2);
        burgerItem.setQuantity(1);
        burgerItem.setUnitPrice(testFoodItem2.getPrice());
        burgerItem.setTotalPrice(testFoodItem2.getPrice());
        burgerItem.setNote("No pickles");
        burgerItem.setOrder(order);
        
        // Save the order items using orderItemRepository
        pizzaItem = orderItemRepository.save(pizzaItem);
        burgerItem = orderItemRepository.save(burgerItem);
        
        // Update the order with the saved items using a mutable ArrayList
        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(pizzaItem);
        orderItems.add(burgerItem);
        order.setOrderItems(orderItems);
        
        // Update total price
        BigDecimal totalPrice = pizzaItem.getTotalPrice().add(burgerItem.getTotalPrice());
        order.setTotalPrice(totalPrice);
        
        // Save the updated order
        return orderRepository.save(order);
    }
}
