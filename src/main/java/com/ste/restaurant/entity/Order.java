package com.ste.restaurant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_time")
    private LocalDateTime orderTime;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.valueOf("PLACED");

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    private String notes;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    @OneToMany(mappedBy = "order")
    private List<OrderItem> orderItems;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private TableTop table;
}