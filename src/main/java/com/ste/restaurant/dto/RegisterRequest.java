package com.ste.restaurant.dto;

import lombok.Data;

import java.util.Date;

@Data
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Date dateOfBirth;
}
