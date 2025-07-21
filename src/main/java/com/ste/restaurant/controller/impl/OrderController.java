package com.ste.restaurant.controller.impl;

import com.ste.restaurant.dto.OrderDto;
import com.ste.restaurant.dto.OrderItemDto;
import com.ste.restaurant.entity.Order;
import com.ste.restaurant.service.impl.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("rest/api/order")
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // List orders and orderItems
    @GetMapping(path = "/order-list")
    public List<OrderDto> getOrderList() {
        return orderService.getOrderList();
    }

    @GetMapping(path = "/orders/{id}")
    public OrderDto getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping(path = "/orders/{orderId}/items")
    public List<OrderItemDto> getOrderItemsFromOrder(@PathVariable Long orderId) {
        return orderService.getOrderItemsFromOrder(orderId);
    }

    @GetMapping(path = "/orderitem-list")
    public List<OrderItemDto> getOrderItemList() {
        return orderService.getOrderItemList();
    }

    // delete order with its contents
    @DeleteMapping(name = "/orders/{id}")
    public OrderDto deleteOrderById(@PathVariable Long id) {
        return orderService.deleteOrderById(id);
    }
}
