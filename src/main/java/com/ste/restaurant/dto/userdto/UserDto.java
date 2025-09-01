package com.ste.restaurant.dto.userdto;

import com.ste.restaurant.dto.AddressDto;
import com.ste.restaurant.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private UserRole role;
    private String email;
    private LocalDate birthday;
    private Integer loyaltyPoints;
    private BigDecimal salary;
    private List<AddressDto> addresses;
}
