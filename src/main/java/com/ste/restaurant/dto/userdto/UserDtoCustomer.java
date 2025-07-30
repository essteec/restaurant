package com.ste.restaurant.dto.userdto;

import com.ste.restaurant.dto.AddressDto;
import com.ste.restaurant.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDtoCustomer {
    private String firstName;
    private String lastName;
    private String email;
    private Date birthday;
    private Integer loyaltyPoints;
    private List<AddressDto> addresses;
}
