package com.ste.restaurant.dto.userdto;

import com.ste.restaurant.dto.AddressDto;
import com.ste.restaurant.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDtoCustomer {
    private String firstName;
    private String lastName;
    private UserRole role;
    private String email;
    private LocalDate birthday;
    private Integer loyaltyPoints;
    private List<AddressDto> addresses;
}
