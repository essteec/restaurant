package com.ste.restaurant.service;

import com.ste.restaurant.dto.OrderDto;
import com.ste.restaurant.dto.OrderItemDto;
import com.ste.restaurant.dto.OrderItemDtoBasic;
import com.ste.restaurant.dto.PlaceOrderDto;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.mapper.OrderMapper;
import com.ste.restaurant.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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

    public OrderDto getOrderById(Long id) {
        Optional<Order> order = orderRepository.findById(id);
        if (order.isEmpty()) {
            return null;
        }
        return orderMapper.orderToOrderDto(order.get());
    }

    public OrderDto getOrderById(Long id,  String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return null;
        }
        Order order = orderOpt.get();

        if (!order.getCustomer().getEmail().equals(email)) {
            throw new AccessDeniedException("Access denied");
        }
        return orderMapper.orderToOrderDto(order);
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

        if (newStatus.equals(OrderStatus.COMPLETED)) {
            mergeRecentOrders(order.getOrderId());
        }

        order.setStatus(newStatus);

        orderRepository.save(order);
        return getOrderById(orderId);
    }

    // Customer
    public OrderDto placeOrder(PlaceOrderDto placingDto, String email) {
        if (placingDto.getOrderItems() == null || placingDto.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("Order has no order items");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Order order = new Order();
        order.setCustomer(user);
        order.setNotes(placingDto.getNotes());
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PLACED);

        if (placingDto.getAddressId() != null) {
            Address address = addressRepository.findById(placingDto.getAddressId()).orElse(null);
            if (address != null && user.getAddresses().contains(address)) {
                order.setAddress(address);
            }
        }
        if (placingDto.getTableNumber() != null) {
            TableTop table = tableTopRepository.findByTableNumber(placingDto.getTableNumber()).orElse(null);
            if (table != null) {
                table.setTableStatus(TableStatus.OCCUPIED);
                order.setTable(table);
            }
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemDtoBasic orderItemDto : placingDto.getOrderItems()) {
            FoodItem foodItem = foodItemRepository.findByFoodName(orderItemDto.getFoodName())
                    .orElse(null);
            if (foodItem == null) {
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
        order.setOrderItems(orderItems);
        order.setTotalPrice(totalPrice);

        orderRepository.save(order);

        OrderDto orderDto = orderMapper.orderToOrderDto(order);
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

    // by customer or waiter
    public OrderDto cancelOrderIfNotReady(Long orderId, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().getEmail().equals(email)) {
            return null;
        }

        if (order.getStatus() == OrderStatus.PLACED || order.getStatus() == OrderStatus.PREPARING) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
        return getOrderById(orderId);
    }

    // todo change table

    public List<OrderItemDto> getOrderItemsForUser(Long orderId, String email) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return null;
        }
        Order order = orderOpt.get();

        if (!order.getCustomer().getEmail().equals(email)) {
            throw new AccessDeniedException("Not allowed to view items of this order.");
        }

        List<OrderItemDto> orderItemDtos = new ArrayList<>();
        for (OrderItem orderItem : order.getOrderItems()) {
            orderItemDtos.add(orderMapper.orderItemToOrderItemDto(orderItem));
        }

        return orderItemDtos;
    }

    // by waiter or admin
    // to do addOrderItem to existing Order ?
//    public OrderDto addOrderItem(Long OrderId, OrderItemDtoBasic orderItemDtoBasic) {
//        Order order =  orderRepository.findById(OrderId)
//                .orElseThrow(() -> new UsernameNotFoundException("Order not found"));
//
//        OrderItem orderItem = orderMapper.orderItemDtoBasicToOrderItem(orderItemDtoBasic);
//        orderItem.setOrder(order);
//
//        List<OrderItem> orderItems = order.getOrderItems();
//
//        if (orderItemDtoBasic.getFoodName() != null) {
//            FoodItem foodItem = foodItemRepository.findByFoodName(orderItemDtoBasic.getFoodName())
//                    .orElseThrow(() -> new RuntimeException("Food item not found"));
//            orderItem.setFoodItem(foodItem);
//            orderItem.setUnitPrice(foodItem.getPrice());
//            orderItem.setTotalPrice(foodItem.getPrice().multiply(BigDecimal.valueOf(orderItemDtoBasic.getQuantity())));
//        }
//
//        order.getOrderItems().add(orderItem);
//
//        BigDecimal totalOrderPrice = orderItems.stream()
//                .map(OrderItem::getTotalPrice)
//        .reduce(BigDecimal.ZERO, BigDecimal::add);
//        order.setTotalPrice(totalOrderPrice);
//
//        orderRepository.save(order);
//        return orderMapper.orderToOrderDto(order);
//    }
    // todo
    @Transactional
    public OrderDto mergeRecentOrders(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Order not found"));

        Order order2 = orderRepository.findByCustomerAndAddressAndStatusAndOrderTimeAfterAndOrderTimeBefore(
                order.getCustomer(),
                order.getAddress(),
                OrderStatus.COMPLETED,
                LocalDateTime.now().minusMinutes(60),
                LocalDateTime.now().minusMinutes(1));

        if (order2 == null) {
            return null;
        }

        order.setTotalPrice(order.getTotalPrice().add(order2.getTotalPrice()));

        order.setNotes((order.getNotes() != null ? order.getNotes() : "") +
                (order2.getNotes() != null ? "\n" + order2.getNotes() : ""));

        order2.getOrderItems().forEach(item -> item.setOrder(order));
        order.getOrderItems().addAll(order2.getOrderItems());
        orderRepository.delete(order2);
        orderRepository.save(order);
        return orderMapper.orderToOrderDto(order);
    }
}