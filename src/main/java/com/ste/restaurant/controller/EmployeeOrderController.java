package com.ste.restaurant.controller;

import com.ste.restaurant.dto.OrderDto;
import com.ste.restaurant.dto.OrderItemDto;
import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.entity.OrderStatus;
import com.ste.restaurant.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/api/employee/orders")
public class EmployeeOrderController {

    private final OrderService orderService;

    public EmployeeOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}")
    public OrderDto deleteOrderById(@PathVariable Long id) {
        return orderService.deleteOrderById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER', 'CHEF')")
    @GetMapping(path = "/{id}/items")
    public List<OrderItemDto> getOrderItemsFromOrder(@PathVariable Long id) {
        return orderService.getOrderItemsFromOrder(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(path = "/items")
    public Page<OrderItemDto> getOrderItemList(@PageableDefault(size = 20) Pageable pageable) {
        return orderService.getOrderItemList(pageable);
    }

    // by admin, waiter and chef
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER', 'CHEF')")
    @GetMapping(path = "/{id}")
    public OrderDto getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER', 'CHEF')")
    @PatchMapping(path = "/{id}/status")
    public OrderDto updateOrderStatus(@PathVariable Long id, @Valid @RequestBody StringDto status) {
        return orderService.updateOrderStatus(id, status);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER', 'CHEF')")
    @GetMapping
    public Page<OrderDto> getAllOrders(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "orderTime") Pageable pageable,
            Authentication auth) {
        String role = auth.getAuthorities().iterator().next().getAuthority();
        if (role.equals("ROLE_CHEF")) {
            return orderService.getAllOrdersBy(
                    List.of(OrderStatus.PLACED,
                    OrderStatus.PREPARING,
                    OrderStatus.READY), pageable);
        }
        else if (role.equals("ROLE_WAITER")) {
            return orderService.getAllOrdersBy(
                    List.of(OrderStatus.PLACED,
                            OrderStatus.READY,
                            OrderStatus.DELIVERED,
                            OrderStatus.SHIPPED,
                            OrderStatus.COMPLETED,
                            OrderStatus.CANCELLED), pageable);
        }
        if (status == null) return orderService.getOrderList(pageable);
        return orderService.getAllOrdersBy(status, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    @PatchMapping(path = "/{id}/table")
    public OrderDto changeTableOfOrder(@PathVariable Long id, @Valid @RequestBody StringDto tableNumberDto) {
        return orderService.changeTableOfOrder(id, tableNumberDto);
    }
}
