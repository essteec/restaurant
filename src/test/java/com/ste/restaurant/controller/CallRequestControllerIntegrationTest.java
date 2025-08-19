package com.ste.restaurant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ste.restaurant.dto.CallRequestDtoBasic;
import com.ste.restaurant.entity.RequestType;
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
public class CallRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class CreateCallRequestTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldCreateCallRequestSuccessfully() throws Exception {
            CallRequestDtoBasic callRequestDto = new CallRequestDtoBasic();
            callRequestDto.setType(RequestType.WATER);
            callRequestDto.setMessage("Need water for table");

            mockMvc.perform(post("/rest/api/call-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(callRequestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("WATER"))
                    .andExpect(jsonPath("$.message").value("Need water for table"))
                    .andExpect(jsonPath("$.callRequestId").exists())
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldCreateCallRequestWithAllTypes() throws Exception {
            for (RequestType type : RequestType.values()) {
                CallRequestDtoBasic callRequestDto = new CallRequestDtoBasic();
                callRequestDto.setType(type);
                callRequestDto.setMessage("Test " + type.name() + " request");

                mockMvc.perform(post("/rest/api/call-requests")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(callRequestDto)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.type").value(type.name()));
            }
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldCreateCallRequestWithoutMessage() throws Exception {
            CallRequestDtoBasic callRequestDto = new CallRequestDtoBasic();
            callRequestDto.setType(RequestType.ASSISTANCE);
            // message is optional

            mockMvc.perform(post("/rest/api/call-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(callRequestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("ASSISTANCE"));
        }

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldValidateRequiredFields() throws Exception {
            CallRequestDtoBasic callRequestDto = new CallRequestDtoBasic();
            // Missing required type

            mockMvc.perform(post("/rest/api/call-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(callRequestDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn403WhenAdminTriesToCreateCallRequest() throws Exception {
            CallRequestDtoBasic callRequestDto = new CallRequestDtoBasic();
            callRequestDto.setType(RequestType.WATER);

            mockMvc.perform(post("/rest/api/call-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(callRequestDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "WAITER")
        void shouldReturn403WhenWaiterTriesToCreateCallRequest() throws Exception {
            CallRequestDtoBasic callRequestDto = new CallRequestDtoBasic();
            callRequestDto.setType(RequestType.PAYMENT);

            mockMvc.perform(post("/rest/api/call-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(callRequestDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        void shouldReturn403WhenChefTriesToCreateCallRequest() throws Exception {
            CallRequestDtoBasic callRequestDto = new CallRequestDtoBasic();
            callRequestDto.setType(RequestType.NEED);

            mockMvc.perform(post("/rest/api/call-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(callRequestDto)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class GetAllCallRequestsTests {

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldGetAllCallRequestsForAdmin() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.pageable").exists());
        }

        @Test
        @WithMockUser(username = "bulent@restaurant.com", roles = "WAITER")
        void shouldGetActiveCallRequestsForWaiter() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldFilterByType() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests")
                            .param("type", "WATER")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldFilterByActive() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests")
                            .param("active", "true")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldFilterByTypeAndActive() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests")
                            .param("type", "PAYMENT")
                            .param("active", "false")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturn403WhenCustomerTriesToGetAllCallRequests() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        void shouldReturn403WhenChefTriesToGetAllCallRequests() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class GetCallRequestByIdTests {

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldGetCallRequestByIdForAdmin() throws Exception {
            // Test access control with a non-existent ID
            mockMvc.perform(get("/rest/api/call-requests/999999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "WAITER")
        void shouldGetCallRequestByIdForWaiter() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests/999999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturn403WhenCustomerTriesToGetCallRequestById() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        void shouldReturn403WhenChefTriesToGetCallRequestById() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class ResolveCallRequestTests {

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldResolveCallRequestForAdmin() throws Exception {
            mockMvc.perform(patch("/rest/api/call-requests/999999/resolve")
                            .with(csrf()))
                    .andExpect(status().isNotFound()); // Test access, non-existent ID
        }

        @Test
        @WithMockUser(username = "bulent@restaurant.com", roles = "WAITER")
        void shouldResolveCallRequestForWaiter() throws Exception {
            mockMvc.perform(patch("/rest/api/call-requests/999999/resolve")
                            .with(csrf()))
                    .andExpect(status().isNotFound()); // Test access, non-existent ID
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturn403WhenCustomerTriesToResolveCallRequest() throws Exception {
            mockMvc.perform(patch("/rest/api/call-requests/1/resolve")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        void shouldReturn403WhenChefTriesToResolveCallRequest() throws Exception {
            mockMvc.perform(patch("/rest/api/call-requests/1/resolve")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class CustomerResolveCallRequestTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldResolveOwnCallRequestAsCustomer() throws Exception {
            mockMvc.perform(patch("/rest/api/call-requests/999999")
                            .with(csrf()))
                    .andExpect(status().isNotFound()); // Test access, non-existent ID
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn403WhenAdminTriesToResolveAsCustomer() throws Exception {
            mockMvc.perform(patch("/rest/api/call-requests/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "WAITER")
        void shouldReturn403WhenWaiterTriesToResolveAsCustomer() throws Exception {
            mockMvc.perform(patch("/rest/api/call-requests/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        void shouldReturn403WhenChefTriesToResolveAsCustomer() throws Exception {
            mockMvc.perform(patch("/rest/api/call-requests/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class GetLatestCallRequestsTests {

        @Test
        @WithMockUser(username = "rick@gmail.com", roles = "CUSTOMER")
        void shouldGetLatestCallRequestsForCustomer() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests/my/latest")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn403WhenAdminTriesToGetLatestCallRequests() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests/my/latest")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "WAITER")
        void shouldReturn403WhenWaiterTriesToGetLatestCallRequests() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests/my/latest")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        void shouldReturn403WhenChefTriesToGetLatestCallRequests() throws Exception {
            mockMvc.perform(get("/rest/api/call-requests/my/latest")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class DeleteCallRequestTests {

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldDeleteCallRequestForAdmin() throws Exception {
            mockMvc.perform(delete("/rest/api/call-requests/999999")
                            .with(csrf()))
                    .andExpect(status().isNotFound()); // Test access, non-existent ID
        }

        @Test
        @WithMockUser(roles = "WAITER")
        void shouldReturn403WhenWaiterTriesToDeleteCallRequest() throws Exception {
            mockMvc.perform(delete("/rest/api/call-requests/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturn403WhenCustomerTriesToDeleteCallRequest() throws Exception {
            mockMvc.perform(delete("/rest/api/call-requests/1")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        void shouldReturn403WhenChefTriesToDeleteCallRequest() throws Exception {
            mockMvc.perform(delete("/rest/api/call-requests/1")
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
            mockMvc.perform(get("/rest/api/call-requests"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturn403WhenPostingWithoutAuthentication() throws Exception {
            CallRequestDtoBasic callRequestDto = new CallRequestDtoBasic();
            callRequestDto.setType(RequestType.WATER);

            mockMvc.perform(post("/rest/api/call-requests")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(callRequestDto)))
                    .andExpect(status().isForbidden());
        }
    }
}
