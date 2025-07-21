package com.ste.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {
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
