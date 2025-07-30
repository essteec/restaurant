package com.ste.restaurant.repository;

import com.ste.restaurant.dto.OrderDto;
import com.ste.restaurant.entity.Address;
import com.ste.restaurant.entity.Order;
import com.ste.restaurant.entity.OrderStatus;
import com.ste.restaurant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {

    Order findFirstByCustomerEmailOrderByOrderTimeDesc(String email);

    List<Order> findAllByCustomerEmailOrderByOrderTimeDesc(String email);

    List<Order> findByCustomer(User customer);

    Order findTopByCustomerAndStatusNotAndStatusNotOrderByOrderTimeDesc(User customer, OrderStatus status, OrderStatus status1);

    List<Order> findAllByStatus(OrderStatus status);

    List<Order> findAllByCustomerAndAddressAndStatusAndOrderTimeBetween(User customer, Address address, OrderStatus status, LocalDateTime orderTimeAfter, LocalDateTime orderTimeBefore);
}
