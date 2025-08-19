package com.ste.restaurant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ste.restaurant.dto.AddressDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AddressControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class CreateAddressTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldCreateAddressSuccessfully() throws Exception {
            AddressDto addressDto = new AddressDto();
            addressDto.setName("New Office"); // Use a unique name to avoid conflicts
            addressDto.setCountry("Turkey");
            addressDto.setCity("Ankara");
            addressDto.setProvince("Central Anatolia");
            addressDto.setSubprovince("Cankaya");
            addressDto.setDistrict("Bahcelievler");
            addressDto.setStreet("Ataturk Bulvari");
            addressDto.setApartment("No: 123");
            addressDto.setDescription("Near the metro station");

            mockMvc.perform(post("/rest/api/addresses")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addressDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New Office"))
                    .andExpect(jsonPath("$.country").value("Turkey"))
                    .andExpect(jsonPath("$.city").value("Ankara"))
                    .andExpect(jsonPath("$.addressId").exists());
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldValidateRequiredFields() throws Exception {
            AddressDto addressDto = new AddressDto();
            // Missing required fields

            mockMvc.perform(post("/rest/api/addresses")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addressDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldValidateBlankFields() throws Exception {
            AddressDto addressDto = new AddressDto();
            addressDto.setName(""); // Blank name
            addressDto.setCountry("");
            addressDto.setCity("");
            addressDto.setProvince("");
            addressDto.setStreet("");
            addressDto.setApartment("");

            mockMvc.perform(post("/rest/api/addresses")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addressDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn403WhenAdminTriesToCreateAddress() throws Exception {
            AddressDto addressDto = new AddressDto();
            addressDto.setName("Test Address");
            addressDto.setCountry("Turkey");
            addressDto.setCity("Istanbul");
            addressDto.setProvince("Marmara");
            addressDto.setStreet("Test Street");
            addressDto.setApartment("1A");

            mockMvc.perform(post("/rest/api/addresses")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addressDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "WAITER")
        void shouldReturn403WhenWaiterTriesToCreateAddress() throws Exception {
            AddressDto addressDto = new AddressDto();
            addressDto.setName("Test Address");
            addressDto.setCountry("Turkey");
            addressDto.setCity("Istanbul");
            addressDto.setProvince("Marmara");
            addressDto.setStreet("Test Street");
            addressDto.setApartment("1A");

            mockMvc.perform(post("/rest/api/addresses")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addressDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        void shouldReturn403WhenChefTriesToCreateAddress() throws Exception {
            AddressDto addressDto = new AddressDto();
            addressDto.setName("Test Address");
            addressDto.setCountry("Turkey");
            addressDto.setCity("Istanbul");
            addressDto.setProvince("Marmara");
            addressDto.setStreet("Test Street");
            addressDto.setApartment("1A");

            mockMvc.perform(post("/rest/api/addresses")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addressDto)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class GetAddressesTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldGetAddressesSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/addresses")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn403WhenAdminTriesToGetAddresses() throws Exception {
            mockMvc.perform(get("/rest/api/addresses")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "WAITER")
        void shouldReturn403WhenWaiterTriesToGetAddresses() throws Exception {
            mockMvc.perform(get("/rest/api/addresses")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        void shouldReturn403WhenChefTriesToGetAddresses() throws Exception {
            mockMvc.perform(get("/rest/api/addresses")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class UpdateAddressTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldUpdateAddressSuccessfully() throws Exception {
            // First create an address
            AddressDto createDto = new AddressDto();
            createDto.setName("Original");
            createDto.setCountry("Turkey");
            createDto.setCity("Istanbul");
            createDto.setProvince("Marmara");
            createDto.setStreet("Original Street");
            createDto.setApartment("1A");

            String createResponse = mockMvc.perform(post("/rest/api/addresses")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            AddressDto createdAddress = objectMapper.readValue(createResponse, AddressDto.class);

            // Update the address
            AddressDto updateDto = new AddressDto();
            updateDto.setName("Updated Address");
            updateDto.setCountry("Turkey");
            updateDto.setCity("Ankara");
            updateDto.setProvince("Central Anatolia");
            updateDto.setStreet("Updated Street");
            updateDto.setApartment("2B");
            updateDto.setDescription("Updated description");

            mockMvc.perform(put("/rest/api/addresses/" + createdAddress.getAddressId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Address"))
                    .andExpect(jsonPath("$.city").value("Ankara"))
                    .andExpect(jsonPath("$.street").value("Updated Street"));
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldValidateUpdateFields() throws Exception {
            AddressDto updateDto = new AddressDto();
            // Missing required fields

            mockMvc.perform(put("/rest/api/addresses/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldReturn404WhenUpdatingNonexistentAddress() throws Exception {
            AddressDto updateDto = new AddressDto();
            updateDto.setName("Test");
            updateDto.setCountry("Turkey");
            updateDto.setCity("Istanbul");
            updateDto.setProvince("Marmara");
            updateDto.setStreet("Test Street");
            updateDto.setApartment("1A");

            mockMvc.perform(put("/rest/api/addresses/999999")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn403WhenAdminTriesToUpdateAddress() throws Exception {
            AddressDto updateDto = new AddressDto();
            updateDto.setName("Test");
            updateDto.setCountry("Turkey");
            updateDto.setCity("Istanbul");
            updateDto.setProvince("Marmara");
            updateDto.setStreet("Test Street");
            updateDto.setApartment("1A");

            mockMvc.perform(put("/rest/api/addresses/1")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class DeleteAddressTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldDeleteAddressSuccessfully() throws Exception {
            // First create an address
            AddressDto createDto = new AddressDto();
            createDto.setName("To Delete");
            createDto.setCountry("Turkey");
            createDto.setCity("Istanbul");
            createDto.setProvince("Marmara");
            createDto.setStreet("Delete Street");
            createDto.setApartment("1A");

            String createResponse = mockMvc.perform(post("/rest/api/addresses")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            AddressDto createdAddress = objectMapper.readValue(createResponse, AddressDto.class);

            // Delete the address
            mockMvc.perform(delete("/rest/api/addresses/" + createdAddress.getAddressId())
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("To Delete"));
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldReturn404WhenDeletingNonexistentAddress() throws Exception {
            mockMvc.perform(delete("/rest/api/addresses/999999")
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn403WhenAdminTriesToDeleteAddress() throws Exception {
            mockMvc.perform(delete("/rest/api/addresses/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "WAITER")
        void shouldReturn403WhenWaiterTriesToDeleteAddress() throws Exception {
            mockMvc.perform(delete("/rest/api/addresses/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        void shouldReturn403WhenChefTriesToDeleteAddress() throws Exception {
            mockMvc.perform(delete("/rest/api/addresses/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class AuthenticationTests {

        @Test
        void shouldReturn403WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/rest/api/addresses"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturn403WhenPostingWithoutAuthentication() throws Exception {
            AddressDto addressDto = new AddressDto();
            addressDto.setName("Test");
            addressDto.setCountry("Turkey");
            addressDto.setCity("Istanbul");
            addressDto.setProvince("Marmara");
            addressDto.setStreet("Test Street");
            addressDto.setApartment("1A");

            mockMvc.perform(post("/rest/api/addresses")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addressDto)))
                    .andExpect(status().isForbidden());
        }
    }
}
