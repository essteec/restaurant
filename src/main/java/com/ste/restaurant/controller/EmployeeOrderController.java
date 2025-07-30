package com.ste.restaurant.controller;

import com.ste.restaurant.dto.OrderDto;
import com.ste.restaurant.dto.OrderItemDto;
import com.ste.restaurant.dto.PlaceOrderDto;
import com.ste.restaurant.dto.StringDto;
import com.ste.restaurant.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/api/employee/orders")
public class EmployeeOrderController {
    @Autowired
    private OrderService orderService;

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}")
    public OrderDto deleteOrderById(@PathVariable Long id) {
        return orderService.deleteOrderById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'Waiter', 'CHEF')")
    @GetMapping(path = "/{orderId}/items")
    public List<OrderItemDto> getOrderItemsFromOrder(@PathVariable Long orderId) {
        return orderService.getOrderItemsFromOrder(orderId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/items")
    public List<OrderItemDto> getOrderItemList() {
        return orderService.getOrderItemList();
    }

    // by admin, waiter and chef
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER', 'CHEF')")
    @GetMapping(path = "/{id}")
    public OrderDto getOrderById(@PathVariable Long id, Authentication auth) {
        return orderService.getOrderById(id);
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER', 'CHEF')")
    @PatchMapping(path = "/{id}/status")
    public OrderDto updateOrderStatus(@PathVariable Long id, @Valid @RequestBody StringDto status) {
        return orderService.updateOrderStatus(id, status);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER', 'CHEF')")
    @GetMapping
    public List<OrderDto> getAllOrders(
            @RequestParam(required = false) String status,
            Authentication auth) {
        String role = auth.getAuthorities().iterator().next().getAuthority();
        if (role.equals("ROLE_CHEF")) {
            List<OrderDto> orders = orderService.getAllOrdersBy("PLACED");
            orders.addAll(orderService.getAllOrdersBy("PREPARING"));
            orders.addAll(orderService.getAllOrdersBy("READY"));
            return orders;
        }
        if (status == null) return orderService.getOrderList();
        return orderService.getAllOrdersBy(status);
    }

}
