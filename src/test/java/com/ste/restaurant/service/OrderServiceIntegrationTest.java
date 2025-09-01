package com.ste.restaurant.service;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.dto.common.WarningResponse;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.entity.enums.OrderStatus;
import com.ste.restaurant.entity.enums.TableStatus;
import com.ste.restaurant.entity.enums.UserRole;
import com.ste.restaurant.exception.*;
import com.ste.restaurant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for OrderService.
 * Tests complex order management operations, order lifecycle,
 * customer ordering process, and staff order handling
 * with real database operations.
 * <p>
 * This verifies:
 * - Order placement and validation
 * - Order status management and transitions
 * - Order item management and calculations
 * - Table and address management for orders
 * - Customer order operations
 * - Staff order management functionality
 * - Order cancellation and modification
 * - Complex business logic like order merging
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("OrderService Integration Tests")
public class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderRepository orderRepository;
    

    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private MenuRepository menuRepository;
    
    @Autowired
    private TableTopRepository tableTopRepository;
    
    @Autowired
    private AddressRepository addressRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testCustomer;
    private User testWaiter;
    private User testChef;
    private User testAdmin;
    private FoodItem testFood1;
    private FoodItem testFood2;
    private FoodItem testFood3;
    private TableTop testTable;
    private Address testAddress;
    private Order testOrder;
    private String timestamp;

    @BeforeEach
    void setUp() {
        timestamp = String.valueOf(System.currentTimeMillis());
        
        // Create test data
        setupTestUsers();
        setupTestFoodItems();
        setupTestTable();
        setupTestAddress();
        setupTestMenu();
        setupTestOrder();
    }

    @Nested
    @DisplayName("Order Placement Integration Tests")
    class OrderPlacementIntegrationTests {

        @Test
        @DisplayName("Should place order successfully for table dining")
        void shouldPlaceOrderSuccessfullyForTableDining() {
            // Given
            PlaceOrderDto placeOrderDto = createTestPlaceOrderDto();
            placeOrderDto.setTableNumber(testTable.getTableNumber());
            placeOrderDto.setAddressId(null);
            
            // When
            WarningResponse<OrderDto> response = orderService.placeOrder(placeOrderDto, testCustomer.getEmail());
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getData()).isNotNull();
            assertThat(response.getWarnings()).isEmpty();
            
            OrderDto orderDto = response.getData();
            assertThat(orderDto.getStatus()).isEqualTo("PLACED");
            assertThat(orderDto.getTotalPrice()).isEqualTo(
                testFood1.getPrice().multiply(BigDecimal.valueOf(2))
                .add(testFood2.getPrice().multiply(BigDecimal.valueOf(1)))
            );
            assertThat(orderDto.getTable().getTableNumber()).isEqualTo(testTable.getTableNumber());
            assertThat(orderDto.getCustomer()).isNull(); // Should be null for security
            
            // Verify table status is updated
            TableTop updatedTable = tableTopRepository.findById(testTable.getTableId()).orElseThrow();
            assertThat(updatedTable.getTableStatus()).isEqualTo(TableStatus.OCCUPIED);
        }

        @Test
        @DisplayName("Should place order successfully for delivery")
        void shouldPlaceOrderSuccessfullyForDelivery() {
            // Given
            PlaceOrderDto placeOrderDto = createTestPlaceOrderDto();
            placeOrderDto.setAddressId(testAddress.getAddressId());
            placeOrderDto.setTableNumber(null);
            
            // When
            WarningResponse<OrderDto> response = orderService.placeOrder(placeOrderDto, testCustomer.getEmail());
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getData()).isNotNull();
            
            OrderDto orderDto = response.getData();
            assertThat(orderDto.getStatus()).isEqualTo("PLACED");
            assertThat(orderDto.getAddress().getAddressId()).isEqualTo(testAddress.getAddressId());
            assertThat(orderDto.getTable()).isNull();
        }

        @Test
        @DisplayName("Should place order with warnings for invalid food items")
        void shouldPlaceOrderWithWarningsForInvalidFoodItems() {
            // Given
            PlaceOrderDto placeOrderDto = new PlaceOrderDto();
            placeOrderDto.setTableNumber(testTable.getTableNumber());
            placeOrderDto.setNotes("Test order with invalid items");
            
            OrderItemDtoBasic validItem = new OrderItemDtoBasic(2, "Good item", testFood1.getFoodName());
            OrderItemDtoBasic invalidItem = new OrderItemDtoBasic(1, "Bad item", "NonExistentFood");
            placeOrderDto.setOrderItems(new ArrayList<>(Arrays.asList(validItem, invalidItem)));
            
            // When
            WarningResponse<OrderDto> response = orderService.placeOrder(placeOrderDto, testCustomer.getEmail());
            
            // Then
            assertThat(response.getData()).isNotNull();
            assertThat(response.getWarnings()).hasSize(1);
            assertThat(response.getWarnings()).contains("NonExistentFood");
            
            // Verify only valid item was processed
            OrderDto orderDto = response.getData();
            assertThat(orderDto.getOrderItems()).hasSize(1);
            assertThat(orderDto.getTotalPrice()).isEqualTo(testFood1.getPrice().multiply(BigDecimal.valueOf(2)));
        }

        @Test
        @DisplayName("Should throw exception when no location is provided")
        void shouldThrowExceptionWhenNoLocationIsProvided() {
            // Given
            PlaceOrderDto placeOrderDto = createTestPlaceOrderDto();
            placeOrderDto.setTableNumber(null);
            placeOrderDto.setAddressId(null);
            
            // When & Then
            assertThatThrownBy(() -> orderService.placeOrder(placeOrderDto, testCustomer.getEmail()))
                    .isInstanceOf(InvalidValueException.class)
                    .hasMessageContaining("Either table number or address must be provided");
        }

        @Test
        @DisplayName("Should throw exception when no valid order items exist")
        void shouldThrowExceptionWhenNoValidOrderItemsExist() {
            // Given
            PlaceOrderDto placeOrderDto = new PlaceOrderDto();
            placeOrderDto.setTableNumber(testTable.getTableNumber());
            
            OrderItemDtoBasic invalidItem = new OrderItemDtoBasic(1, "Invalid", "NonExistentFood");
            placeOrderDto.setOrderItems(new ArrayList<>(Collections.singletonList(invalidItem)));
            
            // When & Then
            assertThatThrownBy(() -> orderService.placeOrder(placeOrderDto, testCustomer.getEmail()))
                    .isInstanceOf(InvalidValueException.class)
                    .hasMessageContaining("No valid order items found");
        }

        @Test
        @DisplayName("Should throw exception for non-existent table")
        void shouldThrowExceptionForNonExistentTable() {
            // Given
            PlaceOrderDto placeOrderDto = createTestPlaceOrderDto();
            placeOrderDto.setTableNumber("NonExistentTable");
            placeOrderDto.setAddressId(null);
            
            // When & Then
            assertThatThrownBy(() -> orderService.placeOrder(placeOrderDto, testCustomer.getEmail()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Table not found");
        }

        @Test
        @DisplayName("Should throw exception for non-existent address")
        void shouldThrowExceptionForNonExistentAddress() {
            // Given
            PlaceOrderDto placeOrderDto = createTestPlaceOrderDto();
            placeOrderDto.setAddressId(99999L);
            placeOrderDto.setTableNumber(null);
            
            // When & Then
            assertThatThrownBy(() -> orderService.placeOrder(placeOrderDto, testCustomer.getEmail()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Address not found");
        }
    }

    @Nested
    @DisplayName("Order Retrieval Integration Tests") 
    class OrderRetrievalIntegrationTests {

        @Test
        @DisplayName("Should retrieve all orders with pagination")
        void shouldRetrieveAllOrdersWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            
            // When
            Page<OrderDto> orderPage = orderService.getOrderList(pageable);
            
            // Then
            assertThat(orderPage).isNotNull();
            assertThat(orderPage.getContent()).isNotEmpty();
            assertThat(orderPage.getContent().size()).isLessThanOrEqualTo(10);
            
            OrderDto firstOrder = orderPage.getContent().get(0);
            assertThat(firstOrder.getOrderId()).isNotNull();
            assertThat(firstOrder.getStatus()).isNotNull();
        }

        @Test
        @DisplayName("Should retrieve orders by status with filtering")
        void shouldRetrieveOrdersByStatusWithFiltering() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            
            // When
            Page<OrderDto> placedOrders = orderService.getAllOrdersBy("PLACED", pageable);
            Page<OrderDto> completedOrders = orderService.getAllOrdersBy("completed", pageable);
            
            // Then
            assertThat(placedOrders.getContent()).allMatch(order -> "PLACED".equals(order.getStatus()));
            assertThat(completedOrders.getContent()).allMatch(order -> "COMPLETED".equals(order.getStatus()));
        }

        @Test
        @DisplayName("Should throw exception for invalid order status")
        void shouldThrowExceptionForInvalidOrderStatus() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            
            // When & Then
            assertThatThrownBy(() -> orderService.getAllOrdersBy("INVALID_STATUS", pageable))
                    .isInstanceOf(InvalidValueException.class)
                    .hasMessageContaining("INVALID_STATUS");
        }

//        @Test
//        @DisplayName("Should retrieve orders for chef with proper status filtering")
//        void shouldRetrieveOrdersForChefWithProperStatusFiltering() {
//            // Given
//            Pageable pageable = PageRequest.of(0, 10);
//
//            // When
//            Page<OrderDto> chefOrders = orderService.getAllOrdersForChef(pageable);
//
//            // Then
//            assertThat(chefOrders.getContent()).allMatch(order ->
//                Arrays.asList("PLACED", "PREPARING", "READY").contains(order.getStatus())
//            );
//        }

        @Test
        @DisplayName("Should retrieve order by ID with complete data")
        void shouldRetrieveOrderByIdWithCompleteData() {
            // When
            OrderDto retrievedOrder = orderService.getOrderById(testOrder.getOrderId());
            
            // Then
            assertThat(retrievedOrder).isNotNull();
            assertThat(retrievedOrder.getOrderId()).isEqualTo(testOrder.getOrderId());
            assertThat(retrievedOrder.getStatus()).isEqualTo(testOrder.getStatus().name());
            assertThat(retrievedOrder.getTotalPrice()).isEqualTo(testOrder.getTotalPrice());
            assertThat(retrievedOrder.getOrderItems()).isNotEmpty();
        }

        @Test
        @DisplayName("Should retrieve order by ID for specific customer")
        void shouldRetrieveOrderByIdForSpecificCustomer() {
            // When
            OrderDto retrievedOrder = orderService.getOrderById(testOrder.getOrderId(), testCustomer.getEmail());
            
            // Then
            assertThat(retrievedOrder).isNotNull();
            assertThat(retrievedOrder.getOrderId()).isEqualTo(testOrder.getOrderId());
        }

        @Test
        @DisplayName("Should throw exception when customer tries to access other's order")
        void shouldThrowExceptionWhenCustomerTriesToAccessOthersOrder() {
            // Given
            User anotherCustomer = createTestUser("another" + timestamp + "@test.com", "Another", "Customer", UserRole.CUSTOMER);
            userRepository.save(anotherCustomer);
            
            // When & Then
            assertThatThrownBy(() -> orderService.getOrderById(testOrder.getOrderId(), anotherCustomer.getEmail()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("Should throw exception for non-existent order")
        void shouldThrowExceptionForNonExistentOrder() {
            // Given
            Long nonExistentId = 99999L;
            
            // When & Then
            assertThatThrownBy(() -> orderService.getOrderById(nonExistentId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Order not found");
        }
    }

    @Nested
    @DisplayName("Order Status Management Integration Tests")
    class OrderStatusManagementIntegrationTests {

        @Test
        @DisplayName("Should update order status successfully")
        void shouldUpdateOrderStatusSuccessfully() {
            // Given
            StringDto statusDto = new StringDto();
            statusDto.setName("PREPARING");
            
            // When
            OrderDto updatedOrder = orderService.updateOrderStatus(testOrder.getOrderId(), statusDto);
            
            // Then
            assertThat(updatedOrder.getStatus()).isEqualTo("PREPARING");
            
            // Verify in database
            Order dbOrder = orderRepository.findById(testOrder.getOrderId()).orElseThrow();
            assertThat(dbOrder.getStatus()).isEqualTo(OrderStatus.PREPARING);
        }

        @Test
        @DisplayName("Should throw exception when setting same status")
        void shouldThrowExceptionWhenSettingSameStatus() {
            // Given
            StringDto statusDto = new StringDto();
            statusDto.setName("PLACED"); // Same as current status
            
            // When & Then
            assertThatThrownBy(() -> orderService.updateOrderStatus(testOrder.getOrderId(), statusDto))
                    .isInstanceOf(AlreadyHasException.class)
                    .hasMessageContaining("PLACED");
        }

        @Test
        @DisplayName("Should throw exception for invalid status")
        void shouldThrowExceptionForInvalidStatus() {
            // Given
            StringDto statusDto = new StringDto();
            statusDto.setName("INVALID_STATUS");
            
            // When & Then
            assertThatThrownBy(() -> orderService.updateOrderStatus(testOrder.getOrderId(), statusDto))
                    .isInstanceOf(InvalidValueException.class)
                    .hasMessageContaining("INVALID_STATUS");
        }

        @Test
        @DisplayName("Should trigger order merging when status is set to DELIVERED")
        void shouldTriggerOrderMergingWhenStatusIsSetToDelivered() {
            // Given - Create additional order for the same customer and table
            Order additionalOrder = createTestOrderForTableAndCustomer(testCustomer, testTable);
            additionalOrder.setStatus(OrderStatus.DELIVERED);
            additionalOrder.setOrderTime(LocalDateTime.now().minusMinutes(30)); // 30 minutes ago
            orderRepository.save(additionalOrder);
            
            StringDto statusDto = new StringDto();
            statusDto.setName("DELIVERED");
            
            BigDecimal expectedTotal = testOrder.getTotalPrice().add(additionalOrder.getTotalPrice());
            
            // When
            OrderDto updatedOrder = orderService.updateOrderStatus(testOrder.getOrderId(), statusDto);
            
            // Then
            assertThat(updatedOrder.getStatus()).isEqualTo("DELIVERED");
            
            // Verify order merging occurred
            Order dbOrder = orderRepository.findById(testOrder.getOrderId()).orElseThrow();
            assertThat(dbOrder.getTotalPrice()).isEqualTo(expectedTotal);
            
            // Verify the merged order was deleted
            Optional<Order> mergedOrder = orderRepository.findById(additionalOrder.getOrderId());
            assertThat(mergedOrder).isEmpty();
        }
    }

    @Nested
    @DisplayName("Order Cancellation Integration Tests")
    class OrderCancellationIntegrationTests {

        @Test
        @DisplayName("Should allow customer to cancel own order when PLACED")
        void shouldAllowCustomerToCancelOwnOrderWhenPlaced() {
            // Given
            testOrder.setStatus(OrderStatus.PLACED);
            orderRepository.save(testOrder);
            
            // When
            OrderDto cancelledOrder = orderService.cancelOrderIfNotReady(testOrder.getOrderId(), testCustomer.getEmail());
            
            // Then
            assertThat(cancelledOrder.getStatus()).isEqualTo("CANCELLED");
            
            Order dbOrder = orderRepository.findById(testOrder.getOrderId()).orElseThrow();
            assertThat(dbOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should allow customer to cancel own order when PREPARING")
        void shouldAllowCustomerToCancelOwnOrderWhenPreparing() {
            // Given
            testOrder.setStatus(OrderStatus.PREPARING);
            orderRepository.save(testOrder);
            
            // When
            OrderDto cancelledOrder = orderService.cancelOrderIfNotReady(testOrder.getOrderId(), testCustomer.getEmail());
            
            // Then
            assertThat(cancelledOrder.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("Should not allow customer to cancel order when READY")
        void shouldNotAllowCustomerToCancelOrderWhenReady() {
            // Given
            testOrder.setStatus(OrderStatus.READY);
            orderRepository.save(testOrder);
            
            // When & Then
            assertThatThrownBy(() -> orderService.cancelOrderIfNotReady(testOrder.getOrderId(), testCustomer.getEmail()))
                    .isInstanceOf(InvalidValueException.class)
                    .hasMessageContaining("Cannot cancel this order is already in the way");
        }

        @Test
        @DisplayName("Should allow waiter to cancel any order")
        void shouldAllowWaiterToCancelAnyOrder() {
            // Given
            testOrder.setStatus(OrderStatus.READY);
            orderRepository.save(testOrder);
            
            // When
            OrderDto cancelledOrder = orderService.cancelOrderIfNotReady(testOrder.getOrderId(), testWaiter.getEmail());
            
            // Then
            assertThat(cancelledOrder.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("Should allow admin to cancel any order")
        void shouldAllowAdminToCancelAnyOrder() {
            // Given
            testOrder.setStatus(OrderStatus.SHIPPED);
            orderRepository.save(testOrder);
            
            // When
            OrderDto cancelledOrder = orderService.cancelOrderIfNotReady(testOrder.getOrderId(), testAdmin.getEmail());
            
            // Then
            assertThat(cancelledOrder.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("Should not allow customer to cancel other customer's order")
        void shouldNotAllowCustomerToCancelOtherCustomersOrder() {
            // Given
            User anotherCustomer = createTestUser("another" + timestamp + "@test.com", "Another", "Customer", UserRole.CUSTOMER);
            userRepository.save(anotherCustomer);
            
            // When & Then
            assertThatThrownBy(() -> orderService.cancelOrderIfNotReady(testOrder.getOrderId(), anotherCustomer.getEmail()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Order not found");
        }
    }

    @Nested
    @DisplayName("Order Item Management Integration Tests")
    class OrderItemManagementIntegrationTests {

        @Test
        @DisplayName("Should retrieve order items from order")
        void shouldRetrieveOrderItemsFromOrder() {
            // When
            List<OrderItemDto> orderItems = orderService.getOrderItemsFromOrder(testOrder.getOrderId());
            
            // Then
            assertThat(orderItems).isNotEmpty();
            assertThat(orderItems).hasSize(testOrder.getOrderItems().size());
            
            OrderItemDto firstItem = orderItems.get(0);
            assertThat(firstItem.getOrderItemId()).isNotNull();
            assertThat(firstItem.getFoodItem()).isNotNull();
            assertThat(firstItem.getQuantity()).isPositive();
            assertThat(firstItem.getUnitPrice()).isPositive();
            assertThat(firstItem.getTotalPrice()).isPositive();
        }

        @Test
        @DisplayName("Should retrieve order items for specific user")
        void shouldRetrieveOrderItemsForSpecificUser() {
            // When
            List<OrderItemDto> orderItems = orderService.getOrderItemsForUser(testOrder.getOrderId(), testCustomer.getEmail());
            
            // Then
            assertThat(orderItems).isNotEmpty();
            assertThat(orderItems).hasSize(testOrder.getOrderItems().size());
        }

        @Test
        @DisplayName("Should not allow user to access other's order items")
        void shouldNotAllowUserToAccessOthersOrderItems() {
            // Given
            User anotherCustomer = createTestUser("another" + timestamp + "@test.com", "Another", "Customer", UserRole.CUSTOMER);
            userRepository.save(anotherCustomer);
            
            // When & Then
            assertThatThrownBy(() -> orderService.getOrderItemsForUser(testOrder.getOrderId(), anotherCustomer.getEmail()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("Should retrieve all order items with pagination")
        void shouldRetrieveAllOrderItemsWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            
            // When
            Page<OrderItemDto> orderItemPage = orderService.getOrderItemList(pageable);
            
            // Then
            assertThat(orderItemPage).isNotNull();
            assertThat(orderItemPage.getContent()).isNotEmpty();
        }

        @Test
        @DisplayName("Should throw exception for non-existent order when retrieving items")
        void shouldThrowExceptionForNonExistentOrderWhenRetrievingItems() {
            // Given
            Long nonExistentId = 99999L;
            
            // When & Then
            assertThatThrownBy(() -> orderService.getOrderItemsFromOrder(nonExistentId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Order not found");
        }
    }

    @Nested
    @DisplayName("Customer Order Operations Integration Tests")
    class CustomerOrderOperationsIntegrationTests {

        @Test
        @DisplayName("Should retrieve customer's last order")
        void shouldRetrieveCustomersLastOrder() {
            // When
            OrderDto lastOrder = orderService.getLastOrder(testCustomer.getEmail());
            
            // Then
            assertThat(lastOrder).isNotNull();
            assertThat(lastOrder.getCustomer()).isNull(); // Should be null for security
        }

        @Test
        @DisplayName("Should retrieve all customer orders ordered by time")
        void shouldRetrieveAllCustomerOrdersOrderedByTime() {
            // Given - Create another order for the same customer
            Order anotherOrder = createTestOrderForTableAndCustomer(testCustomer, testTable);
            anotherOrder.setOrderTime(LocalDateTime.now().plusMinutes(10));
            orderRepository.save(anotherOrder);
            
            // When
            List<OrderDto> customerOrders = orderService.getOrders(testCustomer.getEmail());
            
            // Then
            assertThat(customerOrders).hasSize(2);
            assertThat(customerOrders.get(0).getOrderTime()).isAfter(customerOrders.get(1).getOrderTime());
        }

        @Test
        @DisplayName("Should throw exception when customer has no orders")
        void shouldThrowExceptionWhenCustomerHasNoOrders() {
            // Given
            User customerWithoutOrders = createTestUser("no-orders" + timestamp + "@test.com", "No", "Orders", UserRole.CUSTOMER);
            userRepository.save(customerWithoutOrders);
            
            // When & Then
            assertThatThrownBy(() -> orderService.getLastOrder(customerWithoutOrders.getEmail()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("Should throw exception for non-existent customer")
        void shouldThrowExceptionForNonExistentCustomer() {
            // Given
            String nonExistentEmail = "nonexistent" + timestamp + "@test.com";
            
            // When & Then
            assertThatThrownBy(() -> orderService.getLastOrder(nonExistentEmail))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("Table Management Integration Tests")
    class TableManagementIntegrationTests {

        @Test
        @DisplayName("Should change table of order successfully")
        void shouldChangeTableOfOrderSuccessfully() {
            // Given - Create another available table
            TableTop newTable = new TableTop();
            newTable.setTableNumber("NEW" + timestamp);
            newTable.setCapacity(4);
            newTable.setTableStatus(TableStatus.AVAILABLE);
            tableTopRepository.save(newTable);
            
            StringDto tableNumberDto = new StringDto();
            tableNumberDto.setName(newTable.getTableNumber());
            
            // When
            OrderDto updatedOrder = orderService.changeTableOfOrder(testOrder.getOrderId(), tableNumberDto);
            
            // Then
            assertThat(updatedOrder.getTable().getTableNumber()).isEqualTo(newTable.getTableNumber());
            
            // Verify old table is now available
            TableTop oldTable = tableTopRepository.findById(testTable.getTableId()).orElseThrow();
            assertThat(oldTable.getTableStatus()).isEqualTo(TableStatus.AVAILABLE);
            
            // Verify new table is now occupied
            TableTop updatedNewTable = tableTopRepository.findById(newTable.getTableId()).orElseThrow();
            assertThat(updatedNewTable.getTableStatus()).isEqualTo(TableStatus.OCCUPIED);
        }

        @Test
        @DisplayName("Should throw exception when changing to same table")
        void shouldThrowExceptionWhenChangingToSameTable() {
            // Given
            StringDto tableNumberDto = new StringDto();
            tableNumberDto.setName(testTable.getTableNumber());
            
            // When & Then
            assertThatThrownBy(() -> orderService.changeTableOfOrder(testOrder.getOrderId(), tableNumberDto))
                    .isInstanceOf(AlreadyHasException.class)
                    .hasMessageContaining(testTable.getTableNumber());
        }

        @Test
        @DisplayName("Should throw exception when order has no table")
        void shouldThrowExceptionWhenOrderHasNoTable() {
            // Given - Create order for delivery (no table)
            Order deliveryOrder = createTestOrderForAddressAndCustomer(testCustomer, testAddress);
            orderRepository.save(deliveryOrder);
            
            StringDto tableNumberDto = new StringDto();
            tableNumberDto.setName(testTable.getTableNumber());
            
            // When & Then
            assertThatThrownBy(() -> orderService.changeTableOfOrder(deliveryOrder.getOrderId(), tableNumberDto))
                    .isInstanceOf(NullValueException.class)
                    .hasMessageContaining("table");
        }

        @Test
        @DisplayName("Should throw exception for non-existent table")
        void shouldThrowExceptionForNonExistentTable() {
            // Given
            StringDto tableNumberDto = new StringDto();
            tableNumberDto.setName("NonExistentTable");
            
            // When & Then
            assertThatThrownBy(() -> orderService.changeTableOfOrder(testOrder.getOrderId(), tableNumberDto))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Table not found");
        }
    }

    @Nested
    @DisplayName("Order Deletion Integration Tests")
    class OrderDeletionIntegrationTests {

        @Test
        @DisplayName("Should delete order successfully")
        void shouldDeleteOrderSuccessfully() {
            // Given
            Long orderId = testOrder.getOrderId();
            
            // When
            OrderDto deletedOrder = orderService.deleteOrderById(orderId);
            
            // Then
            assertThat(deletedOrder).isNotNull();
            assertThat(deletedOrder.getOrderId()).isEqualTo(orderId);
            
            // Verify order is deleted from the database
            Optional<Order> deletedOrderCheck = orderRepository.findById(orderId);
            assertThat(deletedOrderCheck).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent order")
        void shouldThrowExceptionWhenDeletingNonExistentOrder() {
            // Given
            Long nonExistentId = 99999L;
            
            // When & Then
            assertThatThrownBy(() -> orderService.deleteOrderById(nonExistentId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Order not found");
        }
    }

    // Helper methods for test data creation
    private void setupTestUsers() {
        testCustomer = createTestUser("customer" + timestamp + "@test.com", "Test", "Customer", UserRole.CUSTOMER);
        userRepository.save(testCustomer);
        
        testWaiter = createTestUser("waiter" + timestamp + "@test.com", "Test", "Waiter", UserRole.WAITER);
        userRepository.save(testWaiter);
        
        testChef = createTestUser("chef" + timestamp + "@test.com", "Test", "Chef", UserRole.CHEF);
        userRepository.save(testChef);
        
        testAdmin = createTestUser("admin" + timestamp + "@test.com", "Test", "Admin", UserRole.ADMIN);
        userRepository.save(testAdmin);
    }

    private void setupTestFoodItems() {
        testFood1 = createTestFoodItem("Pizza" + timestamp, "Delicious pizza", BigDecimal.valueOf(15.99));
        foodItemRepository.save(testFood1);
        
        testFood2 = createTestFoodItem("Burger" + timestamp, "Tasty burger", BigDecimal.valueOf(12.50));
        foodItemRepository.save(testFood2);
        
        testFood3 = createTestFoodItem("Salad" + timestamp, "Fresh salad", BigDecimal.valueOf(8.75));
        foodItemRepository.save(testFood3);
    }

    private void setupTestTable() {
        testTable = new TableTop();
        testTable.setTableNumber("T" + timestamp);
        testTable.setCapacity(4);
        testTable.setTableStatus(TableStatus.AVAILABLE);
        tableTopRepository.save(testTable);
    }

    private void setupTestAddress() {
        testAddress = new Address();
        testAddress.setName("Test Address " + timestamp);
        testAddress.setCountry("Turkey");
        testAddress.setCity("Istanbul");
        testAddress.setDistrict("Test District");
        testAddress.setStreet("Test Street");
        addressRepository.save(testAddress);

        // Add address to customer using ArrayList instead of Arrays.asList
        testCustomer.setAddresses(new ArrayList<>(Collections.singletonList(testAddress)));
        userRepository.save(testCustomer);
    }

    private void setupTestOrder() {
        testOrder = createTestOrderForTableAndCustomer(testCustomer, testTable);
        orderRepository.save(testOrder);
    }

    private void setupTestMenu() {
        Menu menu = new Menu();
        menu.setMenuName("Test Menu " + timestamp);
        menu.setActive(true);
        menu.setFoodItems(new HashSet<>(Arrays.asList(testFood1, testFood2, testFood3)));
        menuRepository.save(menu);
    }

    private User createTestUser(String email, String firstName, String lastName, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode("TestPass123"));
        return user;
    }

    private FoodItem createTestFoodItem(String name, String description, BigDecimal price) {
        FoodItem foodItem = new FoodItem();
        foodItem.setFoodName(name);
        foodItem.setDescription(description);
        foodItem.setPrice(price);
        return foodItem;
    }

    private Order createTestOrderForTableAndCustomer(User customer, TableTop table) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setTable(table);
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PLACED);
        order.setNotes("Test order");
        
        // Create order items
        OrderItem item1 = createTestOrderItem(testFood1, 2, "Extra cheese");
        OrderItem item2 = createTestOrderItem(testFood2, 1, "No onions");
        
        item1.setOrder(order);
        item2.setOrder(order);
        
        order.setOrderItems(new ArrayList<>(Arrays.asList(item1, item2)));
        order.setTotalPrice(
            testFood1.getPrice().multiply(BigDecimal.valueOf(2))
            .add(testFood2.getPrice().multiply(BigDecimal.valueOf(1)))
        );
        
        return order;
    }

    private Order createTestOrderForAddressAndCustomer(User customer, Address address) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setAddress(address);
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PLACED);
        order.setNotes("Delivery order");
        
        // Create order item
        OrderItem item = createTestOrderItem(testFood3, 1, "Fresh please");
        item.setOrder(order);
        
        order.setOrderItems(new ArrayList<>(Collections.singletonList(item)));
        order.setTotalPrice(testFood3.getPrice());
        
        return order;
    }

    private OrderItem createTestOrderItem(FoodItem foodItem, int quantity, String note) {
        OrderItem orderItem = new OrderItem();
        orderItem.setFoodItem(foodItem);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(foodItem.getPrice());
        orderItem.setTotalPrice(foodItem.getPrice().multiply(BigDecimal.valueOf(quantity)));
        orderItem.setNote(note);
        return orderItem;
    }

    private PlaceOrderDto createTestPlaceOrderDto() {
        PlaceOrderDto placeOrderDto = new PlaceOrderDto();
        placeOrderDto.setNotes("Test order from integration test");
        
        OrderItemDtoBasic item1 = new OrderItemDtoBasic(2, "Extra cheese", testFood1.getFoodName());
        OrderItemDtoBasic item2 = new OrderItemDtoBasic(1, "No onions", testFood2.getFoodName());
        
        placeOrderDto.setOrderItems(new ArrayList<>(Arrays.asList(item1, item2)));
        return placeOrderDto;
    }
}
