package com.ste.restaurant.controller;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class AdminDashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class DashboardStatsTests {

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldGetDashboardStatsSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/stats")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRevenue").exists())
                    .andExpect(jsonPath("$.totalOrders").exists())
                    .andExpect(jsonPath("$.averageOrderValue").exists())
                    .andExpect(jsonPath("$.newCustomers").exists());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldUseDefaultDatesWhenStartDateMissing() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/stats")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRevenue").exists())
                    .andExpect(jsonPath("$.totalOrders").exists())
                    .andExpect(jsonPath("$.averageOrderValue").exists())
                    .andExpect(jsonPath("$.newCustomers").exists());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldUseDefaultDatesWhenEndDateMissing() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/stats")
                            .param("startDate", "2025-01-01")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRevenue").exists())
                    .andExpect(jsonPath("$.totalOrders").exists())
                    .andExpect(jsonPath("$.averageOrderValue").exists())
                    .andExpect(jsonPath("$.newCustomers").exists());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldUseDefaultDatesWhenBothDatesMissing() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/stats")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRevenue").exists())
                    .andExpect(jsonPath("$.totalOrders").exists())
                    .andExpect(jsonPath("$.averageOrderValue").exists())
                    .andExpect(jsonPath("$.newCustomers").exists());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldValidateDateFormat() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/stats")
                            .param("startDate", "invalid-date")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "WAITER")
        void shouldReturn403WhenWaiterTriesToAccessStats() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/stats")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturn403WhenCustomerTriesToAccessStats() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/stats")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        void shouldReturn403WhenChefTriesToAccessStats() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/stats")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class RevenueChartTests {

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldGetRevenueChartSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/revenue-chart")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldWorkWithoutParameters() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/revenue-chart")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(roles = "WAITER")
        void shouldReturn403WhenWaiterTriesToAccessRevenueChart() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/revenue-chart")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturn403WhenCustomerTriesToAccessRevenueChart() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/revenue-chart")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class TopPerformingItemsTests {

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldGetTopPerformingItemsSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/top-items")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.pageable").exists())
                    .andExpect(jsonPath("$.size").exists());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldWorkWithDefaultDates() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/top-items")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.pageable").exists())
                    .andExpect(jsonPath("$.size").exists());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldSupportPaginationParameters() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/top-items")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .param("page", "0")
                            .param("size", "5")
                            .param("sort", "totalRevenue,desc")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.pageable.pageSize").value(5));
        }

        @Test
        @WithMockUser(roles = "WAITER")
        void shouldReturn403WhenWaiterTriesToAccessTopItems() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/top-items")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturn403WhenCustomerTriesToAccessTopItems() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/top-items")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class TopPerformingCategoriesTests {

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldGetTopPerformingCategoriesSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/top-categories")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.pageable").exists());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldSupportCustomSorting() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/top-categories")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .param("sort", "totalRevenue,asc")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        void shouldReturn403WhenChefTriesToAccessTopCategories() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/top-categories")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class BusiestTablesTests {

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldGetBusiestTablesSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/busiest-tables")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.pageable").exists());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldSupportDefaultSortingByOrderCount() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/busiest-tables")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sort.sorted").value(true));
        }

        @Test
        @WithMockUser(roles = "WAITER")
        void shouldReturn403WhenWaiterTriesToAccessBusiestTables() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/busiest-tables")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class RevenueHeatmapTests {

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldGetRevenueHeatmapSuccessfully() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/revenue-heatmap")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldHandleSameDateRange() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/revenue-heatmap")
                            .param("startDate", "2025-08-07")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturn403WhenCustomerTriesToAccessRevenueHeatmap() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/revenue-heatmap")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CHEF")
        void shouldReturn403WhenChefTriesToAccessRevenueHeatmap() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/revenue-heatmap")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
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
            mockMvc.perform(get("/rest/api/admin/dashboard/stats")
                            .param("startDate", "2025-01-01")
                            .param("endDate", "2025-08-07")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturn403WhenAccessingAnyEndpointWithoutAuth() throws Exception {
            String[] endpoints = {
                "/rest/api/admin/dashboard/stats",
                "/rest/api/admin/dashboard/revenue-chart",
                "/rest/api/admin/dashboard/top-items",
                "/rest/api/admin/dashboard/top-categories",
                "/rest/api/admin/dashboard/busiest-tables",
                "/rest/api/admin/dashboard/revenue-heatmap"
            };

            for (String endpoint : endpoints) {
                mockMvc.perform(get(endpoint)
                                .param("startDate", "2025-01-01")
                                .param("endDate", "2025-08-07")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden());
            }
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @Transactional
    class DateRangeValidationTests {

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldAcceptValidFutureDateRange() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/stats")
                            .param("startDate", "2025-08-01")
                            .param("endDate", "2025-12-31")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldAcceptValidPastDateRange() throws Exception {
            mockMvc.perform(get("/rest/api/admin/dashboard/revenue-chart")
                            .param("startDate", "2024-01-01")
                            .param("endDate", "2024-12-31")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldRejectInvalidDateFormats() throws Exception {
            String[] invalidDates = {"2025/08/07", "08-07-2025", "2025.08.07", "Aug 7, 2025"};
            
            for (String invalidDate : invalidDates) {
                mockMvc.perform(get("/rest/api/admin/dashboard/stats")
                                .param("startDate", invalidDate)
                                .param("endDate", "2025-08-07")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest());
            }
        }

        @Test
        @WithMockUser(username = "somer@restaurant.com", roles = "ADMIN")
        void shouldWorkWithoutAnyParameters() throws Exception {
            String[] endpoints = {
                "/rest/api/admin/dashboard/stats",
                "/rest/api/admin/dashboard/revenue-chart",
                "/rest/api/admin/dashboard/top-items",
                "/rest/api/admin/dashboard/top-categories",
                "/rest/api/admin/dashboard/busiest-tables",
                "/rest/api/admin/dashboard/revenue-heatmap"
            };

            for (String endpoint : endpoints) {
                mockMvc.perform(get(endpoint)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());
            }
        }
    }
}
