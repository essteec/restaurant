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

    private String type;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
