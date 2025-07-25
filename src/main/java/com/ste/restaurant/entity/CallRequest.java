package com.ste.restaurant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "call_requests")
public class CallRequest {
    @Id
    @Column(name = "call_request_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long callRequestId;

    @Enumerated(EnumType.STRING)
    private RequestType type;

    private String message;

    @JoinColumn(name = "active")
    private boolean active = true;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private TableTop table;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User customer;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
