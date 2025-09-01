package com.ste.restaurant.entity;

import com.ste.restaurant.entity.enums.TableStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "tables")
public class TableTop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tableId;

    @Column(nullable = false, unique = true)
    private String tableNumber;

    @Column(nullable = false)
    private Integer capacity;

    private String qrCode;

    @Enumerated(EnumType.STRING)
    private TableStatus tableStatus = TableStatus.AVAILABLE;
}
