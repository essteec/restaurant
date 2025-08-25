package com.ste.restaurant.service;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.dto.common.WarningResponse;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.exception.*;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final FoodItemRepository foodItemRepository;
    private final UserRepository userRepository;
    private final TableTopRepository tableTopRepository;
    private final AddressRepository addressRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepo, OrderItemRepository orderItemRepo,
                        FoodItemRepository foodItemRepo, UserRepository userRepo,
                        TableTopRepository tableTopRepo, AddressRepository addressRepo, OrderMapper orderMapper) {
        this.orderRepository = orderRepo;
        this.orderItemRepository = orderItemRepo;
        this.foodItemRepository = foodItemRepo;
        this.userRepository = userRepo;
        this.tableTopRepository = tableTopRepo;
        this.addressRepository = addressRepo;
        this.orderMapper = orderMapper;
    }

    public Page<OrderDto> getOrderList(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);

        return orders.map(orderMapper::orderToOrderDto);
    }

    public Page<OrderDto> getAllOrdersBy(String status, Pageable pageable) {
        OrderStatus orderStatus;
        try {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("Order", "status", status);
        }

        Page<Order> orders = orderRepository.findAllByStatus(orderStatus, pageable);
        return orders.map(orderMapper::orderToOrderDto);
    }

    // get all orders for waiter or chef
    public Page<OrderDto> getAllOrdersBy(List<OrderStatus> statuses, Pageable pageable) {
        Page<Order> orders = orderRepository.findAllByStatusInAndOrderTimeAfter(statuses,
                LocalDate.now().atStartOfDay().plusHours(6), pageable);
        return orders.map(orderMapper::orderToOrderDto);
    }

    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order", id));
        return orderMapper.orderToOrderDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long id,  String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order", id));

        if (!order.getCustomer().getEmail().equals(email)) {
            throw new NotFoundException("Order", id);
        }
        return orderMapper.orderToOrderDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderItemDto> getOrderItemsFromOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));

        return orderMapper.orderItemsToOrderItemDtos(order.getOrderItems());
    }

    public Page<OrderItemDto> getOrderItemList(Pageable pageable) {
        Page<OrderItem> orderItems = orderItemRepository.findAll(pageable);
        return orderItems.map(orderMapper::orderItemToOrderItemDto);
    }

    @Transactional
    public OrderDto deleteOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order", id));

        orderRepository.delete(order);
        return orderMapper.orderToOrderDto(order);
    }

    //  by admin or waiter
    @Transactional
    public OrderDto updateOrderStatus(Long orderId, StringDto statusDto) {
        String status = statusDto.getName();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("Order", "status", status);
        }

        if (order.getStatus() == newStatus) {
            throw new AlreadyHasException("Order", "status", status);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        if (newStatus == OrderStatus.COMPLETED) {
            order.getTable().setTableStatus(TableStatus.AVAILABLE);
            tableTopRepository.save(order.getTable());
        }
        if (newStatus.equals(OrderStatus.DELIVERED)) {
            mergeRecentOrders(order);
        }
        return getOrderById(orderId);
    }

    // Customer
    @Transactional
    public WarningResponse<OrderDto> placeOrder(PlaceOrderDto placingDto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        Order order = new Order();

        order.setCustomer(user);

        order.setNotes(placingDto.getNotes());
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PLACED);

        if (placingDto.getAddressId() != null) {
            Address address = addressRepository.findById(placingDto.getAddressId())
                    .orElseThrow(() -> new  NotFoundException("Address", placingDto.getAddressId()));
            if (user.getRole().equals(UserRole.CUSTOMER) && !user.getAddresses().contains(address)) {
                throw new NotFoundException("Address", placingDto.getAddressId());
            }
            order.setAddress(address);
        }
        else if (placingDto.getTableNumber() != null) {
            TableTop table = tableTopRepository.findByTableNumber(placingDto.getTableNumber())
                    .orElseThrow(() -> new NotFoundException("Table", placingDto.getTableNumber()));

            table.setTableStatus(TableStatus.OCCUPIED);
            tableTopRepository.save(table);
            order.setTable(table);

        } else {
            throw new InvalidValueException("Order", "location", "Either table number or address must be provided");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        List<String> failedNames = new ArrayList<>();

        for (OrderItemDtoBasic orderItemDto : placingDto.getOrderItems()) {
            FoodItem foodItem = foodItemRepository.findByFoodName(orderItemDto.getFoodName())
                    .orElse(null);
            if (foodItem == null) {
                failedNames.add(orderItemDto.getFoodName());
                continue;
            }
            OrderItem orderItem = orderMapper.orderItemDtoBasicToOrderItem(orderItemDto);
            orderItem.setFoodItem(foodItem);
            orderItem.setUnitPrice(foodItem.getPrice());

            BigDecimal itemTotal = foodItem.getPrice().multiply(BigDecimal.valueOf(orderItemDto.getQuantity()));
            orderItem.setTotalPrice(itemTotal);
            orderItem.setOrder(order);

            totalPrice = totalPrice.add(itemTotal);
            orderItems.add(orderItem);
        }

        if (orderItems.isEmpty()) {
            throw new InvalidValueException("Order", "OrderItems", "No valid order items found");
        }

        order.setOrderItems(orderItems);
        order.setTotalPrice(totalPrice);

        orderRepository.save(order);

        OrderDto orderDto = orderMapper.orderToOrderDto(order);
        orderDto.setCustomer(null);
        return new WarningResponse<>(orderDto, failedNames);
    }

    @Transactional(readOnly = true)
    public OrderDto getLastOrder(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        Order order = orderRepository.findFirstByCustomerEmailOrderByOrderTimeDesc(user.getEmail());
        if (order == null) {
            throw new NotFoundException("Order");
        }
        OrderDto orderDto = orderMapper.orderToOrderDto(order);
        orderDto.setCustomer(null);
        return orderDto;
    }

    public List<OrderDto> getOrders(String email) {
        User user =  userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        List<Order> orders;
        if (user.getRole().equals(UserRole.CUSTOMER)) {
            orders = orderRepository.findAllByCustomerEmailOrderByOrderTimeDesc(user.getEmail());
        }
        else if (user.getRole().equals(UserRole.WAITER)) {
            // get all the orders that are not completed
            orders = orderRepository.findAllByStatusNotAndOrderTimeAfterOrderByOrderTimeDesc(
                    OrderStatus.COMPLETED,
                    LocalDate.now().atStartOfDay().plusHours(6)
            );
        }
        else if (user.getRole().equals(UserRole.CHEF)) {
            // get all the orders that are in placed, preparing, ready, cancelled.
            orders = orderRepository.findAllByStatusInAndOrderTimeAfterOrderByOrderTimeDesc(
                    List.of(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.CANCELLED),
                    LocalDate.now().atStartOfDay().plusHours(6)
            );
        }
        else {
            throw new InvalidValueException("User", "role", "Unsupported user role for this operation");
        }

        return orderMapper.ordersToOrderDtos(orders);
    }

    // by customer or waiter
    @Transactional
    public OrderDto cancelOrderIfNotReady(Long orderId, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.WAITER) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            return getOrderById(orderId);
        }

        if (!order.getCustomer().getEmail().equals(email)) {
            throw new NotFoundException("Order", orderId);
        }

        if (order.getStatus() == OrderStatus.PLACED || order.getStatus() == OrderStatus.PREPARING) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
        else throw new InvalidValueException("Order", "status", "Cannot cancel this order is already in the way");
        return getOrderById(orderId);
    }

    // by admin or waiter
    @Transactional
    public OrderDto changeTableOfOrder(Long orderId, StringDto tableNumberDto) {
        String tableNumber = tableNumberDto.getName();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));

        TableTop table = tableTopRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> new NotFoundException("Table", tableNumber));

        if (order.getTable() == null) {
            throw new NullValueException("Order", "table");
        }

        if (tableNumber.equals(order.getTable().getTableNumber())) {
            throw new AlreadyHasException("Table", "number", tableNumber);
        }

        // set old table as available
        order.getTable().setTableStatus(TableStatus.AVAILABLE);
        tableTopRepository.save(order.getTable());

        // set new table as occupied
        table.setTableStatus(TableStatus.OCCUPIED);
        tableTopRepository.save(table);

        // finally set order with new table
        order.setTable(table);
        orderRepository.save(order);

        return getOrderById(orderId);
    }

    @Transactional(readOnly = true)
    public List<OrderItemDto> getOrderItemsForUser(Long orderId, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));

        if (!order.getCustomer().getEmail().equals(email)) {
            throw new NotFoundException("Order", orderId);
        }

        return orderMapper.orderItemsToOrderItemDtos(order.getOrderItems());
    }

    @Transactional
    public void mergeRecentOrders(Order mainOrder) {
        if (mainOrder.getTable() == null) return;

        List<Order> recentOrders = orderRepository.findAllByCustomerAndTableAndStatusAndOrderTimeBetween(
                mainOrder.getCustomer(),
                mainOrder.getTable(),
                OrderStatus.DELIVERED,
                LocalDateTime.now().minusMinutes(90),
                mainOrder.getOrderTime().minusMinutes(1)
        );

        if (recentOrders.isEmpty()) return;

        BigDecimal total = mainOrder.getTotalPrice();
        StringBuilder mergedNotes = new StringBuilder(mainOrder.getNotes() != null ? mainOrder.getNotes() : "");

        for (Order order : recentOrders) {
            total = total.add(order.getTotalPrice());

            if (order.getNotes() != null && !order.getNotes().trim().isEmpty()) {
                if (!mergedNotes.isEmpty()) {
                    mergedNotes.append("\n");
                }
                mergedNotes.append(order.getNotes());
            }

            for (OrderItem item : order.getOrderItems()) {
                item.setOrder(mainOrder);
                mainOrder.getOrderItems().add(item);
            }
        }

        mainOrder.setTotalPrice(total);
        mainOrder.setNotes(mergedNotes.toString());

        orderRepository.save(mainOrder);
        orderRepository.deleteAll(recentOrders);
    }
}