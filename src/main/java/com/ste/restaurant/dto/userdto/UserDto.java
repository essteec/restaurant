package com.ste.restaurant.dto.userdto;

import com.ste.restaurant.entity.Address;
import com.ste.restaurant.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private String firstName;
    private String lastName;
    private UserRole Role;
    private String email;
    private Date birthday;
    private Integer loyaltyPoints;
    private BigDecimal salary;
    private LocalDateTime lastLogin;
    private List<Address> addressList;
}
