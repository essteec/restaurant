package com.ste.restaurant.dto.userdto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDtoIO {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Date birthday;
}
