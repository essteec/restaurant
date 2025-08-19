package com.ste.restaurant.repository;

import com.ste.restaurant.dto.OrderDto;
import com.ste.restaurant.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {


    Order findFirstByCustomerEmailOrderByOrderTimeDesc(String email);

    List<Order> findAllByCustomerEmailOrderByOrderTimeDesc(String email);

    List<Order> findByCustomer(User customer);

    List<Order> findAllByStatus(OrderStatus status);

    List<Order> findAllByStatusAndOrderTimeBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);

    Order findFirstByCustomerOrderByOrderTimeAsc(User customer);

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findAllByStatusIn(List<OrderStatus> statuses, Pageable pageable);

    Order findTopByCustomerAndStatusNotInOrderByOrderTimeDesc(User customer, Collection<OrderStatus> statuses);

    List<Order> findAllByCustomerAndTableAndStatusAndOrderTimeBetween(User customer, TableTop table, OrderStatus status, LocalDateTime orderTimeAfter, LocalDateTime orderTimeBefore);

    @Modifying
    @Query("UPDATE Order o Set o.customer = null, o.address = null WHERE o.customer = :customer")
    void updateCustomerAndAddressToNull(@Param("customer") User customer);

    List<Order> findByAddress(Address address);


    Page<Order> findAllByStatusInAndOrderTimeAfter(Collection<OrderStatus> statuses, LocalDateTime orderTimeAfter, Pageable pageable);

    List<Order> findAllByStatusInAndOrderTimeAfter(Collection<OrderStatus> statuses, LocalDateTime orderTimeAfter);

    List<Order> findAllByStatusNotAndOrderTimeAfter(OrderStatus status, LocalDateTime orderTimeAfter);

    List<Order> findAllByStatusInAndOrderTimeAfterOrderByOrderTimeDesc(Collection<OrderStatus> statuses, LocalDateTime orderTimeAfter);

    List<Order> findAllByStatusNotAndOrderTimeAfterOrderByOrderTimeDesc(OrderStatus status, LocalDateTime orderTimeAfter);
}
