package com.ste.restaurant.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "call_requests")
public class CallRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long callRequestId;

    @Enumerated(EnumType.STRING)
    private RequestType type;

    private String message;

    private boolean active = true;

    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "table_id")
    private TableTop table;

    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User customer;

    private LocalDateTime createdAt;
}
