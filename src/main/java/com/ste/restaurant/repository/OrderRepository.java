package com.ste.restaurant.repository;

import com.ste.restaurant.dto.OrderDto;
import com.ste.restaurant.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {

    Order findFirstByCustomerEmailOrderByOrderTimeDesc(String email);

    List<Order> findAllByCustomerEmailOrderByOrderTimeDesc(String email);
}
