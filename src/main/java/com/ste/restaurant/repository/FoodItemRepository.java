package com.ste.restaurant.repository;

import com.ste.restaurant.dto.dashboard.TopPerformingItemDto;
import com.ste.restaurant.entity.FoodItem;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    boolean existsFoodItemByFoodName(String foodName);

    Optional<FoodItem> findByFoodName(String name);
}
