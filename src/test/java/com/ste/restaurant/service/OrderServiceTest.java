package com.ste.restaurant.service;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.dto.common.WarningResponse;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.entity.enums.OrderStatus;
import com.ste.restaurant.entity.enums.TableStatus;
import com.ste.restaurant.entity.enums.UserRole;
import com.ste.restaurant.exception.*;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private FoodItemRepository foodItemRepository;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TableTopRepository tableTopRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderDto testOrderDto;
    private User testCustomer;
    private TableTop testTable;
    private Address testAddress;
    private FoodItem testFoodItem;
    private OrderItem testOrderItem;
    private OrderItemDto testOrderItemDto;
    private PlaceOrderDto testPlaceOrderDto;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Test customer
        testCustomer = new User();
        testCustomer.setUserId(1L);
        testCustomer.setEmail("customer@test.com");
        testCustomer.setRole(UserRole.CUSTOMER);
        testCustomer.setAddresses(new ArrayList<>());

        // Test table
        testTable = new TableTop();
        testTable.setTableId(1L);
        testTable.setTableNumber("T01");
        testTable.setTableStatus(TableStatus.AVAILABLE);

        // Test address
        testAddress = new Address();
        testAddress.setAddressId(1L);
        testAddress.setStreet("Test Street");
        testCustomer.getAddresses().add(testAddress);

        // Test food item
        testFoodItem = new FoodItem();
        testFoodItem.setFoodId(1L);
        testFoodItem.setFoodName("Pizza");
        testFoodItem.setPrice(BigDecimal.valueOf(15.00));

        // Test order item
        testOrderItem = new OrderItem();
        testOrderItem.setOrderItemId(1L);
        testOrderItem.setFoodItem(testFoodItem);
        testOrderItem.setQuantity(2);
        testOrderItem.setUnitPrice(BigDecimal.valueOf(15.00));
        testOrderItem.setTotalPrice(BigDecimal.valueOf(30.00));

        // Test order
        testOrder = new Order();
        testOrder.setOrderId(1L);
        testOrder.setCustomer(testCustomer);
        testOrder.setTable(testTable);
        testOrder.setStatus(OrderStatus.PLACED);
        testOrder.setOrderTime(LocalDateTime.now());
        testOrder.setTotalPrice(BigDecimal.valueOf(30.00));
        testOrder.setOrderItems(new ArrayList<>(Arrays.asList(testOrderItem)));
        testOrderItem.setOrder(testOrder);

        // Test DTOs
        testOrderDto = new OrderDto();
        testOrderDto.setOrderId(1L);
        testOrderDto.setStatus("PLACED");
        testOrderDto.setTotalPrice(BigDecimal.valueOf(30.00));

        testOrderItemDto = new OrderItemDto();
        testOrderItemDto.setOrderItemId(1L);
        testOrderItemDto.setQuantity(2);

        OrderItemDtoBasic orderItemDtoBasic = new OrderItemDtoBasic();
        orderItemDtoBasic.setFoodName("Pizza");
        orderItemDtoBasic.setQuantity(2);

        testPlaceOrderDto = new PlaceOrderDto();
        testPlaceOrderDto.setTableNumber("T01");
        testPlaceOrderDto.setOrderItems(Arrays.asList(orderItemDtoBasic));
        testPlaceOrderDto.setNotes("Test order");

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getOrderList_success() {
        // Arrange
        List<Order> orders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders, pageable, 1);
        when(orderRepository.findAll(pageable)).thenReturn(orderPage);
        when(orderMapper.orderToOrderDto(testOrder)).thenReturn(testOrderDto);

        // Act
        Page<OrderDto> result = orderService.getOrderList(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderId()).isEqualTo(1L);
        verify(orderRepository).findAll(pageable);
        verify(orderMapper).orderToOrderDto(testOrder);
    }

    @Test
    void getAllOrdersBy_success() {
        // Arrange
        List<Order> orders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders, pageable, 1);
        when(orderRepository.findAllByStatus(OrderStatus.PLACED, pageable)).thenReturn(orderPage);
        when(orderMapper.orderToOrderDto(testOrder)).thenReturn(testOrderDto);

        // Act
        Page<OrderDto> result = orderService.getAllOrdersBy("PLACED", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAllByStatus(OrderStatus.PLACED, pageable);
        verify(orderMapper).orderToOrderDto(testOrder);
    }

    @Test
    void getAllOrdersBy_invalidStatus() {
        // Act & Assert
        assertThatThrownBy(() -> orderService.getAllOrdersBy("INVALID_STATUS", pageable))
                .isInstanceOf(InvalidValueException.class);
        verify(orderRepository, never()).findAllByStatus(any(), any());
    }

//    @Test
//    void getAllOrdersForChef_success() {
//        // Arrange
//        List<Order> orders = Arrays.asList(testOrder);
//        Page<Order> orderPage = new PageImpl<>(orders, pageable, 1);
//        List<OrderStatus> chefStatuses = List.of(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.READY);
//        when(orderRepository.findAllByStatusIn(chefStatuses, pageable)).thenReturn(orderPage);
//        when(orderMapper.orderToOrderDto(testOrder)).thenReturn(testOrderDto);
//
//        // Act
//        Page<OrderDto> result = orderService.getAllOrdersBy(, pageable);
//
//        // Assert
//        assertThat(result).isNotNull();
//        assertThat(result.getContent()).hasSize(1);
//        verify(orderRepository).findAllByStatusIn(chefStatuses, pageable);
//        verify(orderMapper).orderToOrderDto(testOrder);
//    }

    @Test
    void getOrderById_success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderMapper.orderToOrderDto(testOrder)).thenReturn(testOrderDto);

        // Act
        OrderDto result = orderService.getOrderById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        verify(orderRepository).findById(1L);
        verify(orderMapper).orderToOrderDto(testOrder);
    }

    @Test
    void getOrderById_notFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(NotFoundException.class);
        verify(orderRepository).findById(999L);
    }

    @Test
    void getOrderById_withEmail_success() {
        // Arrange
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderMapper.orderToOrderDto(testOrder)).thenReturn(testOrderDto);

        // Act
        OrderDto result = orderService.getOrderById(1L, "customer@test.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        verify(userRepository).findByEmail("customer@test.com");
        verify(orderRepository).findById(1L);
        verify(orderMapper).orderToOrderDto(testOrder);
    }

    @Test
    void getOrderById_withEmail_userNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(1L, "nonexistent@test.com"))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findByEmail("nonexistent@test.com");
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void getOrderById_withEmail_orderNotBelongsToUser() {
        // Arrange
        User otherUser = new User();
        otherUser.setEmail("other@test.com");
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherUser));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(1L, "other@test.com"))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findByEmail("other@test.com");
        verify(orderRepository).findById(1L);
    }

    @Test
    void deleteOrderById_success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderMapper.orderToOrderDto(testOrder)).thenReturn(testOrderDto);

        // Act
        OrderDto result = orderService.deleteOrderById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        verify(orderRepository).findById(1L);
        verify(orderRepository).delete(testOrder);
        verify(orderMapper).orderToOrderDto(testOrder);
    }

    @Test
    void updateOrderStatus_success() {
        // Arrange
        StringDto statusDto = new StringDto("PREPARING");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(testOrder)).thenReturn(testOrder);
        when(orderMapper.orderToOrderDto(testOrder)).thenReturn(testOrderDto);

        // Act
        OrderDto result = orderService.updateOrderStatus(1L, statusDto);

        // Assert
        assertThat(result).isNotNull();
        verify(orderRepository, times(2)).findById(1L); // Called twice: once in updateOrderStatus, once in getOrderById
        verify(orderRepository).save(testOrder);
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void updateOrderStatus_invalidStatus() {
        // Arrange
        StringDto statusDto = new StringDto("INVALID_STATUS");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, statusDto))
                .isInstanceOf(InvalidValueException.class);
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatus_alreadyHasStatus() {
        // Arrange
        StringDto statusDto = new StringDto("PLACED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, statusDto))
                .isInstanceOf(AlreadyHasException.class);
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_withTable_success() {
        // Arrange
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(testCustomer));
        when(tableTopRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));
        when(foodItemRepository.findByFoodName("Pizza")).thenReturn(Optional.of(testFoodItem));
        when(menuRepository.existsByActiveAndFoodItemsContains(anyBoolean(), anySet())).thenReturn(true);
        when(orderMapper.orderItemDtoBasicToOrderItem(any())).thenReturn(testOrderItem);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.orderToOrderDto(any(Order.class))).thenReturn(testOrderDto);

        // Act
        WarningResponse<OrderDto> result = orderService.placeOrder(testPlaceOrderDto, "customer@test.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getWarnings()).isEmpty();
        verify(userRepository).findByEmail("customer@test.com");
        verify(tableTopRepository).findByTableNumber("T01");
        verify(foodItemRepository).findByFoodName("Pizza");
        verify(orderRepository).save(any(Order.class));
        verify(tableTopRepository).save(testTable);
        assertThat(testTable.getTableStatus()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void placeOrder_withAddress_success() {
        // Arrange
        testPlaceOrderDto.setTableNumber(null);
        testPlaceOrderDto.setAddressId(1L);
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(testCustomer));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));
        when(foodItemRepository.findByFoodName("Pizza")).thenReturn(Optional.of(testFoodItem));
        when(menuRepository.existsByActiveAndFoodItemsContains(anyBoolean(), anySet())).thenReturn(true);
        when(orderMapper.orderItemDtoBasicToOrderItem(any())).thenReturn(testOrderItem);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.orderToOrderDto(any(Order.class))).thenReturn(testOrderDto);

        // Act
        WarningResponse<OrderDto> result = orderService.placeOrder(testPlaceOrderDto, "customer@test.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getWarnings()).isEmpty();
        verify(userRepository).findByEmail("customer@test.com");
        verify(addressRepository).findById(1L);
        verify(foodItemRepository).findByFoodName("Pizza");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void placeOrder_userNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.placeOrder(testPlaceOrderDto, "nonexistent@test.com"))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findByEmail("nonexistent@test.com");
    }

    @Test
    void placeOrder_noLocationProvided() {
        // Arrange
        testPlaceOrderDto.setTableNumber(null);
        testPlaceOrderDto.setAddressId(null);
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(testCustomer));

        // Act & Assert
        assertThatThrownBy(() -> orderService.placeOrder(testPlaceOrderDto, "customer@test.com"))
                .isInstanceOf(InvalidValueException.class);
        verify(userRepository).findByEmail("customer@test.com");
    }

    @Test
    void placeOrder_invalidFoodItems() {
        // Arrange
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(testCustomer));
        when(tableTopRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));
        when(foodItemRepository.findByFoodName("Pizza")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.placeOrder(testPlaceOrderDto, "customer@test.com"))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("No valid order items found");
        verify(userRepository).findByEmail("customer@test.com");
        verify(tableTopRepository).findByTableNumber("T01");
        verify(foodItemRepository).findByFoodName("Pizza");
    }

    @Test
    void cancelOrderIfNotReady_customer_success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(testCustomer));
        when(orderRepository.save(testOrder)).thenReturn(testOrder);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderMapper.orderToOrderDto(testOrder)).thenReturn(testOrderDto);

        // Act
        OrderDto result = orderService.cancelOrderIfNotReady(1L, "customer@test.com");

        // Assert
        assertThat(result).isNotNull();
        verify(orderRepository, times(2)).findById(1L);
        verify(userRepository).findByEmail("customer@test.com");
        verify(orderRepository).save(testOrder);
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrderIfNotReady_admin_success() {
        // Arrange
        User adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(UserRole.ADMIN);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(orderRepository.save(testOrder)).thenReturn(testOrder);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderMapper.orderToOrderDto(testOrder)).thenReturn(testOrderDto);

        // Act
        OrderDto result = orderService.cancelOrderIfNotReady(1L, "admin@test.com");

        // Assert
        assertThat(result).isNotNull();
        verify(orderRepository, times(2)).findById(1L);
        verify(userRepository).findByEmail("admin@test.com");
        verify(orderRepository).save(testOrder);
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrderIfNotReady_orderNotReady() {
        // Arrange
        testOrder.setStatus(OrderStatus.READY);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(testCustomer));

        // Act & Assert
        assertThatThrownBy(() -> orderService.cancelOrderIfNotReady(1L, "customer@test.com"))
                .isInstanceOf(InvalidValueException.class);
        verify(orderRepository).findById(1L);
        verify(userRepository).findByEmail("customer@test.com");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void changeTableOfOrder_success() {
        // Arrange
        TableTop newTable = new TableTop();
        newTable.setTableNumber("T02");
        newTable.setTableStatus(TableStatus.AVAILABLE);
        
        StringDto tableDto = new StringDto("T02");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(tableTopRepository.findByTableNumber("T02")).thenReturn(Optional.of(newTable));
        when(orderRepository.save(testOrder)).thenReturn(testOrder);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderMapper.orderToOrderDto(testOrder)).thenReturn(testOrderDto);

        // Act
        OrderDto result = orderService.changeTableOfOrder(1L, tableDto);

        // Assert
        assertThat(result).isNotNull();
        verify(orderRepository, times(2)).findById(1L);
        verify(tableTopRepository).findByTableNumber("T02");
        verify(tableTopRepository, times(2)).save(any(TableTop.class));
        verify(orderRepository).save(testOrder);
        assertThat(testTable.getTableStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(newTable.getTableStatus()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void changeTableOfOrder_sameTable() {
        // Arrange
        StringDto tableDto = new StringDto("T01");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(tableTopRepository.findByTableNumber("T01")).thenReturn(Optional.of(testTable));

        // Act & Assert
        assertThatThrownBy(() -> orderService.changeTableOfOrder(1L, tableDto))
                .isInstanceOf(AlreadyHasException.class);
        verify(orderRepository).findById(1L);
        verify(tableTopRepository).findByTableNumber("T01");
    }

    @Test
    void getLastOrder_success() {
        // Arrange
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findFirstByCustomerEmailOrderByOrderTimeDesc("customer@test.com")).thenReturn(testOrder);
        when(orderMapper.orderToOrderDto(testOrder)).thenReturn(testOrderDto);

        // Act
        OrderDto result = orderService.getLastOrder("customer@test.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        verify(userRepository).findByEmail("customer@test.com");
        verify(orderRepository).findFirstByCustomerEmailOrderByOrderTimeDesc("customer@test.com");
        verify(orderMapper).orderToOrderDto(testOrder);
    }

    @Test
    void getLastOrder_noOrderFound() {
        // Arrange
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findFirstByCustomerEmailOrderByOrderTimeDesc("customer@test.com")).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> orderService.getLastOrder("customer@test.com"))
                .isInstanceOf(NotFoundException.class);
        verify(userRepository).findByEmail("customer@test.com");
        verify(orderRepository).findFirstByCustomerEmailOrderByOrderTimeDesc("customer@test.com");
    }

    @Test
    void getOrders_success() {
        // Arrange
        List<Order> orders = Arrays.asList(testOrder);
        List<OrderDto> orderDtos = Arrays.asList(testOrderDto);
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(testCustomer));
        when(orderRepository.findAllByCustomerEmailOrderByOrderTimeDesc("customer@test.com")).thenReturn(orders);
        when(orderMapper.ordersToOrderDtos(orders)).thenReturn(orderDtos);

        // Act
        List<OrderDto> result = orderService.getOrders("customer@test.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(userRepository).findByEmail("customer@test.com");
        verify(orderRepository).findAllByCustomerEmailOrderByOrderTimeDesc("customer@test.com");
        verify(orderMapper).ordersToOrderDtos(orders);
    }

    @Test
    void getOrderItemsFromOrder_success() {
        // Arrange
        List<OrderItemDto> orderItemDtos = Arrays.asList(testOrderItemDto);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderMapper.orderItemsToOrderItemDtos(testOrder.getOrderItems())).thenReturn(orderItemDtos);

        // Act
        List<OrderItemDto> result = orderService.getOrderItemsFromOrder(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(orderRepository).findById(1L);
        verify(orderMapper).orderItemsToOrderItemDtos(testOrder.getOrderItems());
    }

    @Test
    void getOrderItemList_success() {
        // Arrange
        List<OrderItem> orderItems = Arrays.asList(testOrderItem);
        Page<OrderItem> orderItemPage = new PageImpl<>(orderItems, pageable, 1);
        when(orderItemRepository.findAll(pageable)).thenReturn(orderItemPage);
        when(orderMapper.orderItemToOrderItemDto(testOrderItem)).thenReturn(testOrderItemDto);

        // Act
        Page<OrderItemDto> result = orderService.getOrderItemList(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(orderItemRepository).findAll(pageable);
        verify(orderMapper).orderItemToOrderItemDto(testOrderItem);
    }

    @Test
    void getOrderItemsForUser_success() {
        // Arrange
        List<OrderItemDto> orderItemDtos = Arrays.asList(testOrderItemDto);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderMapper.orderItemsToOrderItemDtos(testOrder.getOrderItems())).thenReturn(orderItemDtos);

        // Act
        List<OrderItemDto> result = orderService.getOrderItemsForUser(1L, "customer@test.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(orderRepository).findById(1L);
        verify(orderMapper).orderItemsToOrderItemDtos(testOrder.getOrderItems());
    }

    @Test
    void getOrderItemsForUser_orderNotBelongsToUser() {
        // Arrange
        User otherUser = new User();
        otherUser.setEmail("other@test.com");
        testOrder.setCustomer(otherUser);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderItemsForUser(1L, "customer@test.com"))
                .isInstanceOf(NotFoundException.class);
        verify(orderRepository).findById(1L);
    }
}
