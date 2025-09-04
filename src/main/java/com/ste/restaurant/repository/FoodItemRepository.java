package com.ste.restaurant.repository;

import com.ste.restaurant.dto.dashboard.TopPerformingItemDto;
import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.entity.FoodItemTranslation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    boolean existsFoodItemByFoodName(String foodName);

    Optional<FoodItem> findByFoodName(String name);

    Page<FoodItem> findAllByFoodNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String foodName, String description, Pageable pageable);

    // get popular food items from featured menu if food in active menu
    @Query("""
        SELECT oi.foodItem FROM OrderItem oi 
        JOIN oi.foodItem fi 
        WHERE fi IN (
            SELECT f FROM Menu m 
            JOIN m.foodItems f 
            WHERE m.menuName = 'Featured'
        ) 
        GROUP BY oi.foodItem 
        ORDER BY COUNT(oi.foodItem) DESC
    """)
    List<FoodItem> findPopularFoodItems(Pageable pageable);
}
