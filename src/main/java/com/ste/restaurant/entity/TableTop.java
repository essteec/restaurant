package com.ste.restaurant.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "tables")
public class TableTop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_id")
    private Long tableId;

    private String tableNumber;

    @Enumerated(EnumType.STRING)
    private TableStatus tableStatus = TableStatus.AVAILABLE;
}
