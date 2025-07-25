package com.ste.restaurant.controller;

import com.ste.restaurant.dto.OrderDto;
import com.ste.restaurant.dto.OrderItemDto;
import com.ste.restaurant.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/api/admin/orders")
public class AdminOrderController {
    @Autowired
    private OrderService orderService;

    // list orders and orderItems
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<OrderDto> getOrderList() {
        return orderService.getOrderList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/{id}")
    public OrderDto getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}")
    public OrderDto deleteOrderById(@PathVariable Long id) {
        return orderService.deleteOrderById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/{orderId}/items")
    public List<OrderItemDto> getOrderItemsFromOrder(@PathVariable Long orderId) {
        return orderService.getOrderItemsFromOrder(orderId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/items")
    public List<OrderItemDto> getOrderItemList() {
        return orderService.getOrderItemList();
    }
    // delete order with its contents

    // by waiter or chef?
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER', 'CHEF')")
    @PatchMapping(path = "/{id}/status")
    public OrderDto updateOrderStatus(@PathVariable Long id, @RequestBody String status) {
        return orderService.updateOrderStatus(id, status);
    }

}
