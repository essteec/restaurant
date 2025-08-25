package com.ste.restaurant.service;

import com.ste.restaurant.dto.dashboard.*;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for AdminDashboardService.
 * Tests complex dashboard queries, aggregations, and analytics
 * with real database operations and data analysis.
 * <p>
 * This verifies:
 * - Complex aggregation queries with real data
 * - Multi-entity data analysis and calculations
 * - Performance analytics and time-based filtering
 * - Revenue calculations and ranking algorithms
 * - Real query performance with database constraints
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("AdminDashboardService Integration Tests")
public class AdminDashboardServiceIntegrationTest {

    @Autowired
    private AdminDashboardService adminDashboardService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private FoodItemRepository foodItemRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TableTopRepository tableTopRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;

    private User testCustomer1;
    private User testCustomer2;
    private TableTop testTable1;
    private TableTop testTable2;
    private FoodItem testFoodItem1;
    private FoodItem testFoodItem2;
    private Category testCategory1;
    private Category testCategory2;
    private LocalDate testStartDate;
    private LocalDate testEndDate;

    @BeforeEach
    void setUp() {
        // Set up test date range
        testStartDate = LocalDate.now().minusDays(7);
        testEndDate = LocalDate.now();
        
        // Create test data
        setupTestCategories();
        setupTestCustomers();
        setupTestTables();
        setupTestFoodItems();
        setupTestOrders();
    }

    @Nested
    @DisplayName("Dashboard Stats Integration Tests")
    class DashboardStatsIntegrationTests {

        @Test
        @DisplayName("Should calculate accurate dashboard statistics with real data")
        void shouldCalculateAccurateDashboardStatisticsWithRealData() {
            // When
            DashboardStatsDto stats = adminDashboardService.getDashboardStats(testStartDate, testEndDate);
            
            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.getTotalRevenue()).isGreaterThan(BigDecimal.ZERO);
            assertThat(stats.getTotalOrders()).isGreaterThan(0);
            assertThat(stats.getAverageOrderValue()).isGreaterThan(BigDecimal.ZERO);
            assertThat(stats.getNewCustomers()).isGreaterThanOrEqualTo(0);
            
            // Verify calculations are consistent
            BigDecimal calculatedAverage = stats.getTotalRevenue()
                    .divide(BigDecimal.valueOf(stats.getTotalOrders()), 2, java.math.RoundingMode.HALF_UP);
            assertThat(stats.getAverageOrderValue()).isEqualTo(calculatedAverage);
        }

        @Test
        @DisplayName("Should handle empty date range gracefully")
        void shouldHandleEmptyDateRangeGracefully() {
            // Given - Date range with no orders
            LocalDate futureStart = LocalDate.now().plusDays(10);
            LocalDate futureEnd = LocalDate.now().plusDays(15);
            
            // When
            DashboardStatsDto stats = adminDashboardService.getDashboardStats(futureStart, futureEnd);
            
            // Then
            assertThat(stats.getTotalRevenue()).isEqualTo(BigDecimal.ZERO);
            assertThat(stats.getTotalOrders()).isZero();
            assertThat(stats.getAverageOrderValue()).isEqualTo(BigDecimal.ZERO);
            assertThat(stats.getNewCustomers()).isZero();
        }

        @Test
        @DisplayName("Should correctly identify new customers within date range")
        void shouldCorrectlyIdentifyNewCustomersWithinDateRange() {
            // Given - Create a.java new customer with first order in a.java test period
            String timestamp = String.valueOf(System.currentTimeMillis());
            User newCustomer = createTestCustomer("new" + timestamp + "@customer.com", "New", "Customer");
            Order newCustomerOrder = createTestOrder(newCustomer, testTable1, LocalDateTime.now().minusDays(2));
            newCustomerOrder.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(newCustomerOrder);
            
            // When
            DashboardStatsDto stats = adminDashboardService.getDashboardStats(testStartDate, testEndDate);
            
            // Then
            assertThat(stats.getNewCustomers()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Revenue Chart Integration Tests")
    class RevenueChartIntegrationTests {

        @Test
        @DisplayName("Should generate revenue chart data with proper time grouping")
        void shouldGenerateRevenueChartDataWithProperTimeGrouping() {
            // When
            List<RevenueDataPointDto> chartData = adminDashboardService.getRevenueChart(testStartDate, testEndDate);
            
            // Then
            assertThat(chartData).isNotEmpty();
            
            // Verify data points are in chronological order
            for (int i = 1; i < chartData.size(); i++) {
                String previousLabel = chartData.get(i - 1).getLabel();
                String currentLabel = chartData.get(i).getLabel();
                assertThat(currentLabel.compareTo(previousLabel)).isGreaterThanOrEqualTo(0);
            }
            
            // Verify all revenues are non-negative
            assertThat(chartData).allMatch(point -> point.getRevenue().compareTo(BigDecimal.ZERO) >= 0);
        }

        @Test
        @DisplayName("Should use hourly grouping for short date ranges")
        void shouldUseHourlyGroupingForShortDateRanges() {
            // Given - Short date range (2 days)
            LocalDate shortStart = LocalDate.now().minusDays(1);
            LocalDate shortEnd = LocalDate.now();
            
            // When
            List<RevenueDataPointDto> chartData = adminDashboardService.getRevenueChart(shortStart, shortEnd);
            
            // Then - Should use an hourly format (includes hour)
            if (!chartData.isEmpty()) {
                assertThat(chartData.get(0).getLabel()).contains(" ");
                assertThat(chartData.get(0).getLabel()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:00");
            }
        }

        @Test
        @DisplayName("Should use daily grouping for long date ranges")
        void shouldUseDailyGroupingForLongDateRanges() {
            // Given - Long date range (30 days)
            LocalDate longStart = LocalDate.now().minusDays(30);
            LocalDate longEnd = LocalDate.now();
            
            // When
            List<RevenueDataPointDto> chartData = adminDashboardService.getRevenueChart(longStart, longEnd);

            if (!chartData.isEmpty()) {
                assertThat(chartData.get(0).getLabel()).matches("\\d{4}-\\d{2}-\\d{2}");
                assertThat(chartData.get(0).getLabel()).doesNotContain(" ");
            }
        }
    }

    @Nested
    @DisplayName("Top Performing Items Integration Tests")
    class TopPerformingItemsIntegrationTests {

        @Test
        @DisplayName("Should rank items by revenue correctly with real aggregation")
        void shouldRankItemsByRevenueCorrectlyWithRealAggregation() {
            // When
            Pageable pageable = PageRequest.of(0, 10);
            Page<TopPerformingItemDto> topItems = adminDashboardService.getTopPerformingItems(testStartDate, testEndDate, pageable);
            
            // Then
            assertThat(topItems.getContent()).isNotEmpty();
            
            // Verify items are sorted by revenue (descending)
            List<TopPerformingItemDto> items = topItems.getContent();
            for (int i = 1; i < items.size(); i++) {
                BigDecimal previousRevenue = items.get(i - 1).getTotalRevenue();
                BigDecimal currentRevenue = items.get(i).getTotalRevenue();
                assertThat(previousRevenue.compareTo(currentRevenue)).isGreaterThanOrEqualTo(0);
            }
            
            // Verify quantities and revenues are consistent
            for (TopPerformingItemDto item : items) {
                assertThat(item.getQuantitySold()).isGreaterThan(0);
                assertThat(item.getTotalRevenue()).isGreaterThan(BigDecimal.ZERO);
            }
        }

        @Test
        @DisplayName("Should handle pagination correctly for top items")
        void shouldHandlePaginationCorrectlyForTopItems() {
            // When
            Pageable firstPage = PageRequest.of(0, 2);
            Pageable secondPage = PageRequest.of(1, 2);
            
            Page<TopPerformingItemDto> page1 = adminDashboardService.getTopPerformingItems(testStartDate, testEndDate, firstPage);
            Page<TopPerformingItemDto> page2 = adminDashboardService.getTopPerformingItems(testStartDate, testEndDate, secondPage);
            
            // Then
            assertThat(page1.getNumber()).isZero();
            assertThat(page2.getNumber()).isEqualTo(1);
            
            if (page1.hasContent() && page2.hasContent()) {
                // Verify no overlap between pages
                Set<String> page1Items = Set.of(page1.getContent().stream()
                        .map(TopPerformingItemDto::getFoodName).toArray(String[]::new));
                Set<String> page2Items = Set.of(page2.getContent().stream()
                        .map(TopPerformingItemDto::getFoodName).toArray(String[]::new));
                
                assertThat(page1Items).doesNotContainAnyElementsOf(page2Items);
            }
        }
    }

    @Nested
    @DisplayName("Top Performing Categories Integration Tests")
    class TopPerformingCategoriesIntegrationTests {

        @Test
        @DisplayName("Should calculate category revenue with proper distribution")
        void shouldCalculateCategoryRevenueWithProperDistribution() {
            // When
            Pageable pageable = PageRequest.of(0, 10);
            Page<TopPerformingCategoryDto> topCategories = adminDashboardService.getTopPerformingCategories(testStartDate, testEndDate, pageable);
            
            // Then
            assertThat(topCategories.getContent()).isNotEmpty();
            
            // Verify categories are sorted by revenue (descending)
            List<TopPerformingCategoryDto> categories = topCategories.getContent();
            for (int i = 1; i < categories.size(); i++) {
                BigDecimal previousRevenue = categories.get(i - 1).getTotalRevenue();
                BigDecimal currentRevenue = categories.get(i).getTotalRevenue();
                assertThat(previousRevenue.compareTo(currentRevenue)).isGreaterThanOrEqualTo(0);
            }
            
            // Verify all revenues are positive
            assertThat(categories).allMatch(category -> category.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should handle uncategorized items correctly")
        void shouldHandleUncategorizedItemsCorrectly() {
            // Given - Create food item without categories
            String timestamp = String.valueOf(System.currentTimeMillis());
            FoodItem uncategorizedItem = createTestFoodItem("Uncategorized Item " + timestamp, BigDecimal.valueOf(10.00));
            uncategorizedItem.getCategories().clear(); // Remove all categories
            foodItemRepository.save(uncategorizedItem);
            
            Order uncategorizedOrder = createTestOrder(testCustomer1, testTable1, LocalDateTime.now().minusDays(1));
            uncategorizedOrder.setStatus(OrderStatus.COMPLETED);
            OrderItem orderItem = createTestOrderItem(uncategorizedOrder, uncategorizedItem, 1);
            uncategorizedOrder.getOrderItems().add(orderItem);
            orderRepository.save(uncategorizedOrder);
            
            // When
            Pageable pageable = PageRequest.of(0, 10);
            Page<TopPerformingCategoryDto> topCategories = adminDashboardService.getTopPerformingCategories(testStartDate, testEndDate, pageable);
            
            // Then
            List<String> categoryNames = topCategories.getContent().stream()
                    .map(TopPerformingCategoryDto::getCategoryName)
                    .toList();
            assertThat(categoryNames).contains("Uncategorized");
        }
    }

    @Nested
    @DisplayName("Busiest Tables Integration Tests")
    class BusiestTablesIntegrationTests {

        @Test
        @DisplayName("Should rank tables by order count accurately")
        void shouldRankTablesByOrderCountAccurately() {
            // When
            Pageable pageable = PageRequest.of(0, 10);
            Page<BusiestTableDto> busiestTables = adminDashboardService.getBusiestTables(testStartDate, testEndDate, pageable);
            
            // Then
            assertThat(busiestTables.getContent()).isNotEmpty();
            
            // Verify tables are sorted by order count (descending)
            List<BusiestTableDto> tables = busiestTables.getContent();
            for (int i = 1; i < tables.size(); i++) {
                Long previousCount = tables.get(i - 1).getOrderCount();
                Long currentCount = tables.get(i).getOrderCount();
                assertThat(previousCount).isGreaterThanOrEqualTo(currentCount);
            }
            
            // Verify all counts are positive
            assertThat(tables).allMatch(table -> table.getOrderCount() > 0);
        }
    }

    @Nested
    @DisplayName("Revenue Heatmap Integration Tests")
    class RevenueHeatmapIntegrationTests {

        @Test
        @DisplayName("Should generate heatmap data with proper day and hour distribution")
        void shouldGenerateHeatmapDataWithProperDayAndHourDistribution() {
            // When
            List<RevenueHeatmapPointDto> heatmapData = adminDashboardService.getRevenueHeatmap(testStartDate, testEndDate);
            
            // Then
            assertThat(heatmapData).isNotEmpty();
            
            // Verify proper sorting (Monday-Sunday, then by hour)
            List<String> expectedWeekOrder = List.of(
                    "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
            
            for (RevenueHeatmapPointDto point : heatmapData) {
                assertThat(expectedWeekOrder).contains(point.getDayOfWeek());
                assertThat(point.getHourOfDay()).isBetween(0, 23);
                assertThat(point.getRevenue()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            }
            
            // Verify chronological sorting within the data
            for (int i = 1; i < heatmapData.size(); i++) {
                RevenueHeatmapPointDto prev = heatmapData.get(i - 1);
                RevenueHeatmapPointDto curr = heatmapData.get(i);
                
                int prevDayIndex = expectedWeekOrder.indexOf(prev.getDayOfWeek());
                int currDayIndex = expectedWeekOrder.indexOf(curr.getDayOfWeek());
                
                if (prevDayIndex == currDayIndex) {
                    // The Same day, hour should be greater or equal
                    assertThat(curr.getHourOfDay()).isGreaterThanOrEqualTo(prev.getHourOfDay());
                } else {
                    // Different day, day index should be greater
                    assertThat(currDayIndex).isGreaterThanOrEqualTo(prevDayIndex);
                }
            }
        }
    }

    // Helper methods for creating test data
    private void setupTestCategories() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        testCategory1 = createTestCategory("Main Course " + timestamp);
        testCategory2 = createTestCategory("Appetizer " + timestamp);
    }

    private void setupTestCustomers() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        testCustomer1 = createTestCustomer("test1" + timestamp + "@customer.com", "Customer", "One");
        testCustomer2 = createTestCustomer("test2" + timestamp + "@customer.com", "Customer", "Two");
    }

    private void setupTestTables() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        testTable1 = createTestTable("T001-" + timestamp);
        testTable2 = createTestTable("T002-" + timestamp);
    }

    private void setupTestFoodItems() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        testFoodItem1 = createTestFoodItem("Burger " + timestamp, BigDecimal.valueOf(15.99));
        testFoodItem1.getCategories().add(testCategory1);
        foodItemRepository.save(testFoodItem1);
        
        testFoodItem2 = createTestFoodItem("Salad " + timestamp, BigDecimal.valueOf(8.99));
        testFoodItem2.getCategories().add(testCategory2);
        foodItemRepository.save(testFoodItem2);
    }

    private void setupTestOrders() {
        // Create multiple orders across different dates and times
        for (int i = 0; i < 5; i++) {
            LocalDateTime orderTime = LocalDateTime.now().minusDays(i).withHour(12 + (i % 6)).withMinute(0);
            
            Order order1 = createTestOrder(testCustomer1, testTable1, orderTime);
            OrderItem item1 = createTestOrderItem(order1, testFoodItem1, 1 + i);
            order1.getOrderItems().add(item1);
            order1.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order1);
            
            Order order2 = createTestOrder(testCustomer2, testTable2, orderTime.plusHours(2));
            OrderItem item2 = createTestOrderItem(order2, testFoodItem2, 2);
            order2.getOrderItems().add(item2);
            order2.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order2);
        }
    }

    private Category createTestCategory(String name) {
        Category category = new Category();
        category.setCategoryName(name);
        return categoryRepository.save(category);
    }

    private User createTestCustomer(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("password");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(UserRole.CUSTOMER);
        return userRepository.save(user);
    }

    private TableTop createTestTable(String tableNumber) {
        TableTop table = new TableTop();
        table.setTableNumber(tableNumber);
        table.setCapacity(4);
        table.setTableStatus(TableStatus.AVAILABLE);
        return tableTopRepository.save(table);
    }

    private FoodItem createTestFoodItem(String name, BigDecimal price) {
        FoodItem foodItem = new FoodItem();
        foodItem.setFoodName(name);
        foodItem.setDescription("Test " + name);
        foodItem.setPrice(price);
        return foodItemRepository.save(foodItem);
    }

    private Order createTestOrder(User customer, TableTop table, LocalDateTime orderTime) {
        Order order = new Order();
        order.setOrderTime(orderTime);
        order.setStatus(OrderStatus.COMPLETED);
        order.setTotalPrice(BigDecimal.ZERO);
        order.setNotes("Test order");
        order.setCustomer(customer);
        order.setTable(table);
        return order;
    }

    private OrderItem createTestOrderItem(Order order, FoodItem foodItem, int quantity) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setFoodItem(foodItem);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(foodItem.getPrice());
        orderItem.setTotalPrice(foodItem.getPrice().multiply(BigDecimal.valueOf(quantity)));
        
        // Update order total price
        order.setTotalPrice(order.getTotalPrice().add(orderItem.getTotalPrice()));
        
        return orderItem;
    }
}
