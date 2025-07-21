package com.ste.restaurant.dto.userdto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDtoBasic {
    private String firstName;
    private String lastName;
}
