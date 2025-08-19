package com.ste.restaurant.service;

import com.ste.restaurant.dto.dashboard.*;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AdminDashboardService {

    private final OrderRepository orderRepository;

    public AdminDashboardService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public DashboardStatsDto getDashboardStats(LocalDate startDate, LocalDate endDate) {
        List<Order> orders = orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );

        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = orders.size();

        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Set<User> uniqueCustomers = new HashSet<>();
        for (Order order : orders) {
            if (order.getCustomer() != null) {
                uniqueCustomers.add(order.getCustomer());
            }
        }

        long newCustomers = uniqueCustomers.stream()
                .filter(customer -> {
                    Order firstOrder = orderRepository.findFirstByCustomerOrderByOrderTimeAsc(customer);
                    if (firstOrder == null) {
                        return false;
                    }
                    LocalDate firstOrderDate = firstOrder.getOrderTime().toLocalDate();
                    return !firstOrderDate.isBefore(startDate) && !firstOrderDate.isAfter(endDate);
                })
                .count();

        return new DashboardStatsDto(totalRevenue, totalOrders, averageOrderValue, newCustomers);
    }

    public List<RevenueDataPointDto> getRevenueChart(LocalDate startDate, LocalDate endDate) {
        List<Order> orders = orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );

        Map<String, BigDecimal> revenueByTimeSlot = new LinkedHashMap<>();
        DateTimeFormatter formatter;

        // If the range is 3 days or less, group by hour. Otherwise, group by day.
        if (startDate.plusDays(3).isAfter(endDate)) {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
            orders.forEach(order -> {
                String hourSlot = order.getOrderTime().format(formatter);
                revenueByTimeSlot.merge(hourSlot, order.getTotalPrice(), BigDecimal::add);
            });
        } else {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            orders.forEach(order -> {
                String daySlot = order.getOrderTime().format(formatter);
                revenueByTimeSlot.merge(daySlot, order.getTotalPrice(), BigDecimal::add);
            });
        }

        List<RevenueDataPointDto> revenueDataPoints = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : revenueByTimeSlot.entrySet()) {
            revenueDataPoints.add(new RevenueDataPointDto(entry.getKey(), entry.getValue()));
        }

        // Sort by the time slot label to ensure chronological order for the chart
        revenueDataPoints.sort(Comparator.comparing(RevenueDataPointDto::getLabel));

        return revenueDataPoints;
    }

    public Page<TopPerformingItemDto> getTopPerformingItems(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // Get ALL orders in the date range to calculate accurate top performers
        List<Order>  orders = orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );
        Map<String, TopPerformingItemDto> map = new HashMap<>();

        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                String foodName = item.getFoodItem().getFoodName();
                map.merge(foodName,
                    new TopPerformingItemDto(foodName, item.getQuantity(), item.getTotalPrice()),
                    TopPerformingItemDto::add
                );
            }
        }
        List<TopPerformingItemDto> topPerformingItems = new ArrayList<>(map.values());

        topPerformingItems.sort(Comparator.comparing(
                TopPerformingItemDto::getTotalRevenue).reversed());

        return ServiceUtil.createPage(topPerformingItems, pageable);
    }

    public Page<TopPerformingCategoryDto> getTopPerformingCategories(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // Get ALL orders in the date range to calculate accurate top performers
        List<Order> orders = orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );

        // Map to accumulate revenue by category name
        Map<String, BigDecimal> categoryRevenueMap = new HashMap<>();

        for (Order order : orders) {
            for (OrderItem orderItem : order.getOrderItems()) {
                Set<Category> categories = orderItem.getFoodItem().getCategories();
                BigDecimal itemRevenue = orderItem.getTotalPrice();

                if (!categories.isEmpty()) {
                    BigDecimal revenuePerCategory = itemRevenue.divide(
                            BigDecimal.valueOf(categories.size()),
                            2,
                            RoundingMode.HALF_UP
                    );

                    for (Category category : categories) {
                        categoryRevenueMap.merge(category.getCategoryName(), revenuePerCategory, BigDecimal::add);
                    }
                } else {
                    categoryRevenueMap.merge("Uncategorized", itemRevenue, BigDecimal::add);
                }
            }
        }

        // Convert map entries to DTOs and sort by revenue (highest first)
        List<TopPerformingCategoryDto> topPerformingCategories = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : categoryRevenueMap.entrySet()) {
            topPerformingCategories.add(new TopPerformingCategoryDto(entry.getKey(), entry.getValue()));
        }
        topPerformingCategories.sort(Comparator.comparing(TopPerformingCategoryDto::getTotalRevenue).reversed());

        return ServiceUtil.createPage(topPerformingCategories, pageable);
    }

    public Page<BusiestTableDto> getBusiestTables(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // Get ALL orders in the date range to calculate accurate busiest tables
        List<Order> orders = orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );

        Map<String, Long> tableOrderCounts = new HashMap<>();

        for (Order order : orders) {
            if (order.getTable() != null && order.getTable().getTableNumber() != null) {
                String tableNumber = order.getTable().getTableNumber();
                tableOrderCounts.merge(tableNumber, 1L, Long::sum);
            }
        }
        List<BusiestTableDto> busiestTables = new ArrayList<>();
        for (Map.Entry<String, Long> entry : tableOrderCounts.entrySet()) {
            busiestTables.add(new BusiestTableDto(entry.getKey(), entry.getValue()));
        }
        busiestTables.sort(Comparator.comparing(BusiestTableDto::getOrderCount).reversed());

        return ServiceUtil.createPage(busiestTables, pageable);
    }

    public List<RevenueHeatmapPointDto> getRevenueHeatmap(LocalDate startDate, LocalDate endDate) {
        List<Order> orders = orderRepository.findAllByStatusAndOrderTimeBetween(
                OrderStatus.COMPLETED,
                startDate.atStartOfDay().plusHours(6),
                endDate.atTime(LocalTime.MAX).plusHours(3)
        );
        Map<String, BigDecimal> heatmapData = new HashMap<>();

        for (Order order : orders) {
            String dayOfWeek = order.getOrderTime().getDayOfWeek().toString();
            int hourOfDay = order.getOrderTime().getHour();
            String timeSlotKey = dayOfWeek + "-" + hourOfDay;

            heatmapData.merge(timeSlotKey, order.getTotalPrice(), BigDecimal::add);
        }

        List<RevenueHeatmapPointDto> heatmapPoints = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : heatmapData.entrySet()) {
            String[] parts = entry.getKey().split("-");
            String day = parts[0];
            int hour = Integer.parseInt(parts[1]);
            heatmapPoints.add(new RevenueHeatmapPointDto(day, hour, entry.getValue()));
        }

        // Sort by proper week order (Monday-Sunday), then by hour
        List<String> weekOrder = Arrays.asList(
                "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
        heatmapPoints.sort(Comparator
                .comparing((RevenueHeatmapPointDto point) -> weekOrder.indexOf(point.getDayOfWeek()))
                .thenComparing(RevenueHeatmapPointDto::getHourOfDay));

        return heatmapPoints;
    }
}
