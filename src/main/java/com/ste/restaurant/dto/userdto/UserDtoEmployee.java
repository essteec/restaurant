package com.ste.restaurant.dto.userdto;

import com.ste.restaurant.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDtoEmployee {
    private String firstName;
    private String lastName;
    private UserRole role;
    private String email;
    private LocalDate birthday;
    private Integer loyaltyPoints;
    private BigDecimal salary;
}
