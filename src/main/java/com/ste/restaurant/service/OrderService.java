package com.ste.restaurant.service;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.exception.*;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private TableTopRepository tableTopRepository;

    public List<OrderDto> getOrderList() {
        List<Order> orders = orderRepository.findAll();
        List<OrderDto> orderDtos = new ArrayList<>();

        for (Order order : orders) {
            OrderDto orderDto = orderMapper.orderToOrderDto(order);
            orderDtos.add(orderDto);
        }
        return orderDtos;
    }

    public List<OrderDto> getAllOrdersBy(String status) {
        OrderStatus orderStatus;
        try {
            orderStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("Order", "status", status);
        }

        List<Order> orders = orderRepository.findAllByStatus(orderStatus);
        List<OrderDto> orderDtos = new ArrayList<>();
        for (Order order : orders) {
            OrderDto orderDto = orderMapper.orderToOrderDto(order);
            orderDtos.add(orderDto);
        }

        return orderDtos;
    }

    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order", id));
        return orderMapper.orderToOrderDto(order);
    }

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

    public List<OrderItemDto> getOrderItemsFromOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));

        List<OrderItemDto> orderItemDtos = new ArrayList<>();
        for (OrderItem orderItem : order.getOrderItems()) {
            OrderItemDto orderItemDto = orderMapper.orderItemToOrderItemDto(orderItem);
            orderItemDtos.add(orderItemDto);
        }

        return orderItemDtos;
    }

    public List<OrderItemDto> getOrderItemList() {
        List<OrderItemDto> orderItemDtos = new ArrayList<>();
        List<OrderItem> orderItems = orderItemRepository.findAll();
        for (OrderItem orderItem : orderItems) {
            OrderItemDto orderItemDto = orderMapper.orderItemToOrderItemDto(orderItem);
            orderItemDtos.add(orderItemDto);
        }
        return orderItemDtos;
    }

    @Transactional
    public OrderDto deleteOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order", id));

        orderItemRepository.deleteAll(order.getOrderItems());
        orderRepository.delete(order);
        return orderMapper.orderToOrderDto(order);
    }

    //  by admin or waiter
    public OrderDto updateOrderStatus(Long orderId, StringDto statusDto) {
        if (statusDto.getName() == null) {
            throw new NullValueException("Order", "status");
        }
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

        if (newStatus.equals(OrderStatus.COMPLETED)) {
            mergeRecentOrders(order);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
        return getOrderById(orderId);
    }

    // Customer
    public OrderDto placeOrder(PlaceOrderDto placingDto, String email) {
        if (placingDto.getOrderItems() == null || placingDto.getOrderItems().isEmpty()) {
            throw new NotFoundException("OrderItems");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));

        Order order = new Order();

        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.WAITER) {
            order.setCustomer(null);
        }  else order.setCustomer(user);

        order.setNotes(placingDto.getNotes());
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PLACED);

        if (placingDto.getAddressId() != null) {
            Address address = addressRepository.findById(placingDto.getAddressId()).orElse(null);
            if (address != null && user.getAddresses().contains(address)) {
                order.setAddress(address);
            }
        }
        else if (placingDto.getTableNumber() != null) {
            TableTop table = tableTopRepository.findByTableNumber(placingDto.getTableNumber()).orElse(null);
            if (table != null) {
                table.setTableStatus(TableStatus.OCCUPIED);
                order.setTable(table);
            }
        } else {
            throw new InvalidValueException("Order", "address id and table number", "[Null Value]");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemDtoBasic orderItemDto : placingDto.getOrderItems()) {
            FoodItem foodItem = foodItemRepository.findByFoodName(orderItemDto.getFoodName())
                    .orElse(null);
            if (foodItem == null) continue;

            OrderItem orderItem = orderMapper.orderItemDtoBasicToOrderItem(orderItemDto);
            orderItem.setFoodItem(foodItem);
            orderItem.setUnitPrice(foodItem.getPrice());

            BigDecimal itemTotal = foodItem.getPrice().multiply(BigDecimal.valueOf(orderItemDto.getQuantity()));
            orderItem.setTotalPrice(itemTotal);
            orderItem.setOrder(order);

            totalPrice = totalPrice.add(itemTotal);

            orderItems.add(orderItem);
        }
        order.setOrderItems(orderItems);
        order.setTotalPrice(totalPrice);

        orderRepository.save(order);

        OrderDto orderDto = orderMapper.orderToOrderDto(order);
        orderDto.setCustomer(null);
        return orderDto;
    }

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

        List<Order> orders = orderRepository.findAllByCustomerEmailOrderByOrderTimeDesc(user.getEmail());
        List<OrderDto> orderDtos = new ArrayList<>();
        for (Order order : orders) {
            OrderDto orderDto = orderMapper.orderToOrderDto(order);
            orderDto.setCustomer(null);
            orderDtos.add(orderDto);
        }
        return orderDtos;
    }

    // by customer or waiter
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
        else if (!order.getCustomer().getEmail().equals(email)) {
            throw new NotFoundException("Order", orderId);
        }

        if (order.getStatus() == OrderStatus.PLACED || order.getStatus() == OrderStatus.PREPARING) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
        return getOrderById(orderId);
    }

    // by admin or waiter
    public OrderDto changeTableOfOrder(Long orderId, StringDto tableNumberDto) {
        if (tableNumberDto == null) {
            throw new NullValueException("Table", "number");
        }
        String tableNumber = tableNumberDto.getName();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));

        TableTop table = tableTopRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> new NotFoundException("Table", tableNumber));

        if (tableNumber.equals(order.getTable().getTableNumber())) {
            throw new AlreadyHasException("Table", "number", tableNumber);
        }

        order.setTable(table);
        orderRepository.save(order);
        return getOrderById(orderId);
    }

    public List<OrderItemDto> getOrderItemsForUser(Long orderId, String email) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));

        if (!order.getCustomer().getEmail().equals(email)) {
            throw new NotFoundException("OrderItems");
        }

        List<OrderItemDto> orderItemDtos = new ArrayList<>();
        for (OrderItem orderItem : order.getOrderItems()) {
            orderItemDtos.add(orderMapper.orderItemToOrderItemDto(orderItem));
        }

        return orderItemDtos;
    }

    @Transactional
    public void mergeRecentOrders(Order mainOrder) {


        List<Order> recentOrders = orderRepository.findAllByCustomerAndAddressAndStatusAndOrderTimeBetween(
                mainOrder.getCustomer(),
                mainOrder.getAddress(),
                OrderStatus.COMPLETED,
                LocalDateTime.now().minusMinutes(60),
                LocalDateTime.now().minusMinutes(1)
        );

        recentOrders.removeIf(order -> order.getOrderId().equals(mainOrder.getOrderId()));
        for (Order order : recentOrders) {
            mainOrder.setTotalPrice(mainOrder.getTotalPrice().add(order.getTotalPrice()));

            mainOrder.setNotes(
                    (mainOrder.getNotes() != null ? mainOrder.getNotes() : "") +
                    (order.getNotes() != null ? "\n" + order.getNotes() : "")
            );

            order.getOrderItems().forEach(item -> item.setOrder(mainOrder));
            mainOrder.getOrderItems().addAll(order.getOrderItems());

            orderRepository.delete(order);
        }

        orderRepository.save(mainOrder);
    }
}