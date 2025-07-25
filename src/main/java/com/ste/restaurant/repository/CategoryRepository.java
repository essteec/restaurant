package com.ste.restaurant.repository;

import com.ste.restaurant.entity.Category;
import com.ste.restaurant.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

     boolean existsCategoryByCategoryName(String s);

     Optional<Category> findByCategoryName(String categoryName);

    List<Category> getCategoriesByFoodItems(Set<FoodItem> foodItems);

    Category getCategoriesByCategoryName(String categoryName);
}
