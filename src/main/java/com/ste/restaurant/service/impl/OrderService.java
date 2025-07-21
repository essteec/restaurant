package com.ste.restaurant.service.impl;

import com.ste.restaurant.dto.OrderDto;
import com.ste.restaurant.dto.OrderItemDto;
import com.ste.restaurant.dto.PlaceOrderDto;
import com.ste.restaurant.dto.userdto.UserDto;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.FoodItemRepository;
import com.ste.restaurant.repository.OrderItemRepository;
import com.ste.restaurant.repository.OrderRepository;
import com.ste.restaurant.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    public List<OrderDto> getOrderList() {
        List<Order> orders = orderRepository.findAll();
        List<OrderDto> orderDtos = new ArrayList<>();

        for (Order order : orders) {
            OrderDto orderDto = orderMapper.orderToOrderDto(order);
            orderDtos.add(orderDto);
        }
        return orderDtos;
    }

    public OrderDto getOrderById(Long id) {
        Optional<Order> order = orderRepository.findById(id);
        if (order.isEmpty()) {
            return null;
        }
        return orderMapper.orderToOrderDto(order.get());
    }

    public List<OrderItemDto> getOrderItemsFromOrder(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if  (orderOpt.isEmpty()) {
            return null;
        }
        Order order = orderOpt.get();

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
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return null;
        }
        Order order = orderOpt.get();
        orderItemRepository.deleteAll(order.getOrderItems());
        orderRepository.delete(order);
        return orderMapper.orderToOrderDto(order);
    }

    // Customer
    public OrderDto placeOrder(PlaceOrderDto placeOrderDto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Order order = new Order();
        order.setNotes(placeOrderDto.getNotes());
        order.setCustomer(user);

        if (placeOrderDto.getAddress() != null) {
            Address address = orderMapper.addressDtoToAddress(placeOrderDto.getAddress());
            order.setAddress(address);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemDto orderItemDto : placeOrderDto.getOrderItems()) {
            OrderItem orderItem = orderMapper.orderItemDtoToOrderItem(orderItemDto);
            orderItem.setOrder(order);

            if (orderItemDto.getFoodItem() != null) {
                FoodItem foodItem = foodItemRepository.findByFoodName(orderItemDto.getFoodItem().getFoodName())
                        .orElseThrow(() -> new RuntimeException("Food item not found"));
                orderItem.setFoodItem(foodItem);
                orderItem.setUnitPrice(foodItem.getPrice());
                orderItem.setTotalPrice(foodItem.getPrice().multiply(BigDecimal.valueOf(orderItemDto.getQuantity())));
            }

            orderItems.add(orderItem);
        }
        order.setOrderItems(orderItems);

        BigDecimal totalOrderPrice = orderItems.stream()
                        .map(OrderItem::getTotalPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalPrice(totalOrderPrice);

        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PLACED);

        orderRepository.save(order);

        OrderDto orderDto = getOrderById(order.getOrderId());
        orderDto.setCustomer(null);
        return orderDto;
    }

    public OrderDto getLastOrder(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Order order = orderRepository.findFirstByCustomerEmailOrderByOrderTimeDesc(user.getEmail());
        if (order == null) {
            return null;
        }
        OrderDto orderDto = orderMapper.orderToOrderDto(order);
        orderDto.setCustomer(null);
        return orderDto;
    }

    public List<OrderDto> getOrders(String email) {
        User user =  userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Order> orders = orderRepository.findAllByCustomerEmailOrderByOrderTimeDesc(user.getEmail());
        List<OrderDto> orderDtos = new ArrayList<>();
        for (Order order : orders) {
            OrderDto orderDto = orderMapper.orderToOrderDto(order);
            orderDto.setCustomer(null);
            orderDtos.add(orderDto);
        }
        return orderDtos;
    }

    //  by admin or waiter
    public OrderDto updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new UsernameNotFoundException("Order not found"));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status" + status);
        }

        if (order.getStatus() == newStatus) {
            throw new RuntimeException("Order already has this status");
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        return getOrderById(orderId);
    }

    // by customer or waiter
    public OrderDto cancelOrderIfNotReady(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new UsernameNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.PLACED || order.getStatus() == OrderStatus.PREPARING) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
        return getOrderById(orderId);
    }
}