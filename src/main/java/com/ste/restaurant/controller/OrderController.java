package com.ste.restaurant.controller;

import com.ste.restaurant.dto.OrderDto;
import com.ste.restaurant.dto.OrderItemDto;
import com.ste.restaurant.dto.PlaceOrderDto;
import com.ste.restaurant.dto.common.WarningResponse;
import com.ste.restaurant.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("rest/api/orders")
@RestController
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // save order
    @PostMapping
    public WarningResponse<OrderDto> placeOrder(@Valid @RequestBody PlaceOrderDto placeOrderDto, Authentication auth) {
        return orderService.placeOrder(placeOrderDto, auth.getName());
    }
    // customer cancel if it is not ready. admin and waiter can cancel unlimited
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER', 'CUSTOMER')")
    @PatchMapping(path = "/{id}/cancel")
    public OrderDto cancelOrder(@PathVariable Long id, Authentication auth) {
        return orderService.cancelOrderIfNotReady(id, auth.getName());
    }

    // customer
    @GetMapping
    public List<OrderDto> getOrders(Authentication auth) {
        return orderService.getOrders(auth.getName());
    }

    @GetMapping(path = "/last")
    public OrderDto getLastOrder(Authentication auth) {
        return orderService.getLastOrder(auth.getName());
    }

    @GetMapping(path = "/{id}")
    public OrderDto getOrder(@PathVariable Long id, Authentication auth) {
        return orderService.getOrderById(id, auth.getName());
    }

    @GetMapping(path = "/{id}/items")
    public List<OrderItemDto> getOrderItems(@PathVariable Long id, Authentication auth) {
        return orderService.getOrderItemsForUser(id, auth.getName());
    }
}
