package com.ste.restaurant.controller.impl;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.entity.Menu;
import com.ste.restaurant.repository.MenuRepository;
import com.ste.restaurant.service.impl.AddressService;
import com.ste.restaurant.service.impl.MenuService;
import com.ste.restaurant.service.impl.TableTopService;
import com.ste.restaurant.service.impl.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/rest/api/customer/")
@RestController
@PreAuthorize("hasAnyRole('CUSTOMER', 'VIP_CUSTOMER')")
public class CustomerController {

    @Autowired
    private MenuService menuService;

    @Autowired
    private TableTopService tableTopService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AddressService addressService;

        //      Customer (CustomerController)
        //  Get active menu (getMenu) —          ✔️

        //      Order (OrderController)
        //  Place a new order —                  ✔️
        //  View own orders —                    ✔️
        //  View items in own order —           TODO
        //  Cancel own order —                  TODO

        //      TableTop (TableTopController)
        //  View available tables                ✔️

        //      User (UserController)
        //  Register (saveUser, public) —        ✔️
        //  View own profile —                  TODO
        //  Update own profile —                TODO
        //  Delete own account —                TODO

        //      Address (AddressController)
        //  Add a new address —                 TODO
        //  View own addresses —                TODO
        //  Update own address —                TODO
        //  Delete own address —                TODO


    //  TABLE
    @GetMapping(path = "tables/available")
    public List<TableTopDto> getAvailableTables() {
        return tableTopService.getAvailableTables();
    }

    //  MENU
    @GetMapping(path = "/menus/active")
    public List<MenuDto> getActiveMenu() {
        return menuService.getActiveMenu();
    }

    //  ORDER
    @PostMapping("/save-order")
    public OrderDto placeOrder(@RequestBody PlaceOrderDto placeOrderDto, Authentication authentication) {
        return orderService.placeOrder(placeOrderDto, authentication.getName());
    }

    @GetMapping(path = "/get-order")
    public OrderDto getLastOrder(Authentication authentication) {
        return orderService.getLastOrder(authentication.getName());
    }

    @GetMapping(path = "/get-orders")
    public List<OrderDto> getOrders(Authentication authentication) {
        return orderService.getOrders(authentication.getName());
    }

    //  ADDRESS
    @PostMapping(path = "/save-address")
    public AddressDto saveAddress(@RequestBody AddressDto addressDto, Authentication authentication) {
        if (!authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return addressService.saveAddress(addressDto, authentication.getName());
    }
}
