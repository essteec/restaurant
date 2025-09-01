package com.ste.restaurant.entity;

import com.ste.restaurant.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String firstName;

    private String lastName;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.CUSTOMER;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    private LocalDate birthday;

    private Integer loyaltyPoints;

    private BigDecimal salary;

    @EqualsAndHashCode.Exclude
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private List<Address> addresses;

    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "customer")
    private List<Order> orders;
}
