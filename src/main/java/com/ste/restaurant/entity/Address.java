package com.ste.restaurant.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue
    @Column(name = "address_id")
    private Long addressId;

    private String name;
    private String country;
    private String city;
    private String province;
    private String subprovince;
    private String district;
    private String street;
    private String apartment;
    private String description;
}
