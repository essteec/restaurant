package com.ste.restaurant.service;

import com.ste.restaurant.dto.dashboard.*;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.entity.enums.OrderStatus;
import com.ste.restaurant.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    private LocalDate startDate;
    private LocalDate endDate;
    private Pageable pageable;

    private User testCustomer1;
    private User testCustomer2;
    private FoodItem testFoodItem1;
    private FoodItem testFoodItem2;
    private Category testCategory1;
    private Category testCategory2;
    private TableTop testTable1;
    private TableTop testTable2;

    private Order testOrder1;
    private Order testOrder2;
    private Order testOrder3;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.of(2024, 1, 1);
        endDate = LocalDate.of(2024, 1, 31);
        pageable = PageRequest.of(0, 10, Sort.by("totalRevenue").descending());

        testCustomer1 = new User();
        testCustomer1.setUserId(1L);
        testCustomer1.setEmail("customer1@example.com");

        testCustomer2 = new User();
        testCustomer2.setUserId(2L);
        testCustomer2.setEmail("customer2@example.com");

        testFoodItem1 = new FoodItem();
        testFoodItem1.setFoodId(1L);
        testFoodItem1.setFoodName("Pizza");
        testFoodItem1.setPrice(BigDecimal.valueOf(10.00));

        testFoodItem2 = new FoodItem();
        testFoodItem2.setFoodId(2L);
        testFoodItem2.setFoodName("Pasta");
        testFoodItem2.setPrice(BigDecimal.valueOf(15.00));

        testCategory1 = new Category();
        testCategory1.setCategoryId(1L);
        testCategory1.setCategoryName("Italian");
        testCategory1.setFoodItems(new HashSet<>(Collections.singletonList(testFoodItem1)));
        testFoodItem1.setCategories(new HashSet<>(Collections.singletonList(testCategory1)));

        testCategory2 = new Category();
        testCategory2.setCategoryId(2L);
        testCategory2.setCategoryName("Dessert");
        testCategory2.setFoodItems(new HashSet<>(Collections.singletonList(testFoodItem2)));
        testFoodItem2.setCategories(new HashSet<>(Collections.singletonList(testCategory2)));

        testTable1 = new TableTop();
        testTable1.setTableId(1L);
        testTable1.setTableNumber("T1");

        testTable2 = new TableTop();
        testTable2.setTableId(2L);
        testTable2.setTableNumber("T2");

        // Order 1: Completed, Customer 1, Table 1, Pizza (10.00)
        testOrder1 = new Order();
        testOrder1.setOrderId(1L);
        testOrder1.setOrderTime(LocalDateTime.of(2024, 1, 10, 12, 0));
        testOrder1.setStatus(OrderStatus.COMPLETED);
        testOrder1.setTotalPrice(BigDecimal.valueOf(10.00));
        testOrder1.setCustomer(testCustomer1);
        testOrder1.setTable(testTable1);
        OrderItem oi1 = new OrderItem();
        oi1.setFoodItem(testFoodItem1);
        oi1.setQuantity(1);
        oi1.setTotalPrice(BigDecimal.valueOf(10.00));
        testOrder1.setOrderItems(Collections.singletonList(oi1));

        // Order 2: Completed, Customer 1, Table 1, Pasta (15.00)
        testOrder2 = new Order();
        testOrder2.setOrderId(2L);
        testOrder2.setOrderTime(LocalDateTime.of(2024, 1, 10, 13, 0));
        testOrder2.setStatus(OrderStatus.COMPLETED);
        testOrder2.setTotalPrice(BigDecimal.valueOf(15.00));
        testOrder2.setCustomer(testCustomer1);
        testOrder2.setTable(testTable1);
        OrderItem oi2 = new OrderItem();
        oi2.setFoodItem(testFoodItem2);
        oi2.setQuantity(1);
        oi2.setTotalPrice(BigDecimal.valueOf(15.00));
        testOrder2.setOrderItems(Collections.singletonList(oi2));

        // Order 3: Completed, Customer 2, Table 2, Pizza (10.00)
        testOrder3 = new Order();
        testOrder3.setOrderId(3L);
        testOrder3.setOrderTime(LocalDateTime.of(2024, 1, 11, 18, 0));
        testOrder3.setStatus(OrderStatus.COMPLETED);
        testOrder3.setTotalPrice(BigDecimal.valueOf(10.00));
        testOrder3.setCustomer(testCustomer2);
        testOrder3.setTable(testTable2);
        OrderItem oi3 = new OrderItem();
        oi3.setFoodItem(testFoodItem1);
        oi3.setQuantity(1);
        oi3.setTotalPrice(BigDecimal.valueOf(10.00));
        testOrder3.setOrderItems(Collections.singletonList(oi3));
    }

    @Test
    void getDashboardStats_success() {
        // Arrange
        List<Order> completedOrders = Arrays.asList(testOrder1, testOrder2, testOrder3);
        when(orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        )).thenReturn(completedOrders);

        // Mock first order for new customer calculation
        when(orderRepository.findFirstByCustomerOrderByOrderTimeAsc(testCustomer1)).thenReturn(testOrder1);
        when(orderRepository.findFirstByCustomerOrderByOrderTimeAsc(testCustomer2)).thenReturn(testOrder3);

        // Act
        DashboardStatsDto stats = adminDashboardService.getDashboardStats(startDate, endDate);

        // Assert
        assertNotNull(stats);
        assertEquals(0, new BigDecimal("35.00").compareTo(stats.getTotalRevenue())); // Corrected BigDecimal comparison
        assertEquals(3, stats.getTotalOrders());
        assertEquals(0, new BigDecimal("11.67").compareTo(stats.getAverageOrderValue())); // Corrected BigDecimal comparison
        assertEquals(2, stats.getNewCustomers()); // Both customer1 and customer2 are 'new' in this range

        // Verify
        verify(orderRepository, times(1)).findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );
        verify(orderRepository, times(1)).findFirstByCustomerOrderByOrderTimeAsc(testCustomer1);
        verify(orderRepository, times(1)).findFirstByCustomerOrderByOrderTimeAsc(testCustomer2);
    }

    @Test
    void getDashboardStats_noOrders() {
        // Arrange
        when(orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        )).thenReturn(Collections.emptyList());

        // Act
        DashboardStatsDto stats = adminDashboardService.getDashboardStats(startDate, endDate);

        // Assert
        assertNotNull(stats);
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getTotalRevenue())); // Ensure scale for ZERO
        assertEquals(0, stats.getTotalOrders());
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getAverageOrderValue())); // Ensure scale for ZERO
        assertEquals(0, stats.getNewCustomers());

        // Verify
        verify(orderRepository, times(1)).findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );
        verify(orderRepository, never()).findFirstByCustomerOrderByOrderTimeAsc(any(User.class));
    }

    @Test
    void getDashboardStats_newCustomerOutsideDateRange() {
        // Arrange
        User customerWithOldFirstOrder = new User();
        customerWithOldFirstOrder.setUserId(3L);
        customerWithOldFirstOrder.setEmail("oldcustomer@example.com");
        
        // Create an order within range for the old customer
        Order orderInRange = new Order();
        orderInRange.setOrderId(4L);
        orderInRange.setOrderTime(LocalDateTime.of(2024, 1, 15, 12, 0));
        orderInRange.setStatus(OrderStatus.COMPLETED);
        orderInRange.setTotalPrice(BigDecimal.valueOf(20.00));
        orderInRange.setCustomer(customerWithOldFirstOrder);
        
        // Create their first order outside the range (before start date)
        Order firstOrderOutsideRange = new Order();
        firstOrderOutsideRange.setOrderTime(LocalDateTime.of(2023, 12, 15, 12, 0)); // Before 2024-01-01
        
        List<Order> completedOrders = Arrays.asList(testOrder1, orderInRange);
        
        when(orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        )).thenReturn(completedOrders);

        // Mock first order queries
        when(orderRepository.findFirstByCustomerOrderByOrderTimeAsc(testCustomer1)).thenReturn(testOrder1);
        when(orderRepository.findFirstByCustomerOrderByOrderTimeAsc(customerWithOldFirstOrder)).thenReturn(firstOrderOutsideRange);

        // Act
        DashboardStatsDto stats = adminDashboardService.getDashboardStats(startDate, endDate);

        // Assert
        assertNotNull(stats);
        assertEquals(0, new BigDecimal("30.00").compareTo(stats.getTotalRevenue()));
        assertEquals(2, stats.getTotalOrders());
        assertEquals(1, stats.getNewCustomers()); // Only testCustomer1 is new, not the old customer

        // Verify
        verify(orderRepository, times(1)).findFirstByCustomerOrderByOrderTimeAsc(testCustomer1);
        verify(orderRepository, times(1)).findFirstByCustomerOrderByOrderTimeAsc(customerWithOldFirstOrder);
    }

    @Test
    void getRevenueChart_hourlyGrouping() {
        // Arrange
        LocalDate chartStartDate = LocalDate.of(2024, 1, 10);
        LocalDate chartEndDate = LocalDate.of(2024, 1, 10);
        List<Order> ordersForChart = Arrays.asList(testOrder1, testOrder2);

        when(orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                chartStartDate.atStartOfDay().plusHours(6),
                chartEndDate.atTime(LocalTime.MAX).plusHours(3)
        )).thenReturn(ordersForChart);

        // Act
        List<RevenueDataPointDto> result = adminDashboardService.getRevenueChart(chartStartDate, chartEndDate);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("2024-01-10 12:00", result.get(0).getLabel());
        assertEquals(0, new BigDecimal("10.00").compareTo(result.get(0).getRevenue())); // Corrected BigDecimal comparison
        assertEquals("2024-01-10 13:00", result.get(1).getLabel());
        assertEquals(0, new BigDecimal("15.00").compareTo(result.get(1).getRevenue())); // Corrected BigDecimal comparison

        // Verify
        verify(orderRepository, times(1)).findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                chartStartDate.atStartOfDay().plusHours(6),
                chartEndDate.atTime(LocalTime.MAX).plusHours(3)
        );
    }

    @Test
    void getRevenueChart_dailyGrouping() {
        // Arrange
        LocalDate chartStartDate = LocalDate.of(2024, 1, 1);
        LocalDate chartEndDate = LocalDate.of(2024, 1, 31);
        List<Order> ordersForChart = Arrays.asList(testOrder1, testOrder2, testOrder3);

        when(orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                chartStartDate.atStartOfDay().plusHours(6),
                chartEndDate.atTime(LocalTime.MAX).plusHours(3)
        )).thenReturn(ordersForChart);

        // Act
        List<RevenueDataPointDto> result = adminDashboardService.getRevenueChart(chartStartDate, chartEndDate);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("2024-01-10", result.get(0).getLabel());
        assertEquals(0, new BigDecimal("25.00").compareTo(result.get(0).getRevenue())); // Corrected BigDecimal comparison
        assertEquals("2024-01-11", result.get(1).getLabel());
        assertEquals(0, new BigDecimal("10.00").compareTo(result.get(1).getRevenue())); // Corrected BigDecimal comparison

        // Verify
        verify(orderRepository, times(1)).findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                chartStartDate.atStartOfDay().plusHours(6),
                chartEndDate.atTime(LocalTime.MAX).plusHours(3)
        );
    }

    @Test
    void getTopPerformingItems_success() {
        // Arrange
        List<Order> completedOrders = Arrays.asList(testOrder1, testOrder2, testOrder3);
        when(orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        )).thenReturn(completedOrders);

        // Act
        List<TopPerformingItemDto> resultList = adminDashboardService.getTopPerformingItems(startDate, endDate);

        // Assert
        assertNotNull(resultList);
        assertEquals(2, resultList.size()); // Pizza and Pasta
        assertEquals("Pizza", resultList.get(0).getFoodName()); // Corrected expected order
        assertEquals(0, new BigDecimal("20.00").compareTo(resultList.get(0).getTotalRevenue())); // Corrected BigDecimal comparison
        assertEquals("Pasta", resultList.get(1).getFoodName());
        assertEquals(0, new BigDecimal("15.00").compareTo(resultList.get(1).getTotalRevenue())); // Corrected BigDecimal comparison

        // Verify
        verify(orderRepository, times(1)).findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );
    }

    @Test
    void getTopPerformingItems_noOrders() {
        // Arrange
        when(orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        )).thenReturn(Collections.emptyList());

        // Act
        List<TopPerformingItemDto> resultList = adminDashboardService.getTopPerformingItems(startDate, endDate);

        // Assert
        assertNotNull(resultList);
        assertTrue(resultList.isEmpty());

        // Verify
        verify(orderRepository, times(1)).findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );
    }

        @Test
    void getTopPerformingCategories_success() {
        // Arrange
        List<Order> completedOrders = Arrays.asList(testOrder1, testOrder2, testOrder3);
        when(orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        )).thenReturn(completedOrders);

        // Act
        List<TopPerformingCategoryDto> resultList = adminDashboardService.getTopPerformingCategories(startDate, endDate);

        // Assert
        assertNotNull(resultList);
        assertEquals(2, resultList.size()); // Italian and Dessert
        assertEquals("Italian", resultList.get(0).getCategoryName());
        assertEquals(0, new BigDecimal("20.00").compareTo(resultList.get(0).getTotalRevenue())); // Corrected BigDecimal comparison
        assertEquals("Dessert", resultList.get(1).getCategoryName());
        assertEquals(0, new BigDecimal("15.00").compareTo(resultList.get(1).getTotalRevenue())); // Corrected BigDecimal comparison

        // Verify
        verify(orderRepository, times(1)).findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );
    }

    @Test
    void getTopPerformingCategories_noOrders() {
        // Arrange
        when(orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        )).thenReturn(Collections.emptyList());

        // Act
        List<TopPerformingCategoryDto> resultList = adminDashboardService.getTopPerformingCategories(startDate, endDate);

        // Assert
        assertNotNull(resultList);
        assertTrue(resultList.isEmpty());

        // Verify
        verify(orderRepository, times(1)).findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );
    }

    @Test
    void getBusiestTables_success() {
        // Arrange
        List<Order> completedOrders = Arrays.asList(testOrder1, testOrder2, testOrder3);
        when(orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        )).thenReturn(completedOrders);

        // Act
        List<BusiestTableDto> resultList = adminDashboardService.getBusiestTables(startDate, endDate);

        // Assert
        assertNotNull(resultList);
        assertEquals(2, resultList.size()); // T1 and T2
        assertEquals("T1", resultList.get(0).getTableNumber());
        assertEquals(2, resultList.get(0).getOrderCount());
        assertEquals("T2", resultList.get(1).getTableNumber());
        assertEquals(1, resultList.get(1).getOrderCount());

        // Verify
        verify(orderRepository, times(1)).findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );
    }

    @Test
    void getBusiestTables_noOrders() {
        // Arrange
        when(orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        )).thenReturn(Collections.emptyList());

        // Act
        List<BusiestTableDto> resultList = adminDashboardService.getBusiestTables(startDate, endDate);

        // Assert
        assertNotNull(resultList);
        assertTrue(resultList.isEmpty());

        // Verify
        verify(orderRepository, times(1)).findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );
    }

    @Test
    void getRevenueHeatmap_success() {
        // Arrange
        List<Order> completedOrders = Arrays.asList(testOrder1, testOrder2, testOrder3);
        when(orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        )).thenReturn(completedOrders);

        // Act
        List<RevenueHeatmapPointDto> result = adminDashboardService.getRevenueHeatmap(startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());

        // Service sorts by proper week order (Mon-Sun), then by hour
        // Wednesday comes before Thursday in week order
        assertEquals("WEDNESDAY", result.get(0).getDayOfWeek());
        assertEquals(12, result.get(0).getHourOfDay());
        assertEquals(0, new BigDecimal("10.00").compareTo(result.get(0).getRevenue()));

        assertEquals("WEDNESDAY", result.get(1).getDayOfWeek());
        assertEquals(13, result.get(1).getHourOfDay());
        assertEquals(0, new BigDecimal("15.00").compareTo(result.get(1).getRevenue()));

        assertEquals("THURSDAY", result.get(2).getDayOfWeek());
        assertEquals(18, result.get(2).getHourOfDay());
        assertEquals(0, new BigDecimal("10.00").compareTo(result.get(2).getRevenue()));

        // Verify
        verify(orderRepository, times(1)).findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );
    }

    @Test
    void getRevenueHeatmap_noOrders() {
        // Arrange
        when(orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        )).thenReturn(Collections.emptyList());

        // Act
        List<RevenueHeatmapPointDto> result = adminDashboardService.getRevenueHeatmap(startDate, endDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify
        verify(orderRepository, times(1)).findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );
    }
}