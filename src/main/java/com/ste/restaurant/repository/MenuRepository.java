package com.ste.restaurant.repository;

import com.ste.restaurant.entity.FoodItem;
import com.ste.restaurant.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    boolean existsMenuByMenuName(String menuName);

    Optional<Menu> findByMenuName(String name);

    List<Menu> findAllByActive(boolean active);

    void deleteByMenuName(String name);

    List<Menu> findAllByActiveAndFoodItemsIsContaining(boolean active, Set<FoodItem> foodItems);

    List<Menu> findAllByActiveAndFoodItemsContains(boolean b, Set<String> strings);
}
