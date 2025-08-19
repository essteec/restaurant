package com.ste.restaurant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ste.restaurant.dto.common.BigDecimalDto;
import com.ste.restaurant.dto.common.StringDto;
import com.ste.restaurant.dto.userdto.PasswordChangeDto;
import com.ste.restaurant.dto.userdto.UserDtoIO;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class CreateUserTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldCreateUserSuccessfully() throws Exception {
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("John");
            userDto.setLastName("Doe");
            userDto.setEmail("john.doe.test" + System.currentTimeMillis() + "@example.com");
            userDto.setPassword("TestPass123");
            userDto.setBirthday(LocalDate.of(1990, 1, 1));

            mockMvc.perform(post("/rest/api/users/")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.email").value(userDto.getEmail()))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenNonAdminTriesToCreateUser() throws Exception {
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("John");
            userDto.setLastName("Doe");
            userDto.setEmail("john.doe@example.com");
            userDto.setPassword("TestPass123");

            mockMvc.perform(post("/rest/api/users/")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturn403WhenUnauthenticatedUserTriesToCreateUser() throws Exception {
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("John");
            userDto.setLastName("Doe");
            userDto.setEmail("john.doe@example.com");
            userDto.setPassword("TestPass123");

            mockMvc.perform(post("/rest/api/users/")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn400WhenCreatingUserWithInvalidData() throws Exception {
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName(""); // Invalid: blank
            userDto.setEmail("invalid-email"); // Invalid: not proper email format
            userDto.setPassword("123"); // Invalid: too short and no uppercase/lowercase

            mockMvc.perform(post("/rest/api/users/")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class GetAllUsersTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGetAllUsersSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/users/")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.pageable").exists())
                    .andExpect(jsonPath("$.totalElements").exists());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGetUsersByRoleSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/users/")
                            .param("role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenNonAdminTriesToGetAllUsers() throws Exception {
            mockMvc.perform(get("/rest/api/users/")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturn403WhenUnauthenticatedUserTriesToGetAllUsers() throws Exception {
            mockMvc.perform(get("/rest/api/users/")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class GetUserByIdTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGetUserByIdSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/users/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("somer@restaurant.com"))
                    .andExpect(jsonPath("$.firstName").value("Admin"));
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenNonAdminTriesToGetUserById() throws Exception {
            mockMvc.perform(get("/rest/api/users/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class DeleteUserTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldDeleteUserSuccessfully() throws Exception {
            // Test deleting an existing user from the seed data (User 10 = Elif Kara)
            mockMvc.perform(delete("/rest/api/users/10")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("elif@gmail.com"));
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenNonAdminTriesToDeleteUser() throws Exception {
            mockMvc.perform(delete("/rest/api/users/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class UpdateUserTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldUpdateUserSuccessfully() throws Exception {
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("Updated");
            userDto.setLastName("User");
            userDto.setEmail("updated.user" + System.currentTimeMillis() + "@example.com");
            userDto.setPassword("UpdatedPass123");

            mockMvc.perform(put("/rest/api/users/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Updated"))
                    .andExpect(jsonPath("$.lastName").value("User"));
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenNonAdminTriesToUpdateUser() throws Exception {
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("Updated");
            userDto.setLastName("User");
            userDto.setEmail("updated.user@example.com");
            userDto.setPassword("UpdatedPass123");

            mockMvc.perform(put("/rest/api/users/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldUpdateUserRoleSuccessfully() throws Exception {
            StringDto roleDto = new StringDto();
            roleDto.setName("WAITER");

            mockMvc.perform(patch("/rest/api/users/1/role")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(roleDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("WAITER"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldUpdateEmployeeSalarySuccessfully() throws Exception {
            BigDecimalDto salaryDto = new BigDecimalDto();
            salaryDto.setDecimal(new BigDecimal("5000.00"));

            // Update salary for a WAITER (ID 3 = bulent@restaurant.com)
            mockMvc.perform(patch("/rest/api/users/3/salary")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(salaryDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.salary").value(5000.00));
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class UserAddressTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldGetUserAddressesSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/users/1/addresses")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenNonAdminTriesToGetUserAddresses() throws Exception {
            mockMvc.perform(get("/rest/api/users/1/addresses")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class CustomerProfileTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldGetCustomerProfileSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/users/profile")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("rick@gmail.com"));
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldUpdateCustomerProfileSuccessfully() throws Exception {
            UserDtoIO userDto = new UserDtoIO();
            userDto.setFirstName("Updated");
            userDto.setLastName("Customer");
            userDto.setEmail("updated.customer" + System.currentTimeMillis() + "@example.com");
            userDto.setPassword("UpdatedPass123");

            mockMvc.perform(put("/rest/api/users/profile")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Updated"))
                    .andExpect(jsonPath("$.lastName").value("Customer"));
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldDeleteCustomerProfileSuccessfully() throws Exception {
            mockMvc.perform(delete("/rest/api/users/profile")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("rick@gmail.com"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn403WhenAdminTriesToAccessCustomerProfile() throws Exception {
            mockMvc.perform(get("/rest/api/users/profile")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class EmployeeProfileTests {

        @Test
        @WithMockUser(username = "bulent@restaurant.com", roles = "WAITER")
        void shouldGetWaiterProfileSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/users/employee/profile")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("bulent@restaurant.com"));
        }

        @Test
        @WithMockUser(username = "rachel@hotmail.com", roles = "CHEF")
        void shouldGetChefProfileSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/users/employee/profile")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("rachel@hotmail.com"));
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldGetAdminProfileSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/users/employee/profile")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("somer@restaurant.com"));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturn403WhenCustomerTriesToAccessEmployeeProfile() throws Exception {
            mockMvc.perform(get("/rest/api/users/employee/profile")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class PasswordChangeTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldChangePasswordSuccessfully() throws Exception {
            PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
            passwordChangeDto.setPassword("Rd1234");
            passwordChangeDto.setNewPassword("NewPass123");

            mockMvc.perform(post("/rest/api/users/me/password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordChangeDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("rick@gmail.com"));
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldChangePasswordForAnyAuthenticatedUser() throws Exception {
            PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
            passwordChangeDto.setPassword("As1234");
            passwordChangeDto.setNewPassword("NewPass123");

            mockMvc.perform(post("/rest/api/users/me/password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordChangeDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("somer@restaurant.com"));
        }

        @Test
        void shouldReturn403WhenUnauthenticatedUserTriesToChangePassword() throws Exception {
            PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
            passwordChangeDto.setPassword("OldPass123");
            passwordChangeDto.setNewPassword("NewPass123");

            mockMvc.perform(post("/rest/api/users/me/password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordChangeDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldReturn400WhenChangingPasswordWithInvalidData() throws Exception {
            PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
            passwordChangeDto.setPassword("123"); // Invalid: too short
            passwordChangeDto.setNewPassword("weak"); // Invalid: no uppercase/digits

            mockMvc.perform(post("/rest/api/users/me/password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordChangeDto)))
                    .andExpect(status().isBadRequest());
        }
    }
}
